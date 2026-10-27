package dev.eclipsememo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.texteditor.ITextEditor;

/** Uses public viewer accessors, leaving JDT content providers and filters intact. */
public final class WorkbenchBridge implements IWindowListener, IPageListener, IPartListener2, AutoCloseable {
    private static final String PACKAGES = "org.eclipse.jdt.ui.PackageExplorer";
    private static final String PROJECTS = "org.eclipse.ui.navigator.ProjectExplorer";
    private final Map<IViewPart, FilePane> panes = new IdentityHashMap<>();
    private final Map<IEditorPart, NativeDraftSave> nativeSaves = new IdentityHashMap<>();

    void start() {
        PlatformUI.getWorkbench().addWindowListener(this);
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) windowOpened(window);
    }

    @Override public void windowOpened(IWorkbenchWindow window) {
        window.addPageListener(this);
        for (IWorkbenchPage page : window.getPages()) pageOpened(page);
    }
    @Override public void windowClosed(IWorkbenchWindow window) { window.removePageListener(this); }
    @Override public void windowActivated(IWorkbenchWindow window) {}
    @Override public void windowDeactivated(IWorkbenchWindow window) {}
    @Override public void pageOpened(IWorkbenchPage page) {
        page.addPartListener(this);
        for (IViewReference ref : page.getViewReferences()) attach(ref.getView(false));
        for (org.eclipse.ui.IEditorReference ref : page.getEditorReferences()) attach(ref.getEditor(false));
    }
    @Override public void pageClosed(IWorkbenchPage page) { page.removePartListener(this); }
    @Override public void pageActivated(IWorkbenchPage page) { refresh(); }

    private void attach(IWorkbenchPart part) {
        if (part instanceof IViewPart) {
            IViewPart view = (IViewPart) part;
            String id = view.getSite().getId();
            if ((!PACKAGES.equals(id) && !PROJECTS.equals(id)) || panes.containsKey(view)) return;
            try {
                // IPackagesViewPart is optional: access its public method without a JDT bundle dependency.
                TreeViewer viewer = view instanceof CommonNavigator ? ((CommonNavigator) view).getCommonViewer()
                        : (TreeViewer) view.getClass().getMethod("getTreeViewer").invoke(view);
                if (viewer != null && !viewer.getControl().isDisposed()) {
                    FilePane pane = new FilePane(view, viewer, Activator.getDefault().files());
                    panes.put(view, pane);
                    pane.onDispose(() -> panes.remove(view));
                }
            } catch (Exception ex) {
                Activator.log(id + "에 워크스페이스 파일 영역을 추가하지 못했습니다.", ex);
            }
        }
        if (part instanceof ITextEditor && !(part instanceof MemoEditor)) {
            ITextEditor editor = (ITextEditor) part;
            if (SaveSupport.isDraft(editor)) nativeSaves.computeIfAbsent(editor, ignored -> new NativeDraftSave(editor));
            else removeSave(editor);
        }
    }

    private void removeSave(IEditorPart editor) {
        NativeDraftSave binding = nativeSaves.remove(editor);
        if (binding != null) binding.close();
    }

    public void refresh() {
        for (FilePane pane : new ArrayList<>(panes.values())) pane.refresh();
    }

    public FilePane pane(IViewPart view) { return panes.get(view); }

    @Override public void partOpened(IWorkbenchPartReference ref) { attach(ref.getPart(false)); }
    @Override public void partVisible(IWorkbenchPartReference ref) { attach(ref.getPart(false)); refresh(); }
    @Override public void partInputChanged(IWorkbenchPartReference ref) { attach(ref.getPart(false)); }
    @Override public void partActivated(IWorkbenchPartReference ref) {
        IWorkbenchPart part = ref.getPart(false);
        attach(part);
        if (part instanceof IEditorPart) {
            Path path = EditorSupport.path((IEditorPart) part);
            if (path != null) for (FilePane pane : panes.values()) pane.link(path);
        }
    }
    @Override public void partClosed(IWorkbenchPartReference ref) {
        IWorkbenchPart part = ref.getPart(false);
        if (part instanceof IEditorPart) removeSave((IEditorPart) part);
        if (part instanceof IViewPart) {
            FilePane pane = panes.remove(part);
            if (pane != null) pane.close();
        }
    }
    @Override public void partBroughtToTop(IWorkbenchPartReference ref) {}
    @Override public void partDeactivated(IWorkbenchPartReference ref) {}
    @Override public void partHidden(IWorkbenchPartReference ref) {}

    @Override public void close() {
        PlatformUI.getWorkbench().removeWindowListener(this);
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            window.removePageListener(this);
            for (IWorkbenchPage page : window.getPages()) page.removePartListener(this);
        }
        for (NativeDraftSave save : new ArrayList<>(nativeSaves.values())) save.close();
        nativeSaves.clear();
        for (FilePane pane : new ArrayList<>(panes.values())) pane.close();
        panes.clear();
    }
}

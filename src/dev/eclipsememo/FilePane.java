package dev.eclipsememo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/** An embedded area inside each original Explorer view, not another ViewPart. */
public final class FilePane implements AutoCloseable {
    private final IViewPart view;
    private final WorkspaceFiles files;
    private final Composite originalArea;
    private final Composite originalParent;
    private final SashForm split;
    private final Composite fileArea;
    private final TreeViewer viewer;
    private final ToolBarManager toolbar;
    private final MenuManager menu;
    private Runnable disposed = () -> {};

    FilePane(IViewPart view, TreeViewer originalViewer, WorkspaceFiles files) {
        this.view = view;
        this.files = files;
        originalArea = originalViewer.getControl().getParent();
        originalParent = originalArea.getParent();
        split = new SashForm(originalParent, SWT.VERTICAL);
        split.setLayoutData(originalArea.getLayoutData());
        if (!originalArea.setParent(split)) {
            split.dispose();
            throw new IllegalStateException("탐색기의 파일 영역을 배치할 수 없습니다.");
        }
        fileArea = new Composite(split, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        fileArea.setLayout(layout);
        Composite heading = new Composite(fileArea, SWT.NONE);
        GridLayout headingLayout = new GridLayout(2, false);
        headingLayout.marginHeight = 0;
        headingLayout.marginWidth = 0;
        heading.setLayout(headingLayout);
        heading.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label title = new Label(heading, SWT.NONE);
        title.setText("워크스페이스 파일");
        title.setToolTipText(files.root().toString());
        title.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        toolbar = new ToolBarManager(SWT.FLAT);
        Action newMemo = action("새 메모", ISharedImages.IMG_OBJ_FILE, () -> Activator.getDefault().newMemo(view.getSite().getPage()));
        Action newFolder = action("새 폴더", ISharedImages.IMG_OBJ_FOLDER, this::newFolder);
        Action refresh = action("새로고침", ISharedImages.IMG_TOOL_REDO, () -> Activator.getDefault().refresh());
        toolbar.add(newMemo);
        toolbar.add(newFolder);
        toolbar.add(refresh);
        toolbar.createControl(heading);
        viewer = new TreeViewer(fileArea, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.getControl().setToolTipText(files.root().toString());
        viewer.setContentProvider(new ITreeContentProvider() {
            @Override public Object[] getElements(Object input) { return children(files.root()); }
            @Override public Object[] getChildren(Object parent) {
                return parent instanceof WorkspaceEntry && ((WorkspaceEntry) parent).isDirectory()
                        ? children(((WorkspaceEntry) parent).path()) : new Object[0];
            }
            private Object[] children(Path parent) {
                try { return files.list(parent).toArray(); }
                catch (IOException ex) { Activator.log("파일 목록을 읽지 못했습니다: " + parent, ex); return new Object[0]; }
            }
            @Override public boolean hasChildren(Object element) {
                return element instanceof WorkspaceEntry && ((WorkspaceEntry) element).isDirectory();
            }
            @Override public Object getParent(Object element) {
                if (!(element instanceof WorkspaceEntry)) return null;
                Path parent = ((WorkspaceEntry) element).path().getParent();
                return parent.equals(files.root()) ? files.root() : new WorkspaceEntry(parent);
            }
        });
        viewer.setLabelProvider(new LabelProvider() {
            @Override public String getText(Object element) { return element.toString(); }
            @Override public Image getImage(Object element) {
                return PlatformUI.getWorkbench().getSharedImages().getImage(
                        ((WorkspaceEntry) element).isDirectory() ? ISharedImages.IMG_OBJ_FOLDER : ISharedImages.IMG_OBJ_FILE);
            }
        });
        viewer.setInput(files.root());
        viewer.addSelectionChangedListener(event -> {
            if (!event.getSelection().isEmpty()) originalViewer.setSelection(StructuredSelection.EMPTY);
        });
        viewer.addOpenListener(event -> openSelection());
        viewer.getControl().addListener(SWT.KeyDown, event -> {
            if (event.keyCode == SWT.F5) { refresh(); event.doit = false; }
            if (event.keyCode == SWT.F2) { rename(); event.doit = false; }
            if (event.keyCode == SWT.DEL) { delete(); event.doit = false; }
        });
        menu = new MenuManager();
        menu.add(newMemo);
        menu.add(newFolder);
        menu.add(action("열기", ISharedImages.IMG_OBJ_FILE, this::openSelection));
        menu.add(action("이름 변경…", null, this::rename));
        menu.add(action("삭제…", null, this::delete));
        menu.add(refresh);
        viewer.getControl().setMenu(menu.createContextMenu(viewer.getControl()));
        split.setWeights(new int[] { 65, 35 });
        split.addDisposeListener(event -> { toolbar.dispose(); menu.dispose(); disposed.run(); });
        refresh();
        originalParent.layout(true, true);
    }

    public TreeViewer viewer() { return viewer; }
    public void onDispose(Runnable callback) { disposed = callback; }

    @FunctionalInterface private interface Operation { void run() throws Exception; }
    private Action action(String name, String image, Operation operation) {
        Action result = new Action(name) {
            @Override public void run() {
                try { operation.run(); }
                catch (Exception ex) { Activator.error(view.getSite().getShell(), name + " 작업에 실패했습니다.", ex); }
            }
        };
        result.setToolTipText(name);
        if (image != null) result.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(image));
        return result;
    }

    private WorkspaceEntry selected() {
        Object value = viewer.getStructuredSelection().getFirstElement();
        return value instanceof WorkspaceEntry ? (WorkspaceEntry) value : null;
    }

    private Path selectedDirectory() {
        WorkspaceEntry entry = selected();
        return entry == null ? files.root() : entry.isDirectory() ? entry.path() : entry.path().getParent();
    }

    private void openSelection() {
        WorkspaceEntry entry = selected();
        if (entry == null) return;
        if (entry.isDirectory()) {
            viewer.setExpandedState(entry, !viewer.getExpandedState(entry));
            return;
        }
        try { EditorSupport.open(view.getSite().getPage(), entry.path()); }
        catch (Exception ex) { Activator.error(view.getSite().getShell(), "파일을 열지 못했습니다.", ex); }
    }

    private void newFolder() throws Exception {
        Path parent = selectedDirectory();
        InputDialog dialog = new InputDialog(view.getSite().getShell(), "새 폴더", parent.toString(), "", WorkspaceFiles::validateName);
        if (dialog.open() == Window.OK) {
            Files.createDirectory(files.resolveName(parent, dialog.getValue()));
            Activator.getDefault().refresh();
        }
    }

    private void rename() {
        WorkspaceEntry entry = selected();
        if (entry == null) return;
        try {
            files.check(entry.path(), false);
            if (entry.isDirectory()) throw new IOException("폴더 이름 변경은 아직 지원하지 않습니다.");
            InputDialog dialog = new InputDialog(view.getSite().getShell(), "이름 변경", "새 파일 이름:", entry.toString(), WorkspaceFiles::validateName);
            if (dialog.open() != Window.OK || dialog.getValue().equals(entry.toString())) return;
            Path target = files.resolveName(entry.path().getParent(), dialog.getValue());
            if (Files.exists(target)) throw new IOException("같은 이름의 파일이나 폴더가 있습니다.");
            if (!EditorSupport.closeForFile(entry.path())) return;
            Files.move(entry.path(), target);
            Activator.getDefault().refresh();
            EditorSupport.open(view.getSite().getPage(), target);
        } catch (Exception ex) { Activator.error(view.getSite().getShell(), "이름을 변경하지 못했습니다.", ex); }
    }

    private void delete() {
        WorkspaceEntry entry = selected();
        if (entry == null) return;
        try {
            files.check(entry.path(), false);
            if (!MessageDialog.openQuestion(view.getSite().getShell(), "파일 삭제", entry + "을(를) 영구 삭제할까요?\n폴더는 비어 있을 때만 삭제됩니다.")) return;
            if (!EditorSupport.closeForFile(entry.path())) return;
            Files.delete(entry.path());
            Activator.getDefault().refresh();
        } catch (Exception ex) { Activator.error(view.getSite().getShell(), "삭제하지 못했습니다.", ex); }
    }

    public void refresh() {
        if (split.isDisposed()) return;
        viewer.refresh();
        boolean noProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0;
        Composite maximized = noProjects ? fileArea : null;
        if (split.getMaximizedControl() != maximized) split.setMaximizedControl(maximized);
    }

    void link(Path path) {
        if (!files.isVisible(path) || split.isDisposed()) return;
        try {
            Object enabled = view.getClass().getMethod("isLinkingEnabled").invoke(view);
            if (Boolean.TRUE.equals(enabled)) viewer.setSelection(new StructuredSelection(new WorkspaceEntry(path)), true);
        } catch (ReflectiveOperationException ignored) {
            // Linking is optional. Existing project linking remains owned by the original view.
        }
    }

    @Override public void close() {
        if (split.isDisposed()) return;
        if (!originalArea.isDisposed() && !originalParent.isDisposed()) {
            originalArea.setParent(originalParent);
            originalParent.layout(true, true);
        }
        split.dispose();
    }
}

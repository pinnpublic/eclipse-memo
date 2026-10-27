package dev.eclipsememo.tests;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import dev.eclipsememo.Activator;
import dev.eclipsememo.EditorSupport;
import dev.eclipsememo.FilePane;
import dev.eclipsememo.MemoEditor;
import dev.eclipsememo.SaveSupport;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.texteditor.ITextEditor;

/** Real workbench tests against each installed Eclipse distribution. */
public final class RuntimeTests implements IApplication, org.eclipse.ui.IStartup {
    private final List<String> checks = new ArrayList<>();
    private int exitCode = 1;
    private Display display;

    @Override public void earlyStartup() {
        if (!Boolean.getBoolean("workspaceMemo.test.realWorkbench")) return;
        display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(() -> display.timerExec(1500, this::runAndReport));
    }

    @Override public Object start(IApplicationContext context) throws Exception {
        display = PlatformUI.createDisplay();
        try {
            PlatformUI.createAndRunWorkbench(display, new WorkbenchAdvisor() {
                @Override public org.eclipse.core.runtime.IAdaptable getDefaultPageInput() {
                    return ResourcesPlugin.getWorkspace().getRoot();
                }
                @Override public void preStartup() {
                    // Exercise our startup hook without unrelated update/news downloads in test workspaces.
                    String disabled = Arrays.stream(Platform.getExtensionRegistry()
                            .getConfigurationElementsFor("org.eclipse.ui.startup"))
                            .map(element -> element.getContributor().getName())
                            .filter(id -> !id.equals(Activator.ID)).distinct()
                            .collect(java.util.stream.Collectors.joining(";"));
                    PlatformUI.getPreferenceStore().setValue("PLUGINS_NOT_ACTIVATED_ON_STARTUP", disabled);
                }
                @Override public String getInitialWindowPerspectiveId() { return "dev.eclipsememo.tests.perspective"; }
                @Override public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
                    return new WorkbenchWindowAdvisor(configurer) {
                        @Override public void preWindowOpen() {
                            configurer.setInitialSize(new Point(1200, 850));
                            configurer.setTitle("Workspace Memo Compatibility Test");
                        }
                    };
                }
                @Override public void postStartup() { display.asyncExec(() -> runAndReport()); }
            });
        } finally { display.dispose(); }
        return exitCode;
    }

    private void runAndReport() {
        try {
            runTests();
            exitCode = 0;
        } catch (Throwable ex) {
            checks.add("FAIL: " + ex);
            java.io.StringWriter trace = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(trace));
            checks.add(trace.toString());
        }
        try {
            Files.write(Path.of(System.getProperty("workspaceMemo.test.result")), checks, StandardCharsets.UTF_8);
        } catch (Exception ex) { ex.printStackTrace(); }
        System.out.println(String.join("\n", checks));
        if (!Boolean.getBoolean("workspaceMemo.test.keepOpen")) display.asyncExec(() -> PlatformUI.getWorkbench().close());
    }

    private void runTests() throws Exception {
        Activator plugin = Activator.getDefault();
        plugin.initialize();
        if (PlatformUI.getWorkbench().getIntroManager().getIntro() != null)
            PlatformUI.getWorkbench().getIntroManager().closeIntro(PlatformUI.getWorkbench().getIntroManager().getIntro());
        IWorkbenchPage page = PlatformUI.getWorkbench().showPerspective("dev.eclipsememo.tests.perspective",
                PlatformUI.getWorkbench().getActiveWorkbenchWindow());
        Path root = plugin.files().root();
        check(ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0, "starts with no projects");
        IViewPart packages = page.showView("org.eclipse.jdt.ui.PackageExplorer");
        IViewPart projects = page.showView("org.eclipse.ui.navigator.ProjectExplorer");
        pump();
        FilePane packagePane = plugin.bridge().pane(packages);
        FilePane projectPane = plugin.bridge().pane(projects);
        check(packagePane != null && projectPane != null, "embedded in both original Explorer views");
        page.activate(packages); pump();
        check(packagePane.viewer().getTree().isVisible(), "Package Explorer files visible with zero projects");
        page.activate(projects); pump();
        check(projectPane.viewer().getTree().isVisible(), "Project Explorer files visible with zero projects");

        plugin.newMemo(page);
        ITextEditor editor = (ITextEditor) page.getActiveEditor();
        doc(editor).set("프로젝트 없는 한글 메모\nUTF-8 저장 확인\n");
        check(editor.isDirty(), "new memo editable and dirty");
        Path memo = root.resolve("메모 한글.txt");
        SaveSupport.saveTo(editor, memo, false);
        check(Files.readString(memo).equals("프로젝트 없는 한글 메모\nUTF-8 저장 확인\n"), "UTF-8 saved directly under workspace");
        check(!editor.isDirty() && memo.equals(EditorSupport.path(editor)), "same tab rebound and marked saved");
        check(Arrays.stream(page.getEditorReferences()).map(ref -> ref.getEditor(false))
                .filter(part -> memo.equals(EditorSupport.path(part))).count() == 1,
                "saving does not create duplicate memo editor tabs");
        check(ResourcesPlugin.getWorkspace().getRoot().getProjects().length == 0, "saving creates no synthetic project");
        page.closeEditor(editor, false);
        editor = (ITextEditor) EditorSupport.open(page, memo);
        check(doc(editor).get().contains("프로젝트 없는"), "saved Korean text reopens correctly");
        doc(editor).set("다시 편집한 내용\n");
        editor.doSave(new NullProgressMonitor());
        check(Files.readString(memo).equals("다시 편집한 내용\n"), "subsequent normal save updates original file");

        Path cp949 = root.resolve("기존 CP949.txt");
        Files.write(cp949, "옛날 한글".getBytes(Charset.forName("MS949")));
        ITextEditor legacy = (ITextEditor) EditorSupport.open(page, cp949);
        check(doc(legacy).get().equals("옛날 한글"), "existing CP949 decoded correctly");
        doc(legacy).set("옛날 한글 수정");
        legacy.doSave(new NullProgressMonitor());
        check(new String(Files.readAllBytes(cp949), Charset.forName("MS949")).equals("옛날 한글 수정"), "existing CP949 encoding preserved on save");
        page.closeEditor(legacy, false);

        plugin.newMemo(page);
        ITextEditor canceled = (ITextEditor) page.getActiveEditor();
        doc(canceled).set("취소해도 남아야 하는 내용");
        respondToSave(null);
        check(!SaveSupport.promptAndSave(canceled), "cancel save returns without writing");
        check(canceled.isDirty() && doc(canceled).get().equals("취소해도 남아야 하는 내용"), "cancel keeps unsaved text");
        page.closeEditor(canceled, false);

        plugin.newMemo(page);
        ITextEditor saveAll = (ITextEditor) page.getActiveEditor();
        doc(saveAll).set("Save All 한글");
        respondToSave("모두저장.txt");
        check(page.saveAllEditors(false), "Save All supports a new workspace memo");
        check(Files.readString(root.resolve("모두저장.txt")).equals("Save All 한글"), "Save All writes UTF-8");
        page.closeEditor(saveAll, false);

        IEditorInput nativeInput = (IEditorInput) Platform.getBundle("org.eclipse.ui.editors")
                .loadClass("org.eclipse.ui.internal.editors.text.NonExistingFileEditorInput")
                .getConstructor(org.eclipse.core.filesystem.IFileStore.class, String.class)
                .newInstance(EFS.getStore(root.resolve("not-created-native.txt").toUri()), "Untitled");
        ITextEditor nativeEditor = (ITextEditor) page.openEditor(nativeInput, EditorsUI.DEFAULT_TEXT_EDITOR_ID);
        pump();
        doc(nativeEditor).set("기본 Untitled에서 저장");
        respondToSave("기본메모.txt");
        IHandlerService handlers = nativeEditor.getSite().getService(IHandlerService.class);
        handlers.executeCommand(IWorkbenchCommandConstants.FILE_SAVE, null);
        check(Files.readString(root.resolve("기본메모.txt")).equals("기본 Untitled에서 저장"), "built-in Untitled Save command saves without a project");
        doc(nativeEditor).set("기본 편집기의 다음 저장");
        handlers.executeCommand(IWorkbenchCommandConstants.FILE_SAVE, null);
        check(Files.readString(root.resolve("기본메모.txt")).equals("기본 편집기의 다음 저장"), "native Save handler restored after first save");
        page.closeEditor(nativeEditor, false);

        plugin.bridge().refresh();
        check(has(packagePane, "메모 한글.txt") && has(projectPane, "메모 한글.txt"), "both explorers list workspace files");
        check(!has(packagePane, ".metadata") && !has(projectPane, ".metadata"), "metadata hidden in both explorers");
        Files.writeString(root.resolve("외부에서 생성.txt"), "external");
        pumpUntil(() -> has(packagePane, "외부에서 생성.txt") && has(projectPane, "외부에서 생성.txt"));
        check(has(packagePane, "외부에서 생성.txt") && has(projectPane, "외부에서 생성.txt"), "file watcher refreshes both views");

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("ExistingJavaProject");
        project.create(null);
        project.open(null);
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        project.setDescription(description, null);
        IJavaProject javaProject = JavaCore.create(project);
        javaProject.setRawClasspath(new org.eclipse.jdt.core.IClasspathEntry[0], null);
        pump();
        plugin.bridge().refresh();
        check(!has(packagePane, "ExistingJavaProject") && !has(projectPane, "ExistingJavaProject"), "project directory excluded from ordinary file lists");
        TreeViewer javaViewer = (TreeViewer) packages.getClass().getMethod("getTreeViewer").invoke(packages);
        TreeViewer resourceViewer = ((CommonNavigator) projects).getCommonViewer();
        javaViewer.refresh(); resourceViewer.refresh(); pump();
        pumpUntil(() -> javaViewer.getTree().getItemCount() > 0 && resourceViewer.getTree().getItemCount() > 0);
        check(javaViewer.getTree().getItemCount() > 0 && resourceViewer.getTree().getItemCount() > 0,
                "original package and project trees still show projects");
        page.activate(packages); pump();
        check(javaViewer.getTree().isVisible(), "original package tree visible alongside memo area");
        page.activate(projects); pump();
        check(resourceViewer.getTree().isVisible(), "original project tree visible alongside memo area");
        for (IViewPart explorer : Arrays.asList(packages, projects)) {
            page.activate(explorer); pump();
            FilePane pane = plugin.bridge().pane(explorer);
            Composite heading = (Composite) pane.viewer().getTree().getParent().getChildren()[0];
            org.eclipse.swt.graphics.RGB background = heading.getBackground().getRGB();
            check(!background.equals(pane.viewer().getTree().getBackground().getRGB()), "memo heading contrasts with file list");
            for (int eventType : new int[] { SWT.MouseEnter, SWT.MouseExit, SWT.FocusIn, SWT.FocusOut }) {
                heading.notifyListeners(eventType, new org.eclipse.swt.widgets.Event());
                heading.redraw(); heading.update();
            }
            page.activate(editor); pump();
            page.activate(explorer); pump();
            check(background.equals(heading.getBackground().getRGB()), "heading background survives hover and focus changes");
            // Eclipse theme styling can reset Composite backgrounds after creation.
            heading.setBackground(pane.viewer().getTree().getBackground());
            heading.redraw(); heading.update();
            check(background.equals(heading.getBackground().getRGB()), "heading background recovers after theme repaint");
        }
        page.activate(packages); pump();
        Shell screenshotShell = page.getWorkbenchWindow().getShell();
        org.eclipse.swt.graphics.Rectangle client = screenshotShell.getClientArea();
        org.eclipse.swt.graphics.Image screenshot = new org.eclipse.swt.graphics.Image(display, client.width, client.height);
        org.eclipse.swt.graphics.GC screenshotGc = new org.eclipse.swt.graphics.GC(screenshotShell);
        try {
            screenshotGc.copyArea(screenshot, client.x, client.y);
            org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
            loader.data = new org.eclipse.swt.graphics.ImageData[] { screenshot.getImageData() };
            loader.save(Path.of(System.getProperty("workspaceMemo.test.result")).resolveSibling("header-focus.png").toString(), SWT.IMAGE_PNG);
        } finally { screenshotGc.dispose(); screenshot.dispose(); }
        javaViewer.setSelection(new StructuredSelection(javaProject));
        packagePane.viewer().setSelection(new StructuredSelection(packagePane.viewer().getTree().getItem(0).getData()));
        check(javaViewer.getSelection().isEmpty(), "file selection clears stale project selection");

        page.hideView(packages);
        packages = page.showView("org.eclipse.jdt.ui.PackageExplorer");
        pump();
        check(plugin.bridge().pane(packages) != null, "closing and reopening Package Explorer reattaches area");
        page.hideView(projects);
        projects = page.showView("org.eclipse.ui.navigator.ProjectExplorer");
        pump();
        check(plugin.bridge().pane(projects) != null, "closing and reopening Project Explorer reattaches area");
        EditorSupport.open(page, memo);
        checks.add("PASS: " + checks.size() + " runtime checks on " + Platform.getBundle("org.eclipse.platform").getVersion());
    }

    private IDocument doc(ITextEditor editor) { return editor.getDocumentProvider().getDocument(editor.getEditorInput()); }
    private boolean has(FilePane pane, String name) {
        return Arrays.stream(pane.viewer().getTree().getItems()).anyMatch(item -> item.getText().equals(name));
    }
    private void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks.add("OK: " + message);
        try { Files.write(Path.of(System.getProperty("workspaceMemo.test.result")), checks, StandardCharsets.UTF_8); }
        catch (Exception ex) { throw new AssertionError(ex); }
    }
    private void pump() {
        long end = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < end) { if (!display.readAndDispatch()) {
            try { Thread.sleep(10); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }}
    }
    private void pumpUntil(java.util.function.BooleanSupplier condition) {
        long end = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < end) pump();
    }
    private void respondToSave(String name) {
        display.timerExec(100, new Runnable() {
            int attempts;
            @Override public void run() {
                for (Shell shell : display.getShells()) {
                    if (!shell.isDisposed() && shell.getText().equals("워크스페이스에 저장")) {
                        if (name == null) shell.close();
                        else {
                            Text text = findText(shell);
                            text.setText(name);
                            shell.getDefaultButton().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
                        }
                        return;
                    }
                }
                if (++attempts < 30) display.timerExec(100, this);
            }
        });
    }
    private Text findText(Composite parent) {
        for (Control control : parent.getChildren()) {
            if (control instanceof Text) return (Text) control;
            if (control instanceof Composite) { Text result = findText((Composite) control); if (result != null) return result; }
        }
        return null;
    }
    @Override public void stop() { if (display != null && !display.isDisposed()) display.asyncExec(() -> PlatformUI.getWorkbench().close()); }
}

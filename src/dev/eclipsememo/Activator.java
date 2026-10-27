package dev.eclipsememo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public final class Activator extends AbstractUIPlugin {
    public static final String ID = "dev.eclipsememo";
    private static Activator instance;
    private WorkspaceFiles files;
    private WorkspaceWatcher watcher;
    private WorkbenchBridge bridge;
    private IResourceChangeListener resourceListener;
    private boolean pending;

    public static Activator getDefault() { return instance; }

    @Override public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    public synchronized WorkspaceFiles files() throws IOException {
        if (files == null) {
            Path root = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
            files = new WorkspaceFiles(root, candidate -> Arrays.stream(
                    ResourcesPlugin.getWorkspace().getRoot().getProjects()).map(IProject::getLocation)
                    .filter(location -> location != null)
                    .anyMatch(location -> candidate.equals(location.toFile().toPath().toAbsolutePath().normalize())));
        }
        return files;
    }

    public void initialize() {
        if (bridge != null || !PlatformUI.isWorkbenchRunning()) return;
        try {
            files();
            bridge = new WorkbenchBridge();
            bridge.start();
            watcher = new WorkspaceWatcher(files, this::refresh);
            watcher.start();
            resourceListener = event -> refresh();
            ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
        } catch (Exception ex) {
            log("워크스페이스 메모 초기화에 실패했습니다.", ex);
        }
    }

    public void refresh() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display.isDisposed()) return;
        display.asyncExec(() -> {
            if (pending || bridge == null) return;
            pending = true;
            display.timerExec(150, () -> {
                pending = false;
                if (bridge != null) bridge.refresh();
            });
        });
    }

    public void newMemo(IWorkbenchPage page) throws Exception {
        initialize();
        page.openEditor(new DraftInput(), MemoEditor.ID);
    }

    public WorkbenchBridge bridge() { return bridge; }

    @Override public void stop(BundleContext context) throws Exception {
        if (watcher != null) watcher.close();
        if (resourceListener != null) ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        if (bridge != null && PlatformUI.isWorkbenchRunning()) {
            PlatformUI.getWorkbench().getDisplay().syncExec(() -> bridge.close());
            bridge = null;
        }
        instance = null;
        super.stop(context);
    }

    public static void log(String message, Throwable ex) {
        if (instance != null) instance.getLog().log(new Status(IStatus.ERROR, ID, message, ex));
    }

    public static void error(Shell shell, String message, Exception ex) {
        log(message, ex);
        MessageDialog.openError(shell, "Workspace Memo", message + "\n" + ex.getMessage());
    }
}

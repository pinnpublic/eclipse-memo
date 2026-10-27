package dev.eclipsememo;

import java.nio.file.Path;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;

public final class EditorSupport {
    private EditorSupport() {}

    public static IEditorPart open(IWorkbenchPage page, Path path) throws Exception {
        Activator.getDefault().files().check(path, false);
        IFileStore store = EFS.getStore(path.toUri());
        FileStoreEditorInput input = new FileStoreEditorInput(store);
        IEditorPart existing = page.findEditor(input);
        if (existing != null) { page.activate(existing); return existing; }
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        boolean alreadyConnected = manager.getFileStoreTextFileBuffer(store) != null;
        manager.connectFileStore(store, null);
        try {
            ITextFileBuffer buffer = manager.getFileStoreTextFileBuffer(store);
            if (!alreadyConnected) {
                buffer.setEncoding(WorkspaceFiles.detectEncoding(path));
                buffer.revert(null);
            }
            return page.openEditor(input, MemoEditor.ID);
        } finally {
            manager.disconnectFileStore(store, null);
        }
    }

    public static Path path(IEditorPart editor) {
        if (editor != null && editor.getEditorInput() instanceof IURIEditorInput) {
            java.net.URI uri = ((IURIEditorInput) editor.getEditorInput()).getURI();
            if ("file".equalsIgnoreCase(uri.getScheme())) return java.nio.file.Paths.get(uri).toAbsolutePath().normalize();
        }
        return null;
    }

    public static boolean closeForFile(Path path) {
        for (org.eclipse.ui.IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    IEditorPart editor = ref.getEditor(false);
                    if (path.equals(path(editor)) && !page.closeEditor(editor, true)) return false;
                }
            }
        }
        return true;
    }
}

package dev.eclipsememo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public final class SaveSupport {
    private SaveSupport() {}

    public static boolean isDraft(IEditorPart editor) {
        return editor instanceof ITextEditor && (editor.getEditorInput() instanceof DraftInput
                || editor.getEditorInput().getClass().getName().equals(
                        "org.eclipse.ui.internal.editors.text.NonExistingFileEditorInput"));
    }

    public static boolean promptAndSave(ITextEditor editor) {
        try {
            WorkspaceFiles files = Activator.getDefault().files();
            Path parent = files.root();
            String suggestion = isDraft(editor) ? "memo.txt" : editor.getEditorInput().getName();
            InputDialog dialog = new InputDialog(editor.getSite().getShell(), "워크스페이스에 저장",
                    "저장 위치: " + parent + "\n파일 이름:", suggestion, name -> {
                        try { files.resolveName(parent, name); return null; }
                        catch (IOException ex) { return ex.getMessage(); }
                    });
            if (dialog.open() != Window.OK) return false;
            Path target = files.resolveName(parent, dialog.getValue());
            boolean replace = Files.exists(target);
            if (replace && !MessageDialog.openQuestion(editor.getSite().getShell(), "덮어쓰기 확인",
                    target.getFileName() + " 파일을 덮어쓸까요?")) return false;
            saveTo(editor, target, replace);
            return true;
        } catch (Exception ex) {
            Activator.error(editor.getSite().getShell(), "메모를 저장하지 못했습니다.", ex);
            return false;
        }
    }

    /** Commits through Eclipse's buffer manager, then rebinds the same editor tab. */
    public static void saveTo(ITextEditor editor, Path target, boolean replace) throws Exception {
        Activator.getDefault().files().check(target, false);
        if (!(editor instanceof IReusableEditor)) throw new IOException("이 편집기는 저장 위치 변경을 지원하지 않습니다.");
        if (Files.isDirectory(target)) throw new IOException("같은 이름의 폴더가 있습니다.");
        if (!replace && Files.exists(target)) throw new java.nio.file.FileAlreadyExistsException(target.toString());
        IFileStore store = EFS.getStore(target.toUri());
        FileStoreEditorInput input = new FileStoreEditorInput(store);
        for (org.eclipse.ui.IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    IEditorPart other = ref.getEditor(false);
                    if (other != null && other != editor && input.equals(other.getEditorInput()))
                        throw new IOException("대상 파일이 다른 편집 탭에 열려 있습니다. 먼저 해당 탭을 닫아 주세요.");
                }
            }
        }
        IDocument source = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (source == null) throw new IOException("편집 중인 내용을 읽을 수 없습니다.");
        String text = source.get();
        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        manager.connectFileStore(store, null);
        try {
            ITextFileBuffer buffer = manager.getFileStoreTextFileBuffer(store);
            if (buffer.isDirty() && buffer.getDocument() != source)
                throw new IOException("대상 파일에 저장하지 않은 변경이 있습니다.");
            buffer.setEncoding("UTF-8");
            buffer.getDocument().set(text);
            buffer.commit(null, true);
            ((IReusableEditor) editor).setInput(input);
        } finally {
            manager.disconnectFileStore(store, null);
        }
        Activator.getDefault().refresh();
    }
}

package dev.eclipsememo;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public final class SaveMemoHandler extends AbstractHandler {
    @Override public Object execute(ExecutionEvent event) {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (editor instanceof ITextEditor) SaveSupport.promptAndSave((ITextEditor) editor);
        return null;
    }
}

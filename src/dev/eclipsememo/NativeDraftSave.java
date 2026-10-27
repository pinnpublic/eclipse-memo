package dev.eclipsememo;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditor;

/** A handler scoped to this editor, removed as soon as its input is a real file. */
final class NativeDraftSave implements AutoCloseable {
    private final IHandlerService service;
    private final List<IHandlerActivation> activations = new ArrayList<>();

    NativeDraftSave(ITextEditor editor) {
        service = editor.getSite().getService(IHandlerService.class);
        AbstractHandler handler = new AbstractHandler() {
            @Override public Object execute(ExecutionEvent event) {
                if (SaveSupport.promptAndSave(editor)) {
                    close();
                    editor.getEditorSite().getActionBars().updateActionBars();
                }
                return null;
            }
        };
        activations.add(service.activateHandler(IWorkbenchCommandConstants.FILE_SAVE, handler));
        activations.add(service.activateHandler(IWorkbenchCommandConstants.FILE_SAVE_AS, handler));
    }

    @Override public void close() {
        for (IHandlerActivation activation : activations) service.deactivateHandler(activation);
        activations.clear();
    }
}

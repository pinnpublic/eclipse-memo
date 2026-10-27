package dev.eclipsememo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public final class MemoEditor extends TextEditor {
    public static final String ID = "dev.eclipsememo.editor";

    public MemoEditor() {
        // DraftInput has no global document-provider association in the Eclipse registry.
        setDocumentProvider(new TextFileDocumentProvider());
    }

    @Override public void doSave(IProgressMonitor monitor) {
        if (getEditorInput() instanceof DraftInput) {
            boolean saved = SaveSupport.promptAndSave(this);
            if (monitor != null) monitor.setCanceled(!saved);
        } else {
            super.doSave(monitor);
        }
    }

    @Override protected void performSaveAs(IProgressMonitor monitor) {
        boolean saved = SaveSupport.promptAndSave(this);
        if (monitor != null) monitor.setCanceled(!saved);
    }
}

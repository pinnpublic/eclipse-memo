package dev.eclipsememo;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public final class NewMemoHandler extends AbstractHandler {
    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        try { Activator.getDefault().newMemo(HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage()); }
        catch (Exception ex) { throw new ExecutionException("메모 편집기를 열지 못했습니다.", ex); }
        return null;
    }
}

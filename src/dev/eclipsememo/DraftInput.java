package dev.eclipsememo;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.ILocationProvider;

/** The document stays in memory; no draft file or project is created. */
public final class DraftInput implements IEditorInput, ILocationProvider {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private final String name = "새 메모 " + SEQUENCE.incrementAndGet();
    private final IPath location = org.eclipse.core.runtime.Path.fromOSString(
            Paths.get(System.getProperty("java.io.tmpdir"), "eclipse-memo-" + UUID.randomUUID() + ".txt").toString());

    @Override public boolean exists() { return false; }
    @Override public String getName() { return name; }
    @Override public String getToolTipText() { return "워크스페이스에 저장할 새 메모"; }
    @Override public IPersistableElement getPersistable() { return null; }
    @Override public ImageDescriptor getImageDescriptor() {
        return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor("memo.txt");
    }
    @Override public <T> T getAdapter(Class<T> type) {
        return type == ILocationProvider.class ? type.cast(this) : null;
    }
    @Override public IPath getPath(Object input) { return input == this ? location : null; }
}

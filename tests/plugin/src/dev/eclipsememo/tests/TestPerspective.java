package dev.eclipsememo.tests;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public final class TestPerspective implements IPerspectiveFactory {
    @Override public void createInitialLayout(IPageLayout layout) {
        layout.addView("org.eclipse.jdt.ui.PackageExplorer", IPageLayout.LEFT, 0.3f, layout.getEditorArea());
        layout.addView("org.eclipse.ui.navigator.ProjectExplorer", IPageLayout.BOTTOM, 0.5f,
                "org.eclipse.jdt.ui.PackageExplorer");
    }
}

package dev.eclipsememo;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public final class MemoWizard extends Wizard implements INewWizard {
    private IWorkbench workbench;
    private Text name;
    private WizardPage page;

    @Override public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        setWindowTitle("Workspace Memo");
    }

    @Override public void addPages() {
        page = new WizardPage("memo", "워크스페이스 메모", null) {
            @Override public void createControl(Composite parent) {
                Composite area = new Composite(parent, SWT.NONE);
                area.setLayout(new GridLayout(1, false));
                Label location = new Label(area, SWT.WRAP);
                try { location.setText("저장 위치: " + Activator.getDefault().files().root()); }
                catch (Exception ex) { setErrorMessage(ex.getMessage()); }
                new Label(area, SWT.NONE).setText("파일 이름:");
                name = new Text(area, SWT.BORDER);
                name.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false));
                name.setText("memo.txt");
                name.addModifyListener(event -> {
                    String error = WorkspaceFiles.validateName(name.getText());
                    setErrorMessage(error);
                    setPageComplete(error == null);
                });
                setControl(area);
            }
        };
        addPage(page);
    }

    @Override public boolean performFinish() {
        try {
            WorkspaceFiles files = Activator.getDefault().files();
            Path target = files.resolveName(files.root(), name.getText());
            Files.createFile(target);
            EditorSupport.open(workbench.getActiveWorkbenchWindow().getActivePage(), target);
            Activator.getDefault().refresh();
            return true;
        } catch (Exception ex) { page.setErrorMessage(ex.getMessage()); return false; }
    }
}

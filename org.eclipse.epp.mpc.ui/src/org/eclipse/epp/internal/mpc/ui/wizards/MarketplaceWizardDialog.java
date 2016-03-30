/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     JBoss (Pascal Rapicault) - Bug 406907 - Add p2 remediation page to MPC install flow
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import java.util.Arrays;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;

public class MarketplaceWizardDialog extends WizardDialog {
	private Button backButton;

	private Button nextButton;

	public MarketplaceWizardDialog(Shell parentShell, MarketplaceWizard newWizard) {
		//bug 424729 - make the wizard dialog modal
		//don't pass on parentShell, so we get a new top-level shell with its own taskbar entry
		//TODO is there some way to still get centering on the parentShell?
		super(null, newWizard);
		int shellStyle = getShellStyle();
		int allModal = SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL;
		shellStyle &= ~allModal;
		shellStyle |= SWT.MODELESS;
		setShellStyle(shellStyle);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setData(this);//make jface dialog accessible for swtbot
		new MarketplaceDropAdapter().installDropTarget(newShell);
		final IWorkbenchListener workbenchListener = new IWorkbenchListener() {

			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				MarketplaceWizardDialog wizardDialog = MarketplaceWizardDialog.this;
				Shell wizardShell = wizardDialog.getShell();
				if (wizardShell != null && !wizardShell.isDisposed()) {
					if (!forced) {
						MarketplaceWizard wizard = wizardDialog.getWizard();
						boolean hasPendingActions = false;
						IWizardPage currentPage = wizardDialog.getCurrentPage();
						if (currentPage != null && wizard != null) {
							if (currentPage == wizard.getCatalogPage()) {
								hasPendingActions = !wizard.getSelectionModel().getSelectedCatalogItems().isEmpty();
							} else {
								hasPendingActions = true;
							}
						}
						if (hasPendingActions) {
							Shell parentShell = activate(wizardDialog.getShell());
							MessageDialog messageDialog = new MessageDialog(parentShell, Messages.MarketplaceWizardDialog_PromptPendingActionsTitle,
									null, Messages.MarketplaceWizardDialog_PromptPendingActionsMessage,
									MessageDialog.QUESTION_WITH_CANCEL, new String[] { IDialogConstants.YES_LABEL,
											IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL },
									SWT.NONE);
							int result = messageDialog.open();
							switch (result) {
							case 0: //yes
								finishWizard();
								return false;
							case 1: //no
								break;
							case 3: //cancel
							case SWT.DEFAULT: //[x]
							default:
								return false;
							}
						}
					}
					if (forced) {
						wizardShell.close();
					} else {
						boolean closed = wizardDialog.close();
						return closed;
					}
				}
				return true;
			}

			private void finishWizard() {
				MarketplaceWizardDialog wizardDialog = MarketplaceWizardDialog.this;
				MarketplaceWizard wizard = wizardDialog.getWizard();
				IWizardPage currentPage = wizardDialog.getCurrentPage();
				if (currentPage == wizard.getCatalogPage()) {
					((MarketplacePage) currentPage).showNextPage();
				}
			}

			private Shell activate(Shell shell) {
				Shell activeShell = shell.getDisplay().getActiveShell();
				if (activeShell != shell) {
					Shell[] childShells = shell.getShells();
					if (childShells.length == 0 || !Arrays.asList(childShells).contains(activeShell)) {
						shell.forceActive();
						shell.forceFocus();
					}
				}
				if (activeShell == null) {
					activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
					if (activeShell == null) {
						activeShell = shell;
					}
				}
				return activeShell;
			}

			public void postShutdown(IWorkbench workbench) {
			}
		};
		PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
		newShell.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
			}
		});
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		switch (id) {
		case IDialogConstants.NEXT_ID:
			nextButton = button;
			break;
		case IDialogConstants.BACK_ID:
			backButton = button;
			break;
		}
		return button;
	}

	@Override
	protected void backPressed() {
		IWizardPage fromPage = getCurrentPage();
		super.backPressed();
		if (fromPage instanceof FeatureSelectionWizardPage
				&& ((FeatureSelectionWizardPage) fromPage).isInRemediationMode()) {
			((FeatureSelectionWizardPage) fromPage).flipToDefaultComposite();
		}
		IWizardPage previousPage = getCurrentPage();
		if (previousPage != fromPage) {
			if (fromPage instanceof IWizardPageAction) {
				IWizardPageAction sourceAction = (IWizardPageAction) fromPage;
				sourceAction.exit(false);
			}
			if (previousPage instanceof IWizardPageAction) {
				IWizardPageAction nextAction = (IWizardPageAction) previousPage;
				nextAction.enter(false);
			}
		}
	}

	@Override
	protected void nextPressed() {
		IWizardPage fromPage = getCurrentPage();
		if (fromPage instanceof IWizardPageAction) {
			IWizardPageAction sourceAction = (IWizardPageAction) fromPage;
			if (!sourceAction.exit(true)) {
				return;
			}
		}
		super.nextPressed();
		IWizardPage nextPage = getCurrentPage();
		if (nextPage != fromPage && nextPage instanceof IWizardPageAction) {
			IWizardPageAction nextAction = (IWizardPageAction) nextPage;
			nextAction.enter(true);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		AccessibleAdapter adapter = new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				final Button button = (Button) ((Accessible) e.getSource()).getControl();
				final String text = button.getText();
				e.result = text.replace('<', ' ').replace('>', ' ');
			}
		};
		backButton.getAccessible().addAccessibleListener(adapter);
		nextButton.getAccessible().addAccessibleListener(adapter);
	}

	@Override
	public MarketplaceWizard getWizard() {
		return (MarketplaceWizard) super.getWizard();
	}

	@Override
	public void updateButtons() {
		super.updateButtons();
		IWizardPage currentPage = getCurrentPage();
		if (currentPage != null) {
			boolean buttonsChanged = false;
			String nextButtonLabel = getNextButtonLabel(currentPage);
			if (!nextButtonLabel.equals(nextButton.getText())) {
				nextButton.setText(nextButtonLabel);
				setButtonLayoutData(nextButton);
				buttonsChanged = true;
			}
			String backButtonLabel = getBackButtonLabel(currentPage);
			if (!backButtonLabel.equals(backButton.getText())) {
				backButton.setText(backButtonLabel);
				setButtonLayoutData(backButton);
				buttonsChanged = true;
			}
			if (buttonsChanged) {
				Composite buttonBar = backButton.getParent();
				buttonBar.layout(true);
			}
		}
	}

	public String getNextButtonLabel(IWizardPage page) {
		if (page instanceof IWizardButtonLabelProvider) {
			IWizardButtonLabelProvider labelProvider = (IWizardButtonLabelProvider) page;
			String nextButtonLabel = labelProvider.getNextButtonLabel();
			if (nextButtonLabel != null) {
				return nextButtonLabel;
			}
		}
		return IDialogConstants.NEXT_LABEL;
	}

	public String getBackButtonLabel(IWizardPage page) {
		if (page instanceof IWizardButtonLabelProvider) {
			IWizardButtonLabelProvider labelProvider = (IWizardButtonLabelProvider) page;
			String backButtonLabel = labelProvider.getBackButtonLabel();
			if (backButtonLabel != null) {
				return backButtonLabel;
			}
		}
		return IDialogConstants.BACK_LABEL;
	}
}

/*******************************************************************************
 * Copyright (c) 2010 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.internal.mpc.ui.wizards;

import org.eclipse.epp.internal.mpc.ui.catalog.UserActionCatalogItem;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.CatalogViewer;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.DiscoveryResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

class InfoViewerItem extends UserActionViewerItem<UserActionCatalogItem> {
	public InfoViewerItem(Composite parent, DiscoveryResources resources,
			IShellProvider shellProvider, UserActionCatalogItem element, CatalogViewer viewer) {
		super(parent, resources, shellProvider, element, viewer);
		createContent();
	}

	@Override
	protected Control createActionLink(Composite parent) {
		String text = getLinkText();
		Label label = new Label(parent, SWT.NONE);
		label.setBackground(null);
		label.setText(text);
		return label;
	}

	@Override
	protected String getLinkText() {
		return this.getData().getDescription();
	}

	@Override
	protected void actionPerformed(Object data) {
		// ignore

	}
}
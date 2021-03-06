/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard.widgets;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.hamcrest.SelfDescribing;

public class SWTBotClickableStyledText extends SWTBotStyledText {

	public SWTBotClickableStyledText(StyledText styledText, SelfDescribing description) throws WidgetNotFoundException {
		super(styledText, description);
	}

	public SWTBotClickableStyledText(StyledText styledText) throws WidgetNotFoundException {
		super(styledText);
	}

	public static SWTBotClickableStyledText from(SWTBotStyledText botText) {
		if (botText instanceof SWTBotClickableStyledText) {
			return (SWTBotClickableStyledText) botText;
		}
		return new SWTBotClickableStyledText(botText.widget);
	}

	public void click(final int offset) {
		Point location = UIThreadRunnable.syncExec((Result<Point>) () -> {
			Point location1 = widget.getLocationAtOffset(offset);
			int lineHeight = widget.getLineHeight(offset);
			location1.y += lineHeight / 2;
			return location1;
		});
		click(location.x, location.y, false);
	}

	public void click(StyleRange range) {
		click(range.start + range.length / 2);
	}
}

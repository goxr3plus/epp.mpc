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
package org.eclipse.epp.internal.mpc.core.transport.httpclient;

import org.eclipse.epp.mpc.core.service.ITransport;
import org.eclipse.epp.mpc.core.service.ITransportFactory;

public class HttpClientTransportFactory implements ITransportFactory {

	private HttpClientTransport transport;

	@Override
	public ITransport getTransport() {
		return transport;
	}

	public void setTransport(HttpClientTransport transport) {
		this.transport = transport;
	}

	public void bindTransport(HttpClientTransport transport) {
		setTransport(transport);
	}

	public void unbindTransport(HttpClientTransport transport) {
		if (transport == this.transport) {
			setTransport(null);
		}
	}
}

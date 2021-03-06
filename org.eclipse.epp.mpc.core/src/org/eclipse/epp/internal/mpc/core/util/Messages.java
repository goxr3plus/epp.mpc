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
package org.eclipse.epp.internal.mpc.core.util;

import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.epp.internal.mpc.core.util.messages"; //$NON-NLS-1$

	public static String P2TransportFactory_AuthenticationFailed;

	public static String DefaultMarketplaceService_serviceUnavailable503;

	public static String FallbackTransportFactory_disablingTransport;

	public static String FallbackTransportFactory_fallbackStream;

	public static String ProxyAuthenticator_prompt;

	public static String ProxyHelper_replacingAuthenticator;

	public static String ServiceUtil_ignoringIncompatibleServiceProperty;

	public static String TransportFactory_available;

	public static String TransportFactory_DefaultService;

	public static String TransportFactory_DefaultTransportUnavailable_UseFallback;

	public static String TransportFactory_FallbackService;

	public static String TransportFactory_LegacyFallbackCreationError;

	public static String TransportFactory_LegacyFallbacksError;

	public static String TransportFactory_NoLegacyTransportFactoriesError;

	public static String TransportFactory_ServiceErrorAppliedFilter;

	public static String TransportFactory_ServiceErrorDetails;

	public static String TransportFactory_ServiceErrorRegisteredService;

	public static String TransportFactory_ServiceErrorNoneAvailable;

	public static String TransportFactory_ServiceErrorNotFound;

	public static String TransportFactory_ServiceErrorServiceReference;

	public static String TransportFactory_ServiceErrorUnregistered;

	public static String TransportFactory_StaticFactoryInfo;

	public static String TransportFactory_transportAvailabilityError;

	public static String TransportFactory_unavailable;

	public static String TransportFactory_UseLegacyFallback;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

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
package org.eclipse.epp.internal.mpc.core.service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.epp.internal.mpc.core.transport.httpclient.RequestTemplate;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.osgi.util.NLS;
import org.eclipse.userstorage.IBlob;
import org.eclipse.userstorage.internal.Session;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.util.ConflictException;
import org.eclipse.userstorage.util.NoServiceException;
import org.eclipse.userstorage.util.NotFoundException;
import org.eclipse.userstorage.util.ProtocolException;

@SuppressWarnings("restriction")
public class UserFavoritesService extends AbstractDataStorageService implements IUserFavoritesService {

	private static final Pattern JSON_CONTENT_ID_PATTERN = Pattern
			.compile("\\{[^\\}]*\"content_id\"\\s*:\\s*\"([^\"]*)\"[^\\}]*\\}"); //$NON-NLS-1$

	private static final Pattern JSON_MPC_FAVORITES_PATTERN = Pattern
			.compile("\\{\\s*\"mpc_favorites\"\\s*:\\s*\\[((?:\\s*\\{[^\\{\\}]+\\}\\s*,?\\s*)*)\\],.*\\}"); //$NON-NLS-1$

	private static final Pattern MARKETPLACE_USER_URL_PATTERN = Pattern.compile("https?://.*/user/([^/]+)"); //$NON-NLS-1$

	private static final Pattern USER_MAIL_PATTERN = Pattern.compile(".+\\@.+"); //$NON-NLS-1$

	private static final String FAVORITES_API__ENDPOINT = "/api/mpc_favorites?{0}={1}"; //$NON-NLS-1$

	private static final String FAVORITES_API__USER_MAIL = "mail"; //$NON-NLS-1$

	private static final String FAVORITES_API__USER_NAME = "name"; //$NON-NLS-1$

	private static final String KEY = "mpc_favorites"; //$NON-NLS-1$

	private static final int RETRY_COUNT = 3;

	private static final String SEPARATOR = ","; //$NON-NLS-1$

	private final Map<String, Integer> favoritesCorrections = new HashMap<String, Integer>();

	private final Set<String> favorites = new HashSet<String>();

	protected IBlob getFavoritesBlob() {
		return getStorageService().getBlob(KEY);
	}

	public Integer getFavoriteCount(INode node) {
		Integer favorited = node.getFavorited();
		if (favorited == null) {
			return null;
		}
		Integer correction = favoritesCorrections.get(node.getId());
		if (correction != null) {
			favorited += correction;
		}
		return favorited;
	}

	public Set<String> getLastFavoriteIds() {
		return Collections.unmodifiableSet(favorites);
	}

	public Set<String> getFavoriteIds(IProgressMonitor monitor)
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.DefaultMarketplaceService_FavoritesRetrieve, 1000);
		try {
			String favoritesData = getFavoritesBlob().getContentsUTF();
			progress.worked(950);//FIXME waiting for USS bug 488335 to have proper progress and cancelation
			Set<String> result = parseFavoritesBlobData(favoritesData);
			synchronized (this) {
				favorites.clear();
				favorites.addAll(result);
			}
			return result;
		} catch (NotFoundException ex) {
			//the user does not yet have favorites
			return new LinkedHashSet<String>();
		} catch (OperationCanceledException ex) {
			throw processProtocolException(ex);
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
	}

	public List<INode> getLastFavorites() {
		Set<String> favoriteIds = getLastFavoriteIds();
		List<INode> favoriteNodes = toNodes(favoriteIds);
		return favoriteNodes;
	}

	public List<INode> getFavorites(IProgressMonitor monitor)
			throws NoServiceException, NotAuthorizedException, IllegalStateException, IOException {
		Set<String> favoriteIds = getFavoriteIds(monitor);
		List<INode> favoriteNodes = toNodes(favoriteIds);
		return favoriteNodes;
	}

	private static List<INode> toNodes(Collection<String> favoriteIds) {
		List<INode> favoriteNodes = new ArrayList<INode>(favoriteIds.size());
		for (String nodeId : favoriteIds) {
			INode node = QueryHelper.nodeById(nodeId);
			favoriteNodes.add(node);
		}
		return favoriteNodes;
	}

	protected Set<String> parseFavoritesBlobData(String favoritesData) {
		Set<String> favoriteIds = new LinkedHashSet<String>();
		for (StringTokenizer tokenizer = new StringTokenizer(favoritesData, SEPARATOR); tokenizer.hasMoreTokens();) {
			String nodeId = tokenizer.nextToken();
			favoriteIds.add(nodeId);
		}
		return favoriteIds;
	}

	public void setFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NoServiceException, ConflictException, NotAuthorizedException, IllegalStateException, IOException
	{
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		String favoritesData = createFavoritesBlobData(nodes);
		try {
			getFavoritesBlob().setContentsUTF(favoritesData);
			progress.worked(900);//FIXME waiting for USS bug 488335 to have proper progress and cancelation
		} catch (OperationCanceledException ex) {
			throw processProtocolException(ex);
		} catch (ProtocolException ex) {
			throw processProtocolException(ex);
		}
		synchronized (this) {
			SubMonitor notifyNewProgress = SubMonitor.convert(progress.newChild(50), nodes.size());
			Set<String> newFavorites = new HashSet<String>();
			for (INode node : nodes) {
				String id = node.getId();
				if (newFavorites.add(id)) {
					boolean newFavorite = favorites.add(id);
					if (newFavorite) {
						didChangeFavorite(id, true);
					}
				}
				notifyNewProgress.worked(1);
			}
			SubMonitor notifyRemovedProgress = SubMonitor.convert(progress.newChild(50), favorites.size());
			for (Iterator<String> i = favorites.iterator(); i.hasNext();) {
				String id = i.next();
				if (!newFavorites.contains(id)) {
					i.remove();
					didChangeFavorite(id, false);
				}
				notifyRemovedProgress.worked(1);
			}
		}
	}

	private void didChangeFavorite(String nodeId, boolean favorite) {
		Integer correction = favoritesCorrections.get(nodeId);
		if (correction == null) {
			correction = 0;
		}
		correction += favorite ? 1 : -1;
		if (correction < -1) {
			correction = -1;
		} else if (correction > 1) {
			correction = 1;
		}
		if (correction == 0) {
			favoritesCorrections.remove(nodeId);
		} else {
			favoritesCorrections.put(nodeId, correction);
		}
	}

	protected String createFavoritesBlobData(Collection<? extends INode> nodes) {
		List<String> nodeIds = new ArrayList<String>(nodes.size());
		for (INode node : nodes) {
			nodeIds.add(node.getId());
		}
		Collections.sort(nodeIds);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String nodeId : nodeIds) {
			if (first) {
				first = false;
			} else {
				builder.append(SEPARATOR);
			}
			builder.append(nodeId);
		}
		return builder.toString();
	}

	public void setFavorite(INode node, boolean favorite, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(Collections.singleton(node), favorite, monitor);
	}

	public void addFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(nodes, true, monitor);
	}

	public void removeFavorites(Collection<? extends INode> nodes, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		alterFavorites(nodes, false, monitor);
	}

	private void alterFavorites(Collection<? extends INode> nodes, boolean favorite, IProgressMonitor monitor)
			throws NotAuthorizedException, ConflictException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		ConflictException conflictException = null;
		for (int i = 0; i < RETRY_COUNT; i++) {
			try {
				progress.setWorkRemaining(1000);
				doAlterFavorites(nodes, favorite, progress.newChild(800));
				progress.done();
				return;
			} catch (ConflictException e) {
				conflictException = e;
			} catch (OperationCanceledException ex) {
				throw processProtocolException(ex);
			} catch (ProtocolException ex) {
				throw processProtocolException(ex);
			}
		}
		if (conflictException != null) {
			throw conflictException;
		}
	}

	private void doAlterFavorites(Collection<? extends INode> nodes, boolean favorite, IProgressMonitor monitor)
			throws ConflictException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.UserFavoritesService_SettingUserFavorites, 1000);
		List<INode> favorites = getFavorites(progress.newChild(300));
		for (INode node : nodes) {
			INode currentFavorite = QueryHelper.findById(favorites, node);
			if (currentFavorite != null && !favorite) {
				favorites.remove(currentFavorite);
			} else if (currentFavorite == null && favorite) {
				favorites.add(node);
			}
		}
		setFavorites(favorites, progress.newChild(700));
	}

	public List<INode> getFavorites(String user, IProgressMonitor monitor) throws IOException {
		List<String> nodeIds = getFavoriteIds(user, monitor);
		return toNodes(nodeIds);
	}

	public List<String> getFavoriteIds(String user, IProgressMonitor monitor) throws IOException {
		Matcher matcher = MARKETPLACE_USER_URL_PATTERN.matcher(user);
		if (matcher.find()) {
			return importUserFavoriteIdsFromUserId(matcher.group(1));
		}
		matcher = USER_MAIL_PATTERN.matcher(user);
		if (matcher.find()) {
			return importUserFavoriteIdsFromUserEmail(user);
		}
		return importUserFavoriteIdsFromUserId(user);
	}

	private List<String> importUserFavoriteIdsFromUserId(String userId) throws IOException {
		return importUserFavoriteIds(FAVORITES_API__USER_NAME, userId);
	}

	private List<String> importUserFavoriteIds(String userIdParam, String userId) throws IOException {
		try {
			return new RequestTemplate<List<String>>() {

				@Override
				protected Request configureRequest(Request request, URI uri) {
					return super.configureRequest(request, uri).setHeader(HttpHeaders.USER_AGENT, Session.USER_AGENT_ID)
							.addHeader(HttpHeaders.CONTENT_TYPE, Session.APPLICATION_JSON) //
							.addHeader(HttpHeaders.ACCEPT, Session.APPLICATION_JSON);
				}

				@Override
				protected List<String> handleResponseStream(InputStream content) throws IOException {
					List<String> favoriteIds = new ArrayList<String>();
					String body = read(content);
					Matcher matcher = JSON_MPC_FAVORITES_PATTERN.matcher(body);
					if (matcher.find()) {
						String favorites = matcher.group(1);
						Matcher contentIdMatcher = JSON_CONTENT_ID_PATTERN.matcher(favorites);
						while (contentIdMatcher.find()) {
							String id = contentIdMatcher.group(1);
							favoriteIds.add(id);
						}
					}
					return favoriteIds;
				}

				@Override
				protected Request createRequest(URI uri) {
					return Request.Get(uri);
				}
			}.execute(getStorageService().getServiceUri()
					.resolve(NLS.bind(FAVORITES_API__ENDPOINT, userIdParam, userId)));
		} catch (FileNotFoundException e) {
			return new ArrayList<String>();
		}
	}

	private List<String> importUserFavoriteIdsFromUserEmail(String email) throws IOException {
		return importUserFavoriteIds(FAVORITES_API__USER_MAIL, email);
	}

	private static String read(InputStream in) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtil.copy(in, baos);
			return StringUtil.fromUTF(baos.toByteArray());
		} finally {
			IOUtil.close(in);
		}
	}
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.cache;

import java.util.concurrent.CompletionStage;

import org.wildfly.clustering.server.manager.DecoratedManager;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionStatistics;

/**
 * A session manager decorator.
 * @author Paul Ferraro
 * @param <C> the session context type
 */
public class DecoratedSessionManager<C> extends DecoratedManager<String> implements SessionManager<C> {

	private final SessionManager<C> manager;

	/**
	 * Creates a session manager decorator.
	 * @param manager the decorated session manager.
	 */
	public DecoratedSessionManager(SessionManager<C> manager) {
		super(manager);
		this.manager = manager;
	}

	@Override
	public CompletionStage<Session<C>> createSessionAsync(String id) {
		return this.manager.createSessionAsync(id);
	}

	@Override
	public CompletionStage<Session<C>> findSessionAsync(String id) {
		return this.manager.findSessionAsync(id);
	}

	@Override
	public CompletionStage<ImmutableSession> findImmutableSessionAsync(String id) {
		return this.manager.findImmutableSessionAsync(id);
	}

	@Override
	public Session<C> getDetachedSession(String id) {
		return this.manager.getDetachedSession(id);
	}

	@Override
	public SessionStatistics getStatistics() {
		return this.manager.getStatistics();
	}
}

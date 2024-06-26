/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session;

/**
 * Represents a session.
 * @author Paul Ferraro
 * @param <C> the session context type
 */
public interface Session<C> extends ImmutableSession, AutoCloseable {
	@Override
	SessionMetaData getMetaData();

	/**
	 * Invalidates this session.
	 * @throws IllegalStateException if this session was already invalidated.
	 */
	void invalidate();

	/**
	 * Returns the local context of this session.
	 * The local context is *not* replicated to other nodes in the cluster.
	 * @return a local context
	 */
	C getContext();

	@Override
	void close();
}

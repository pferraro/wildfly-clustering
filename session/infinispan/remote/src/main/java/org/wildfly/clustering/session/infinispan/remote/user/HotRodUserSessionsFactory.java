/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.infinispan.remote.user;

import static org.wildfly.clustering.cache.function.Functions.constantFunction;
import static org.wildfly.common.function.Functions.discardingConsumer;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheEntryMutator;
import org.wildfly.clustering.session.cache.user.MutableUserSessions;
import org.wildfly.clustering.session.cache.user.UserSessionsFactory;
import org.wildfly.clustering.session.user.UserSessions;

/**
 * @param <D> the deployment type
 * @param <S> the session type
 * @author Paul Ferraro
 */
public class HotRodUserSessionsFactory<D, S> implements UserSessionsFactory<Map<D, S>, D, S> {

	private final RemoteCache<UserSessionsKey, Map<D, S>> readCache;
	private final RemoteCache<UserSessionsKey, Map<D, S>> writeCache;

	public HotRodUserSessionsFactory(RemoteCacheConfiguration configuration) {
		this.readCache = configuration.getCache();
		this.writeCache = configuration.getIgnoreReturnCache();
	}

	@Override
	public UserSessions<D, S> createUserSessions(String id, Map<D, S> value) {
		UserSessionsKey key = new UserSessionsKey(id);
		CacheEntryMutator mutator = new RemoteCacheEntryMutator<>(this.writeCache, key, value);
		return new MutableUserSessions<>(value, mutator);
	}

	@Override
	public CompletionStage<Map<D, S>> createValueAsync(String id, Void context) {
		Map<D, S> sessions = new ConcurrentHashMap<>();
		return this.writeCache.putAsync(new UserSessionsKey(id), sessions).thenApply(constantFunction(sessions));
	}

	@Override
	public CompletionStage<Map<D, S>> findValueAsync(String id) {
		return this.readCache.getAsync(new UserSessionsKey(id));
	}

	@Override
	public CompletionStage<Void> removeAsync(String id) {
		return this.writeCache.removeAsync(new UserSessionsKey(id)).thenAccept(discardingConsumer());
	}
}

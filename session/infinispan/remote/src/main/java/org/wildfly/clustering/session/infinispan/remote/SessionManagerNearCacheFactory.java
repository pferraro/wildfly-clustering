/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.infinispan.remote;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.configuration.NearCacheConfiguration;
import org.infinispan.client.hotrod.near.NearCache;
import org.infinispan.client.hotrod.near.NearCacheFactory;
import org.wildfly.clustering.cache.infinispan.remote.near.CaffeineNearCache;
import org.wildfly.clustering.cache.infinispan.remote.near.EvictionListener;
import org.wildfly.clustering.cache.infinispan.remote.near.SimpleKeyWeigher;
import org.wildfly.clustering.session.infinispan.remote.attributes.SessionAttributesKey;
import org.wildfly.clustering.session.infinispan.remote.metadata.SessionAccessMetaDataKey;
import org.wildfly.clustering.session.infinispan.remote.metadata.SessionCreationMetaDataKey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * A near-cache factory based on max-active-sessions.
 * @author Paul Ferraro
 */
public class SessionManagerNearCacheFactory implements NearCacheFactory {

	private final Integer maxActiveSessions;

	public SessionManagerNearCacheFactory(Integer maxActiveSessions) {
		this.maxActiveSessions = maxActiveSessions;
	}

	@Override
	public <K, V> NearCache<K, V> createNearCache(NearCacheConfiguration config, BiConsumer<K, MetadataValue<V>> removedConsumer) {
		EvictionListener<K, V> listener = (this.maxActiveSessions != null) ? new EvictionListener<>(removedConsumer, new InvalidationListener()) : null;
		Caffeine<Object, Object> builder = Caffeine.newBuilder();
		if (listener != null) {
			builder.executor(Runnable::run)
					.maximumWeight(this.maxActiveSessions.longValue())
					.weigher(new SimpleKeyWeigher(SessionCreationMetaDataKey.class::isInstance))
					.removalListener(listener);
		}
		Cache<K, MetadataValue<V>> cache = builder.build();
		if (listener != null) {
			listener.accept(cache);
		}
		return new CaffeineNearCache<>(cache);
	}

	private static class InvalidationListener implements BiConsumer<Cache<Object, MetadataValue<Object>>, Map.Entry<Object, Object>> {

		@Override
		public void accept(Cache<Object, MetadataValue<Object>> cache, Map.Entry<Object, Object> entry) {
			Object key = entry.getKey();
			if (key instanceof SessionCreationMetaDataKey) {
				String id = ((SessionCreationMetaDataKey) key).getId();
				List<Object> keys = new LinkedList<>();
				keys.add(new SessionAccessMetaDataKey(id));
				keys.add(new SessionAttributesKey(id));
				cache.invalidateAll(keys);
			}
		}
	}
}

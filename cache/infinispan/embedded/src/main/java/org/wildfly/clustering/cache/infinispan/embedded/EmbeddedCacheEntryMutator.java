/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.cache.infinispan.AbstractCacheEntryMutator;
import org.wildfly.clustering.function.Consumer;

/**
 * Mutates a given cache entry.
 * @param <K> the cache entry key type
 * @param <V> the cache entry value type
 * @author Paul Ferraro
 */
public class EmbeddedCacheEntryMutator<K, V> extends AbstractCacheEntryMutator {
	private final Cache<K, V> cache;
	private final K key;
	private final V value;

	EmbeddedCacheEntryMutator(Cache<K, V> cache, Map.Entry<K, V> entry) {
		this(cache, entry.getKey(), entry.getValue());
	}

	EmbeddedCacheEntryMutator(Cache<K, V> cache, K key, V value) {
		this.cache = cache;
		this.key = key;
		this.value = value;
	}

	@Override
	public CompletionStage<Void> mutateAsync() {
		Duration maxIdleDuration = this.get();
		long seconds = maxIdleDuration.getSeconds();
		int nanos = maxIdleDuration.getNano();
		if (nanos > 0) {
			seconds += 1;
		}
		// Use FAIL_SILENTLY to prevent mutation from failing locally due to remote exceptions
		return this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FAIL_SILENTLY).putAsync(this.key, this.value, 0L, TimeUnit.SECONDS, seconds, TimeUnit.SECONDS).thenAccept(Consumer.empty());
	}
}

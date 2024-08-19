/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import jakarta.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.CacheCollection;
import org.infinispan.CacheSet;
import org.infinispan.cache.impl.AbstractDelegatingAdvancedCache;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.infinispan.context.Flag;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.wildfly.clustering.cache.infinispan.embedded.util.CacheCollections;
import org.wildfly.clustering.cache.infinispan.tx.BlockingTransactionManager;
import org.wildfly.clustering.server.util.BlockingExecutorService;

/**
 * A cache decorator that executes all cache operations via a {@link BlockingExecutorService}.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class BlockingCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

	private final BlockingExecutorService executor;
	private final Map<Object, CacheEventFilter<? super K, ? super V>> listeners;

	public BlockingCache(AdvancedCache<K, V> cache) {
		this(cache, BlockingExecutorService.newInstance(cache::start, cache::stop), Collections.synchronizedMap(new LinkedHashMap<>()));
	}

	private BlockingCache(AdvancedCache<K, V> cache, BlockingExecutorService executor, Map<Object, CacheEventFilter<? super K, ? super V>> listeners) {
		super(cache);
		this.executor = executor;
		this.listeners = listeners;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AdvancedCache rewrap(AdvancedCache cache) {
		return new BlockingCache<>(cache, this.executor, this.listeners);
	}

	@Override
	public TransactionManager getTransactionManager() {
		return new BlockingTransactionManager(super.getTransactionManager(), this.executor);
	}

	@Override
	public <C> void addListener(Object listener, CacheEventFilter<? super K, ? super V> filter, CacheEventConverter<? super K, ? super V, C> converter) {
		if (this.listeners.put(listener, filter) == null) {
			super.addListener(listener, filter, converter);
		}
	}

	@Override
	public void removeListener(Object listener) {
		if (this.listeners.remove(listener) != null) {
			super.removeListener(listener);
		}
	}

	@Override
	public void clear() {
		this.executor.execute(this.cache::clear);
	}

	@Override
	public CompletableFuture<Void> clearAsync() {
		return this.executor.executeAsync(this.cache::clearAsync).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public boolean containsKey(Object key) {
		return this.executor.execute(() -> this.cache.containsKey(key)).orElse(false);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.executor.execute(() -> this.cache.containsValue(value)).orElse(false);
	}

	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, Metadata metadata) {
		return this.executor.execute(() -> this.cache.compute(key, remappingFunction, metadata)).orElse(null);
	}

	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, Metadata metadata) {
		return this.executor.execute(() -> this.cache.computeIfAbsent(key, mappingFunction, metadata)).orElse(null);
	}

	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction, Metadata metadata) {
		return this.executor.execute(() -> this.cache.computeIfPresent(key, remappingFunction, metadata)).orElse(null);
	}

	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		this.executor.execute(() -> this.cache.forEach(action));
	}

	@Override
	public V get(Object key) {
		return this.executor.execute(() -> this.cache.get(key)).orElse(null);
	}

	@Override
	public CompletableFuture<V> getAsync(K key) {
		return this.executor.executeAsync(() -> this.cache.getAsync(key)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return this.executor.execute(() -> this.cache.getOrDefault(key, defaultValue)).orElse(null);
	}

	@Override
	public CacheSet<Map.Entry<K, V>> entrySet() {
		return this.executor.execute(this.cache::entrySet).<CacheSet<Map.Entry<K, V>>>map(set -> new BlockingCacheSet<>(set, this.executor)).orElse(CacheCollections.emptySet());
	}

	@Override
	public void evict(K key) {
		this.executor.execute(() -> this.cache.evict(key));
	}

	@Override
	public boolean isEmpty() {
		return this.executor.execute(this.cache::isEmpty).orElse(true);
	}

	@Override
	public CacheSet<K> keySet() {
		return this.executor.execute(this.cache::keySet).<CacheSet<K>>map(set -> new BlockingCacheSet<>(set, this.executor)).orElse(CacheCollections.emptySet());
	}

	@Override
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction, Metadata metadata) {
		return this.executor.execute(() -> this.cache.merge(key, value, remappingFunction, metadata)).orElse(value);
	}

	@Override
	public V put(K key, V value, Metadata metadata) {
		return this.executor.execute(() -> this.cache.put(key, value, metadata)).orElse(null);
	}

	@Override
	public CompletableFuture<V> putAsync(K key, V value, Metadata metadata) {
		return this.executor.executeAsync(() -> this.cache.putAsync(key, value, metadata)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map, Metadata metadata) {
		this.executor.execute(() -> this.cache.putAll(map, metadata));
	}

	@Override
	public CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> map, Metadata metadata) {
		return this.executor.executeAsync(() -> this.cache.putAllAsync(map, metadata)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public void putForExternalRead(K key, V value, Metadata metadata) {
		this.executor.execute(() -> this.cache.putForExternalRead(key, value, metadata));
	}

	@Override
	public V putIfAbsent(K key, V value, Metadata metadata) {
		return this.executor.execute(() -> this.cache.putIfAbsent(key, value, metadata)).orElse(null);
	}

	@Override
	public CompletableFuture<V> putIfAbsentAsync(K key, V value, Metadata metadata) {
		return this.executor.executeAsync(() -> this.cache.putIfAbsentAsync(key, value, metadata)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public V remove(Object key) {
		return this.executor.execute(() -> this.cache.remove(key)).orElse(null);
	}

	@Override
	public CompletableFuture<V> removeAsync(Object key) {
		return this.executor.executeAsync(() -> this.cache.removeAsync(key)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public boolean remove(Object key, Object value) {
		return this.executor.execute(() -> this.cache.remove(key, value)).orElse(false);
	}

	@Override
	public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
		return this.executor.executeAsync(() -> this.cache.removeAsync(key, value)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedFalse());
	}

	@Override
	public V replace(K key, V value, Metadata metadata) {
		return this.executor.execute(() -> this.cache.replace(key, value, metadata)).orElse(null);
	}

	@Override
	public CompletableFuture<V> replaceAsync(K key, V value, Metadata metadata) {
		return this.executor.executeAsync(() -> this.cache.replaceAsync(key, value, metadata)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue, Metadata metadata) {
		return this.executor.execute(() -> this.cache.replace(key, oldValue, newValue, metadata)).orElse(null);
	}

	@Override
	public CompletableFuture<Boolean> replaceAsync(K key, V oldValue, V newValue, Metadata metadata) {
		return this.executor.executeAsync(() -> this.cache.replaceAsync(key, oldValue, newValue, metadata)).map(CompletionStage::toCompletableFuture).orElse(CompletableFutures.completedNull());
	}

	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		this.executor.execute(() -> this.cache.replaceAll(function));
	}

	@Override
	public int size() {
		return this.executor.execute(this.cache::size).orElse(0);
	}

	@Override
	public CompletableFuture<Long> sizeAsync() {
		return this.executor.executeAsync(this.cache::sizeAsync).map(CompletionStage::toCompletableFuture).orElse(CompletableFuture.completedFuture(0L));
	}

	@Override
	public CacheCollection<V> values() {
		return this.executor.execute(this.cache::values).<CacheCollection<V>>map(collection -> new BlockingCacheCollection<>(collection, this.executor)).orElse(CacheCollections.emptyCollection());
	}

	@Override
	public AdvancedCache<K, V> noFlags() {
		return this.executor.execute(this.cache::noFlags).orElse(this);
	}

	@Override
	public AdvancedCache<K, V> withFlags(Flag flag) {
		return this.executor.execute(() -> this.cache.withFlags(flag)).orElse(this);
	}

	@Override
	public AdvancedCache<K, V> withFlags(Flag... flags) {
		return this.executor.execute(() -> this.cache.withFlags(flags)).orElse(this);
	}

	@Override
	public AdvancedCache<K, V> withFlags(Collection<Flag> flags) {
		return this.executor.execute(() -> this.cache.withFlags(flags)).orElse(this);
	}
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import javax.transaction.xa.Xid;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.infinispan.client.hotrod.transaction.manager.RemoteXid;
import org.infinispan.commons.tx.TransactionImpl;
import org.infinispan.commons.util.concurrent.CompletableFutures;
import org.wildfly.clustering.cache.infinispan.remote.transaction.TransactionKey;
import org.wildfly.clustering.cache.infinispan.transaction.TransactionContextFactory;
import org.wildfly.clustering.context.Context;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.function.Supplier;

/**
 * A remote cache that performs locking reads if a transaction is active.
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @author Paul Ferraro
 */
public class ReadForUpdateRemoteCache<K, V> extends RemoteCacheDecorator<K, V> {
	static final System.Logger LOGGER = System.getLogger(ReadForUpdateRemoteCache.class.getName());

	interface SynchronizationFactory extends Function<Xid, CompletableFuture<Synchronization>>, Synchronization {
	}

	private final BiFunction<K, Transaction, CompletableFuture<Synchronization>> syncFactory;
	private final TransactionContextFactory contextFactory;

	/**
	 * Decorates a cache with read-for-update semantics.
	 * @param cache a remote cache.
	 */
	public ReadForUpdateRemoteCache(RemoteCache<K, V> cache) {
		this((InternalRemoteCache<K, V>) cache);
	}

	@SuppressWarnings("unchecked")
	ReadForUpdateRemoteCache(InternalRemoteCache<K, V> cache) {
		super(cache.getRemoteCacheContainer(), cache, ReadForUpdateRemoteCache::new);
		TransactionContextFactory contextFactory = TransactionContextFactory.of(cache.getTransactionManager());
		RemoteCache<TransactionKey<K>, Xid> putCache = (RemoteCache<TransactionKey<K>, Xid>) cache.withFlags(Flag.FORCE_RETURN_VALUE, Flag.SKIP_LISTENER_NOTIFICATION, Flag.SKIP_CACHE_LOAD);
		RemoteCache<TransactionKey<K>, Xid> removeCache = (RemoteCache<TransactionKey<K>, Xid>) cache.withFlags(Flag.SKIP_LISTENER_NOTIFICATION, Flag.SKIP_CACHE_LOAD);
		Duration maxTxDuration = Duration.ofMillis(cache.getRemoteCacheContainer().getConfiguration().transactionTimeout());
		Xid noTxId = RemoteXid.create(UUID.randomUUID());
		this.contextFactory = contextFactory;
		this.syncFactory = new BiFunction<>() {
			@Override
			public CompletableFuture<Synchronization> apply(K key, Transaction suspendedTx) {
				TransactionKey<K> currentTxKey = new TransactionKey<>(key);
				Xid currentTxId = ((TransactionImpl) suspendedTx).getXid();
				Instant timeout = Instant.now().plus(maxTxDuration);
				return new SynchronizationFactory() {
					@Override
					public CompletableFuture<Synchronization> apply(Xid txId) {
						if (txId == null) {
							// TX entry was newly created
							return CompletableFuture.completedFuture(this);
						}
						if (currentTxId.equals(txId)) {
							// TX entry already existed
							return CompletableFutures.completedNull();
						}
						// Try to create TX entry
						return putCache.putIfAbsentAsync(currentTxKey, currentTxId, maxTxDuration.toMillis(), TimeUnit.MILLISECONDS).thenCompose(this).orTimeout(Duration.between(Instant.now(), timeout).toNanos(), TimeUnit.NANOSECONDS);
					}

					@Override
					public void beforeCompletion() {
						// Do nothing
					}

					@Override
					public void afterCompletion(int status) {
						// Remove TX entry (outside of TX scope) without blocking
						try (Context<Transaction> context = contextFactory.suspendWithContext()) {
							removeCache.removeAsync(currentTxKey, currentTxId);
						}
					}
				}.apply(noTxId);
			}
		};
	}

	@Override
	public CompletableFuture<V> getAsync(K key) {
		return this.execute(() -> super.getAsync(key), Set.of(key));
	}

	@Override
	public CompletableFuture<MetadataValue<V>> getWithMetadataAsync(K key) {
		return this.execute(() -> super.getWithMetadataAsync(key), Set.of(key));
	}

	@SuppressWarnings("unchecked")
	@Override
	public CompletableFuture<Map<K, V>> getAllAsync(Set<?> keys) {
		if (keys.isEmpty()) return CompletableFutures.completedEmptyMap();
		return this.execute(() -> super.getAllAsync(keys), (Set<K>) keys);
	}

	private <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation, Set<? extends K> keys) {
		try (Context<Transaction> suspended = this.contextFactory.suspendWithContext()) {
			Transaction suspendedTx = suspended.get();
			return (suspendedTx != null) ? this.sync(keys, suspendedTx).thenCompose(synchronization -> {
				if (synchronization != null) {
					try (Context<Transaction> resumed = this.contextFactory.resumeWithContext(suspendedTx)) {
						resumed.get().registerSynchronization(synchronization);
					} catch (RollbackException e) {
						synchronization.afterCompletion(Status.STATUS_MARKED_ROLLBACK);
					} catch (SystemException e) {
						throw new IllegalStateException(e);
					}
				}
				return operation.get();
			}) : operation.get();
		}
	}

	private CompletableFuture<Synchronization> sync(Set<? extends K> keys, Transaction suspendedTx) {
		if (keys.size() == 1) return this.syncFactory.apply(keys.iterator().next(), suspendedTx);
		AtomicInteger remaining = new AtomicInteger(keys.size());
		CompletableFuture<Synchronization> result = new CompletableFuture<>();
		for (K key : keys) {
			this.syncFactory.apply(key, suspendedTx).whenComplete((xid, exception) -> {
				if (exception != null) {
					result.completeExceptionally(exception);
				} else if (remaining.decrementAndGet() == 0) {
					result.complete(null);
				}
			});
		}
		return result;
	}
}

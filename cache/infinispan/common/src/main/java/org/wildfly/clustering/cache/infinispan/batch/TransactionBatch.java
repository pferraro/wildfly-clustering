/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.batch;

import java.util.function.Function;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.cache.batch.SuspendedBatch;

/**
 * {@link TransactionBatch} that associates and exposes the underlying transaction.
 * @author Paul Ferraro
 */
public interface TransactionBatch extends Batch, SuspendedBatch {
	/**
	 * Returns the transaction associated with this batch
	 * @return a transaction
	 */
	Transaction getTransaction();

	/**
	 * Returns an interposed batch.
	 * @return an interposed batch.
	 */
	TransactionBatch interpose();

	/**
	 * Factory for creating {@link TransactionBatch} instances.
	 * @author Paul Ferraro
	 */
	interface Factory extends Batch.Factory {
		/**
		 * Returns a transaction-based {@link Batch} factory.
		 * @param <E> the target exception type
		 * @param containerName the name of the cache container
		 * @param cacheName the name of the cache
		 * @param tm a transaction manager
		 * @param exceptionTransformer an exception wrapper
		 * @return a new transaction batcher
		 */
		static <E extends RuntimeException> Factory of(String containerName, String cacheName, TransactionManager tm, Function<Throwable, E> exceptionTransformer) {
			System.Logger logger = System.getLogger(String.join(".", TransactionBatch.class.getPackageName(), containerName, cacheName));
			Synchronization synchronization = new Synchronization() {
				@Override
				public void beforeCompletion() {
				}

				@Override
				public void afterCompletion(int status) {
					// Disassociate batch with thread on tx completion
					ThreadLocalTransactionBatch.setCurrentBatch(null);
				}
			};
			return new Factory() {
				@Override
				public TransactionBatch get() {
					TransactionBatch batch = ThreadLocalTransactionBatch.getCurrentBatch();
					try {
						if ((batch != null) && batch.isActive()) {
							return batch.interpose();
						}
						Transaction suspendedTx = tm.suspend();
						// Ensure there is no current transaction
						if ((suspendedTx != null) && (suspendedTx.getStatus() != Status.STATUS_NO_TRANSACTION)) {
							throw new IllegalStateException(suspendedTx.toString());
						}
						tm.begin();
						Transaction tx = tm.getTransaction();
						tx.registerSynchronization(synchronization);
						batch = new ThreadLocalTransactionBatch<>(tm, tx, logger, exceptionTransformer);
						ThreadLocalTransactionBatch.setCurrentBatch(batch);
						return batch;
					} catch (RollbackException | SystemException | NotSupportedException e) {
						throw exceptionTransformer.apply(e);
					}
				}
			};
		}

		@Override
		TransactionBatch get();
	}
}

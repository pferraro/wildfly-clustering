/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.tx;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.wildfly.clustering.server.util.BlockingExecutor;

/**
 * A {@link TransactionManager} decorator that blocks an executor from shutting down while transactions are in progress.
 * @author Paul Ferraro
 */
public class BlockingTransactionManager implements TransactionManager {

	private final TransactionManager tm;
	private final BlockingExecutor executor;

	public BlockingTransactionManager(TransactionManager tm, BlockingExecutor executor) {
		this.tm = tm;
		this.executor = executor;
	}

	@Override
	public void begin() throws NotSupportedException, SystemException {
		try {
			this.executor.block(this.tm::begin).ifPresent(unblock -> {
				try {
					// Register a synchronization that unblocks
					this.tm.getTransaction().registerSynchronization(new Synchronization() {
						@Override
						public void beforeCompletion() {
						}

						@Override
						public void afterCompletion(int status) {
							unblock.run();
						}
					});
				} catch (SystemException | RollbackException e) {
					throw new IllegalStateException(e);
				}
			});
		} catch (NotSupportedException e) {
			throw e;
		} catch (SystemException e) {
			throw e;
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
		try {
			this.executor.execute(this.tm::commit);
		} catch (RollbackException e) {
			throw e;
		} catch (HeuristicMixedException e) {
			throw e;
		} catch (HeuristicRollbackException e) {
			throw e;
		} catch (SystemException e) {
			throw e;
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public int getStatus() throws SystemException {
		return this.executor.execute(this.tm::getStatus).orElse(Status.STATUS_NO_TRANSACTION);
	}

	@Override
	public Transaction getTransaction() throws SystemException {
		return this.executor.execute(this.tm::getTransaction).orElse(null);
	}

	@Override
	public void resume(Transaction tx) throws InvalidTransactionException, SystemException {
		try {
			this.executor.execute(() -> this.tm.resume(tx));
		} catch (InvalidTransactionException e) {
			throw e;
		} catch (SystemException e) {
			throw e;
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void rollback() throws SystemException {
		this.executor.execute(this.tm::rollback);
	}

	@Override
	public void setRollbackOnly() throws SystemException {
		this.executor.execute(this.tm::setRollbackOnly);
	}

	@Override
	public void setTransactionTimeout(int seconds) throws SystemException {
		this.executor.execute(() -> this.tm.setTransactionTimeout(seconds));
	}

	@Override
	public Transaction suspend() throws SystemException {
		return this.executor.execute(this.tm::suspend).orElse(null);
	}
}

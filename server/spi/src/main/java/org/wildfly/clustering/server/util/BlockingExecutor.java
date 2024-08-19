/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.util;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.StampedLock;

import org.wildfly.common.function.ExceptionBiFunction;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * An executor supporting safe invocation of tasks that require resources not otherwise available after shutdown.
 * @author Paul Ferraro
 */
public interface BlockingExecutor extends ExceptionExecutor {

	/**
	 * Blocks and performs the specified task, if the executor is available, returning an unblocking task.
	 * @param <E> the exception type
	 * @param task a runnable task
	 * @return an optional unblocking task, present if the task was executed.
	 * @throws E if execution fails
	 */
	<E extends Exception> Optional<Runnable> block(ExceptionRunnable<E> task) throws E;

	/**
	 * Executes the specified blocking task, but only if the executor is available.
	 * If executed, the specified task must return a non-null value, to be distinguishable from a non-execution.
	 * @param task a task to execute
	 * @return an optional value that is present only if the specified task was run.
	 * @throws E if the task execution failed
	 */
	<R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> task) throws E;

	/**
	 * Executes the specified blocking task, but only if the executor is available.
	 * If executed, the specified task must return a non-null value, to be distinguishable from a non-execution.
	 * @param task a task to execute
	 * @param onClose an on-close function
	 * @return an optional value that is present only if the specified task was run.
	 * @throws E if the task execution failed
	 */
	<R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> task, ExceptionBiFunction<R, Runnable, R, E> onClose) throws E;

	/**
	 * Executes the specified blocking task, but only if the executor is available.
	 * If executed, the specified task must return a non-null value, to be distinguishable from a non-execution.
	 * @param task a task to execute
	 * @param onClose an on-close function
	 * @return an optional entry containing the task result and close task that *must* be executed.
	 * @throws E if the task execution failed
	 */
	<R, E extends Exception> Optional<Map.Entry<R, Runnable>> block(ExceptionSupplier<R, E> task) throws E;

	/**
	 * Executes the specified asynchronous task, but only if the executor is available.
	 * If executed, the specified task must return a non-null stage, to be distinguishable from a non-execution.
	 * @param asyncTask a task to execute
	 * @return an optional completion stage that is present only if the specified task was initiated.
	 * @throws E if the task execution failed
	 */
	<R, E extends Exception> Optional<CompletionStage<R>> executeAsync(ExceptionSupplier<CompletionStage<R>, E> asyncTask) throws E;

	abstract class AbstractBlockingExecutor implements BlockingExecutor {
		private static final Runnable EMPTY = new Runnable() {
			@Override
			public void run() {
			}
		};
		private final StampedLock lock = new StampedLock();

		StampedLock getLock() {
			return this.lock;
		}

		private Runnable createUnlockTask(long stamp) {
			StampedLock lock = this.lock;
			return new Runnable() {
				@Override
				public void run() {
					lock.unlock(stamp);
				}
			};
		}

		@Override
		public <E extends Exception> void execute(ExceptionRunnable<E> executeTask) throws E {
			long stamp = this.lock.tryReadLock();
			if (stamp != 0L) {
				try {
					executeTask.run();
				} finally {
					this.lock.unlock(stamp);
				}
			}
		}

		@Override
		public <E extends Exception> Optional<Runnable> block(ExceptionRunnable<E> task) throws E {
			long stamp = this.lock.tryReadLock();
			if (stamp != 0L) {
				Runnable unlock = this.createUnlockTask(stamp);
				Runnable finallyTask = unlock;
				try {
					task.run();
					finallyTask = EMPTY;
					return Optional.of(unlock);
				} finally {
					finallyTask.run();
				}
			}
			return Optional.empty();
		}

		@Override
		public <R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> executeTask) throws E {
			long stamp = this.lock.tryReadLock();
			if (stamp != 0L) {
				try {
					return Optional.of(executeTask.get());
				} finally {
					this.lock.unlock(stamp);
				}
			}
			return Optional.empty();
		}

		@Override
		public <R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> executeTask, ExceptionBiFunction<R, Runnable, R, E> onClose) throws E {
			long stamp = this.lock.tryReadLock();
			if (stamp != 0L) {
				Runnable unlock = this.createUnlockTask(stamp);
				Runnable finallyTask = unlock;
				try {
					R result = executeTask.get();
					finallyTask = EMPTY;
					return Optional.of(onClose.apply(result, unlock));
				} finally {
					finallyTask.run();
				}
			}
			return Optional.empty();
		}

		@Override
		public <R, E extends Exception> Optional<Map.Entry<R, Runnable>> block(ExceptionSupplier<R, E> executeTask) throws E {
			long stamp = this.lock.tryReadLock();
			if (stamp != 0L) {
				Runnable unlock = this.createUnlockTask(stamp);
				Runnable finallyTask = unlock;
				try {
					R result = executeTask.get();
					finallyTask = EMPTY;
					return Optional.of(Map.entry(result, unlock));
				} finally {
					finallyTask.run();
				}
			}
			return Optional.empty();
		}

		@Override
		public <R, E extends Exception> Optional<CompletionStage<R>> executeAsync(ExceptionSupplier<CompletionStage<R>, E> asyncTask) throws E {
			long stamp = this.lock.tryReadLock();
			return (stamp != 0L) ? Optional.of(asyncTask.get().whenComplete((result, e) -> this.lock.unlock(stamp))) : Optional.empty();
		}
	}
}

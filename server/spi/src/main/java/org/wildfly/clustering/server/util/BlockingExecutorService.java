/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.wildfly.clustering.server.manager.Service;

/**
 * @author Paul Ferraro
 */
public interface BlockingExecutorService extends BlockingExecutor, Service {

	/**
	 * Creates new blocking executor service that runs the specified tasks upon {@link #start()} and {@link #stop()}.
	 * @param startTask a task to run when this executor is started.
	 * @param stopTask a task to run when this executor is stopped.
	 * @return a new blocking executor service
	 */
	static BlockingExecutorService newInstance(Runnable startTask, Runnable stopTask) {
		return new AbstractBlockingExecutorService() {
			private final AtomicBoolean started = new AtomicBoolean(false);
			private final AtomicLong stamp = new AtomicLong(this.getLock().writeLock());

			@Override
			public void start() {
				if (this.started.compareAndSet(false, true)) {
					long stamp = this.stamp.getAndSet(0L);
					if (stamp != 0L) {
						this.getLock().unlock(stamp);
						startTask.run();
					}
				}
			}

			@Override
			public void stop() {
				if (this.started.compareAndSet(true, false)) {
					try {
						this.stamp.set(this.getLock().writeLockInterruptibly());
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						stopTask.run();
					}
				}
			}
		};
	}

	abstract class AbstractBlockingExecutorService extends AbstractBlockingExecutor implements BlockingExecutorService {
	}
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paul Ferraro
 */
public interface CloseableBlockingExecutor extends BlockingExecutor, AutoCloseable {

	@Override
	void close();

	/**
	 * Creates new blocking executor that runs the specified task upon {@link #close()}.
	 * The specified task will only execute once, upon the first {@link #close()} invocation.
	 * @param closeTask a task to run when this executor is closed.
	 * @return a new blocking executor
	 */
	static CloseableBlockingExecutor newInstance(Runnable closeTask) {
		return new AbstractCloseableBlockingExecutor() {
			private final AtomicBoolean closed = new AtomicBoolean(false);

			@Override
			public void close() {
				// Allow only one thread to close
				if (this.closed.compareAndSet(false, true)) {
					// Closing is final - we don't need the stamp
					try {
						this.getLock().writeLockInterruptibly();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						closeTask.run();
					}
				}
			}
		};
	}

	abstract class AbstractCloseableBlockingExecutor extends AbstractBlockingExecutor implements CloseableBlockingExecutor {
	}
}

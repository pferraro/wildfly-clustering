/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.batch;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;

import org.wildfly.clustering.function.Consumer;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractContextualBatch implements ContextualBatch {
	private final AtomicInteger count = new AtomicInteger(0);
	private final String name;
	private final StackTraceElement[] stackTrace;
	private final Consumer<Status> closeTask;

	@SuppressWarnings("removal")
	AbstractContextualBatch(String name, Consumer<Status> closeTask) {
		this.name = name;
		PrivilegedAction<StackTraceElement[]> action = Thread.currentThread()::getStackTrace;
		this.stackTrace = LOGGER.isLoggable(System.Logger.Level.DEBUG) ? ((System.getSecurityManager() != null) ? AccessController.doPrivileged(action) : action.run()) : null;
		this.closeTask = closeTask;
	}

	@Override
	public ContextualBatch get() {
		int count = this.count.incrementAndGet();
		LOGGER.log(System.Logger.Level.DEBUG, "Created child context {0}[{1}]", this, count);
		return this;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void attach(Throwable exception) {
		if (this.stackTrace != null) {
			exception.setStackTrace(this.stackTrace);
		}
	}

	@Override
	public void close() {
		int count = this.count.getAndDecrement();
		if (count == 0) {
			this.closeTask.accept(this.getStatus());
		} else {
			LOGGER.log(System.Logger.Level.DEBUG, "Closed child context {0}[{1}]", this, count);
		}
	}
}

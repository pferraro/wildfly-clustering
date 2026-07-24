/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.arquillian.container;

import java.util.concurrent.atomic.AtomicBoolean;

import org.testcontainers.lifecycle.Startable;
import org.wildfly.clustering.arquillian.Lifecycle;

/**
 * Lifecycle facade for an OCI container.
 * @author Paul Ferraro
 */
class StartableLifecycle implements Lifecycle {
	private final Startable container;
	private final AtomicBoolean started = new AtomicBoolean(false);

	StartableLifecycle(Startable container) {
		System.out.println("Created OCI container lifecycle");
		this.container = container;
	}

	@Override
	public void start() {
		if (this.started.compareAndSet(false, true)) {
			this.container.start();
		}
	}

	@Override
	public void stop() {
		if (this.started.compareAndSet(true, false)) {
			this.container.stop();
		}
	}

	@Override
	public boolean isStarted() {
		return this.started.get();
	}
}

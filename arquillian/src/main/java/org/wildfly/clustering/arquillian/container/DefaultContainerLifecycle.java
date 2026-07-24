/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.arquillian.container;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.wildfly.clustering.arquillian.Lifecycle;

/**
 * Lifecycle facade for an arquillian container.
 * @author Paul Ferraro
 */
class DefaultContainerLifecycle implements Lifecycle {
	private final Container<?> container;

	DefaultContainerLifecycle(Container<?> container) {
		System.out.println("Created default container lifecycle");
		this.container = container;
	}

	@Override
	public boolean isStarted() {
		return this.container.getState() == Container.State.STARTED;
	}

	@Override
	public void start() {
		try {
			this.container.start();
		} catch (LifecycleException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void stop() {
		try {
			this.container.stop();
		} catch (LifecycleException e) {
			throw new IllegalStateException(e);
		}
	}
}

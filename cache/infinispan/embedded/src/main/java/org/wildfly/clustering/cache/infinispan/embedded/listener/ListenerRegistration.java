/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.listener;

/**
 * An Infinispan listener registration that unregisters on {@link #close()}.
 * @author Paul Ferraro
 */
public interface ListenerRegistration extends AutoCloseable {
	@Override
	void close();
}

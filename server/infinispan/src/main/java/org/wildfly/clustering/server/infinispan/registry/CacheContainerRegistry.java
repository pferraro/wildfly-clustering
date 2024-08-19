/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.registry;

import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.manager.Service;
import org.wildfly.clustering.server.registry.Registry;

/**
 * @author Paul Ferraro
 * @param <K> the registry key type
 * @param <V> the registry value type
 */
public interface CacheContainerRegistry<K, V> extends Registry<CacheContainerGroupMember, K, V>, Service {

	@Override
	CacheContainerGroup getGroup();
}

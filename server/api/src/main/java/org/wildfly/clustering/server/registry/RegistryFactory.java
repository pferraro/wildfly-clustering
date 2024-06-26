/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.registry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.wildfly.clustering.server.GroupMember;

/**
 * Factory for creating a clustered registry.
 * @param <M> the member type
 * @param <K> the type of the registry entry key
 * @param <V> the type of the registry entry value
 * @author Paul Ferraro
 */
public interface RegistryFactory<M extends GroupMember, K, V> {

	/**
	 * Creates a registry using the specified entry.
	 *
	 * @param entry the local registry entry
	 * @return a registry
	 */
	Registry<M, K, V> createRegistry(Map.Entry<K, V> entry);

	static <M extends GroupMember, K, V> RegistryFactory<M, K, V> singleton(BiFunction<Map.Entry<K, V>, Runnable, Registry<M, K, V>> factory) {
		AtomicReference<Map.Entry<K, V>> reference = new AtomicReference<>();
		return new RegistryFactory<>() {
			@Override
			public Registry<M, K, V> createRegistry(Map.Entry<K, V> entry) {
				// Ensure only one registry is created at a time
				if (!reference.compareAndSet(null, entry)) {
					throw new IllegalStateException();
				}
				return factory.apply(entry, () -> reference.set(null));
			}
		};
	}
}

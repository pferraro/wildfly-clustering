/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.registry;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryListener;

/**
 * @author Paul Ferraro
 */
public class CacheRegistryITCase {
	private static final String CLUSTER_NAME = "cluster";
	private static final String MEMBER_1 = "member1";
	private static final String MEMBER_2 = "member2";

	@Test
	public void test() throws Exception {
		Map.Entry<String, UUID> entry1 = Map.entry("foo", UUID.randomUUID());
		Map.Entry<String, UUID> entry2 = Map.entry("bar", UUID.randomUUID());
		try (CacheContainerRegistryFactoryContext<String, UUID> factory1 = new CacheContainerRegistryFactoryContext<>(CLUSTER_NAME, MEMBER_1)) {
			try (Registry<CacheContainerGroupMember, String, UUID> registry1 = factory1.get().createRegistry(entry1)) {
				CacheContainerGroupMember member1 = registry1.getGroup().getLocalMember();
				assertThat(registry1.getEntry(member1)).isEqualTo(entry1);
				assertThat(registry1.getEntries()).containsExactlyEntriesOf(Map.ofEntries(entry1));

				RegistryListener<String, UUID> listener = mock(RegistryListener.class);
				try (Registration registration = registry1.register(listener)) {

					verifyNoInteractions(listener);

					try (CacheContainerRegistryFactoryContext<String, UUID> factory2 = new CacheContainerRegistryFactoryContext<>(CLUSTER_NAME, MEMBER_2)) {
						try (Registry<CacheContainerGroupMember, String, UUID> registry2 = factory2.get().createRegistry(entry2)) {
							CacheContainerGroupMember member2 = registry2.getGroup().getLocalMember();

							assertThat(registry1.getEntry(member1)).isEqualTo(entry1);
							assertThat(registry2.getEntry(member1)).isEqualTo(entry1);
							assertThat(registry1.getEntry(member2)).isEqualTo(entry2);
							assertThat(registry2.getEntry(member2)).isEqualTo(entry2);

							assertThat(registry1.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(entry1, entry2));
							assertThat(registry2.getEntries()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(entry1, entry2));

							Thread.sleep(100);

							verify(listener).added(Map.ofEntries(entry2));
							verifyNoMoreInteractions(listener);
						}
					}

					assertThat(registry1.getEntry(member1)).isEqualTo(entry1);
					assertThat(registry1.getEntries()).containsExactlyEntriesOf(Map.ofEntries(entry1));

					Thread.sleep(100);

					verify(listener).removed(Map.ofEntries(entry2));
					verifyNoMoreInteractions(listener);
				}
			}
		}
	}
}

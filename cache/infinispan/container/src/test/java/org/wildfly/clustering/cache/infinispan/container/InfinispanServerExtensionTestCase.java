/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.cache.infinispan.container;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Paul Ferraro
 */
public class InfinispanServerExtensionTestCase {

	@RegisterExtension
	static final InfinispanServerExtension INFINISPAN = new InfinispanServerExtension();

	@Test
	public void test() {
		Assertions.assertThat(INFINISPAN.getContainer().isCreated()).isTrue();
		Assertions.assertThat(INFINISPAN.getContainer().isHostAccessible()).isTrue();
		Assertions.assertThat(INFINISPAN.getContainer().isRunning()).isTrue();
	}
}

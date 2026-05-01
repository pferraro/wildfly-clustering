/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote.transaction;

import java.util.UUID;

import org.infinispan.client.hotrod.transaction.manager.RemoteXid;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link RemoteXidMarshaller}.
 * @author Paul Ferraro
 */
public class RemoteXidMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource(MarshallingTesterFactory.class)
	public void test(TesterFactory factory) {
		Tester<RemoteXid> tester = factory.createTester();

		tester.accept(RemoteXid.create(UUID.randomUUID()));
	}
}

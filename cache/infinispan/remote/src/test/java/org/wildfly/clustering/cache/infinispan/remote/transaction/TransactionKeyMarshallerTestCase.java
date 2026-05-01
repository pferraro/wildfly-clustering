/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote.transaction;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link TransactionKey} marshalling.
 * @author Paul Ferraro
 */
public class TransactionKeyMarshallerTestCase {

	@ParameterizedTest
	@TesterFactorySource(MarshallingTesterFactory.class)
	public void test(MarshallingTesterFactory factory) throws IOException {
		Tester<TransactionKey<UUID>> tester = factory.createTester();

		UUID key = UUID.randomUUID();
		TransactionKey<UUID> txKey = new TransactionKey<>(key);

		// Verify that key is distinguishable from its wrapped key
		assertThat(txKey).isNotEqualTo(key).doesNotHaveSameHashCodeAs(key);
		// Verify that marshalled key is distinguishable from its wrapped key
		assertThat(factory.getMarshaller().write(txKey).array()).isNotEqualTo(factory.getMarshaller().write(key).array());

		tester.accept(txKey);
	}
}

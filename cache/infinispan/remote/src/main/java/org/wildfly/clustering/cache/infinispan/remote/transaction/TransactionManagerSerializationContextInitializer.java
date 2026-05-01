/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote.transaction;

import org.infinispan.client.hotrod.transaction.manager.RemoteXid;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * The serialization context initializer for this package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class TransactionManagerSerializationContextInitializer extends AbstractSerializationContextInitializer {

	/**
	 * Creates a serialization context initializer.
	 */
	public TransactionManagerSerializationContextInitializer() {
		super(RemoteXid.class.getPackage());
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(RemoteXidMarshaller.INSTANCE);
	}
}

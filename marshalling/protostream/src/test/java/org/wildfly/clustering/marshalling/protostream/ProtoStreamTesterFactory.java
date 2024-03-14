/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.List;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices({ MarshallingTesterFactory.class, ProtoStreamTesterFactory.class })
public class ProtoStreamTesterFactory implements MarshallingTesterFactory {
	private final ByteBufferMarshaller marshaller;

	public ProtoStreamTesterFactory() {
		this(List.of());
	}

	public ProtoStreamTesterFactory(Iterable<SerializationContextInitializer> initializers) {
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		ImmutableSerializationContext context = SerializationContextBuilder.newInstance(ClassLoaderMarshaller.of(loader)).load(loader).register(initializers).build();
		this.marshaller = new ProtoStreamByteBufferMarshaller(context);
	}

	@Override
	public ByteBufferMarshaller getMarshaller() {
		return this.marshaller;
	}

	@Override
	public String toString() {
		return this.marshaller.toString();
	}
}

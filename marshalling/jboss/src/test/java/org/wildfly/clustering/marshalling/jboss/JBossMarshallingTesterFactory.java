/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import org.jboss.marshalling.SimpleClassResolver;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;

/**
 * @author Paul Ferraro
 */
@MetaInfServices({ MarshallingTesterFactory.class, JBossMarshallingTesterFactory.class })
public class JBossMarshallingTesterFactory implements MarshallingTesterFactory {

	private final ByteBufferMarshaller marshaller;

	public JBossMarshallingTesterFactory() {
		ClassLoader loader = ClassLoader.getSystemClassLoader();
		this.marshaller = new JBossByteBufferMarshaller(MarshallingConfigurationBuilder.newInstance(new SimpleClassResolver(loader)).load(loader).build(), loader);
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

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

/**
 * Initializer that registers protobuf schema for java.lang.* classes.
 * @author Paul Ferraro
 */
public class LangSerializationContextInitializer extends AbstractSerializationContextInitializer {

	private final ClassLoaderMarshaller loaderMarshaller;

	public LangSerializationContextInitializer(ClassLoaderMarshaller loaderMarshaller) {
		super(Class.class.getPackage());
		this.loaderMarshaller = loaderMarshaller;
	}

	@Override
	public void registerMarshallers(SerializationContext context) {
		context.registerMarshaller(new ClassMarshaller(this.loaderMarshaller));
		context.registerMarshaller(StackTraceElementMarshaller.INSTANCE);
		context.registerMarshaller(new ExceptionMarshaller<>(Throwable.class));
	}
}

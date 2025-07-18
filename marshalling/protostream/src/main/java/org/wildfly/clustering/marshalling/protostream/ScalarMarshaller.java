/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.function.Predicate;
import org.wildfly.clustering.function.Supplier;

/**
 * Marshaller for a single scalar value.
 * This marshaller does not write any tags, nor does it read beyond a single value.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface ScalarMarshaller<T> extends Marshallable<T> {

	/**
	 * Returns the wire type of the scalar value written by this marshaller.
	 * @return the wire type of the scalar value written by this marshaller.
	 */
	WireType getWireType();

	/**
	 * Returns a marshaller for a wrapper of this scalar value, using the specified wrapping and unwrapping functions.
	 * @param <V> the wrapper type
	 * @param targetClass the wrapper marshaller type
	 * @param unwrapper a function exposing the scalar type written by this marshaller from the wrapper instance
	 * @param wrapper a function creating a wrapped instance from the scalar value read by this marshaller
	 * @return a new marshaller
	 */
	default <V> ProtoStreamMarshaller<V> toMarshaller(Class<V> targetClass, Function<V, T> unwrapper, Function<T, V> wrapper) {
		return this.toMarshaller(targetClass, Predicate.never(), unwrapper, Supplier.empty(), wrapper);
	}

	/**
	 * Returns a marshaller for a wrapper of this scalar value, using the specified wrapping and unwrapping functions.
	 * @param <V> the wrapper type
	 * @param targetClass the wrapper marshaller type
	 * @param unwrapper a function exposing the scalar type written by this marshaller from the wrapper instance
	 * @param defaultFactory provides the default value returned by the marshaller created by this function in the event that this marshaller did not write a scalar value.
	 * @param wrapper a function creating a wrapped instance from the scalar value read by this marshaller
	 * @return a new marshaller
	 */
	default <V> ProtoStreamMarshaller<V> toMarshaller(Class<V> targetClass, Function<V, T> unwrapper, java.util.function.Supplier<V> defaultFactory, Function<T, V> wrapper) {
		return this.toMarshaller(targetClass, Objects::equals, unwrapper, defaultFactory, wrapper);
	}

	/**
	 * Returns a marshaller for a wrapper of this scalar value, using the specified wrapping and unwrapping functions.
	 * @param <V> the wrapper type
	 * @param targetClass the wrapper marshaller type
	 * @param equals a predicate indicating whether the wrapped scalar value of the object to be written by this marshaller matches the wrapped scalar value of the value returned by the specified default factory
	 * @param unwrapper a function exposing the scalar type written by this marshaller from the wrapper instance
	 * @param defaultFactory provides the default value returned by the marshaller created by this function in the event that this marshaller did not write a scalar value.
	 * @param wrapper a function creating a wrapped instance from the scalar value read by this marshaller
	 * @return a new marshaller
	 */
	default <V> ProtoStreamMarshaller<V> toMarshaller(Class<V> targetClass, BiPredicate<T, T> equals, Function<V, T> unwrapper, java.util.function.Supplier<V> defaultFactory, Function<T, V> wrapper) {
		return this.toMarshaller(targetClass, new Predicate<>() {
			@Override
			public boolean test(V value) {
				return equals.test(unwrapper.apply(value), unwrapper.apply(defaultFactory.get()));
			}
		}, unwrapper, defaultFactory, wrapper);
	}

	/**
	 * Returns a marshaller for an {@link Optional} wrapper of this scalar value.
	 * @return an optional marshaller
	 */
	default ProtoStreamMarshaller<Optional<T>> toMarshaller() {
		return this.toMarshaller(Function.identity(), Function.identity());
	}

	/**
	 * Returns a marshaller for an {@link Optional} wrapper of this scalar value.
	 * @param unwrapper a function exposing the scalar type written by this marshaller from the wrapper instance
	 * @param wrapper a function creating a wrapped instance from the scalar value read by this marshaller
	 * @return an optional marshaller
	 */
	@SuppressWarnings("unchecked")
	default <V> ProtoStreamMarshaller<Optional<V>> toMarshaller(Function<V, T> unwrapper, Function<T, V> wrapper) {
		return this.toMarshaller((Class<Optional<V>>) (Class<?>) Optional.class, Optional::isEmpty, unwrapper.compose(Optional::get), Supplier.of(Optional.empty()), wrapper.andThen(Optional::of));
	}

	/**
	 * Returns a marshaller for a wrapper of this scalar value, using the specified wrapping and unwrapping functions.
	 * @param <V> the wrapper type
	 * @param targetClass the wrapper marshaller type
	 * @param skipWrite a predicate indicating whether the marshaller returned by this function can skip writing its scalar value, and instead return the value returned by the default factory on read.
	 * @param unwrapper a function exposing the scalar type written by this marshaller from the wrapper instance
	 * @param defaultFactory provides the default value returned by the marshaller created by this function in the event that this marshaller did not write a scalar value.
	 * @param wrapper a function creating a wrapped instance from the scalar value read by this marshaller
	 * @return a new marshaller
	 */
	default <V> ProtoStreamMarshaller<V> toMarshaller(Class<V> targetClass, java.util.function.Predicate<V> skipWrite, Function<V, T> unwrapper, java.util.function.Supplier<V> defaultFactory, Function<T, V> wrapper) {
		ScalarMarshaller<T> marshaller = this;
		return new ProtoStreamMarshaller<>() {
			@Override
			public Class<? extends V> getJavaClass() {
				return targetClass;
			}

			@Override
			public V readFrom(ProtoStreamReader reader) throws IOException {
				V value = defaultFactory.get();
				while (!reader.isAtEnd()) {
					int tag = reader.readTag();
					switch (WireType.getTagFieldNumber(tag)) {
						case 1 -> {
							value = wrapper.apply(marshaller.readFrom(reader));
						}
						default -> reader.skipField(tag);
					}
				}
				return value;
			}

			@Override
			public void writeTo(ProtoStreamWriter writer, V value) throws IOException {
				if (!skipWrite.test(value)) {
					writer.writeTag(1, marshaller.getWireType());
					marshaller.writeTo(writer, unwrapper.apply(value));
				}
			}
		};
	}
}

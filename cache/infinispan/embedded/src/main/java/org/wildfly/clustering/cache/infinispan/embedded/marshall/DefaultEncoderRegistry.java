/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.marshall;

import static org.infinispan.util.logging.Log.CONTAINER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.Transcoder;
import org.infinispan.commons.dataconversion.Wrapper;
import org.infinispan.commons.dataconversion.WrapperIds;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.wildfly.clustering.cache.function.Predicate;

/**
 * Custom {@link EncoderRegistry} that supports transcoder removal.
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
public class DefaultEncoderRegistry implements EncoderRegistry {
	@SuppressWarnings("removal")
	private final Map<Short, org.infinispan.commons.dataconversion.Encoder> encoders = new ConcurrentHashMap<>();
	private final Map<Byte, Wrapper> wrappers = new ConcurrentHashMap<>();
	private final List<Transcoder> transcoders = Collections.synchronizedList(new ArrayList<>());
	private final Map<MediaType, Map<MediaType, Transcoder>> transcoderCache = new ConcurrentHashMap<>();

	@Override
	public void registerTranscoder(Transcoder transcoder) {
		this.transcoders.add(transcoder);
	}

	@Override
	public Transcoder getTranscoder(MediaType fromType, MediaType toType) {
		Transcoder transcoder = this.findTranscoder(fromType, toType);
		if (transcoder == null) {
			throw CONTAINER.cannotFindTranscoder(fromType, toType);
		}
		return transcoder;
	}

	@Override
	public <T extends Transcoder> T getTranscoder(Class<T> targetClass) {
		return targetClass.cast(this.transcoders.stream().filter(Predicate.<Class<?>>same(targetClass).map(Object::getClass)).findAny().orElse(null));
	}

	@Override
	public boolean isConversionSupported(MediaType fromType, MediaType toType) {
		return fromType.match(toType) || this.findTranscoder(fromType, toType) != null;
	}

	@Override
	public Object convert(Object object, MediaType fromType, MediaType toType) {
		if (object == null) return null;
		return this.getTranscoder(fromType, toType).transcode(object, fromType, toType);
	}

	@Override
	public void unregisterTranscoder(MediaType type) {
		synchronized (this.transcoders) {
			Iterator<Transcoder> transcoders = this.transcoders.iterator();
			while (transcoders.hasNext()) {
				if (transcoders.next().getSupportedMediaTypes().contains(type)) {
					transcoders.remove();
				}
			}
		}
		this.transcoderCache.remove(type);
		for (Map<MediaType, Transcoder> map : this.transcoderCache.values()) {
			map.remove(type);
		}
	}

	private Transcoder findTranscoder(MediaType fromType, MediaType toType) {
		return this.transcoderCache.computeIfAbsent(fromType, mt -> new ConcurrentHashMap<>(4)).computeIfAbsent(toType, mt -> this.transcoders.stream().filter(t -> t.supportsConversion(fromType, toType)).findFirst().orElse(null));
	}

	@Deprecated
	@Override
	public org.infinispan.commons.dataconversion.Encoder getEncoder(Class<? extends org.infinispan.commons.dataconversion.Encoder> encoderClass, short encoderId) {
		if (encoderId != org.infinispan.commons.dataconversion.EncoderIds.NO_ENCODER) {
			org.infinispan.commons.dataconversion.Encoder encoder = this.encoders.get(encoderId);
			if (encoder == null) {
				throw CONTAINER.encoderIdNotFound(encoderId);
			}
			return encoder;
		}
		org.infinispan.commons.dataconversion.Encoder encoder = this.encoders.values().stream().filter(Predicate.<Class<?>>same(encoderClass).map(Object::getClass)).findFirst().orElse(null);
		if (encoder == null) {
			throw CONTAINER.encoderClassNotFound(encoderClass);
		}
		return encoder;
	}

	@Deprecated
	@Override
	public boolean isRegistered(Class<? extends org.infinispan.commons.dataconversion.Encoder> encoderClass) {
		return this.encoders.values().stream().anyMatch(Predicate.<Class<?>>same(encoderClass).map(Object::getClass));
	}

	@Deprecated
	@Override
	public Wrapper getWrapper(Class<? extends Wrapper> wrapperClass, byte wrapperId) {
		if (wrapperClass == null && wrapperId == WrapperIds.NO_WRAPPER) {
			return null;
		}
		if (wrapperId != WrapperIds.NO_WRAPPER) {
			Wrapper wrapper = this.wrappers.get(wrapperId);
			if (wrapper == null) {
				throw CONTAINER.wrapperIdNotFound(wrapperId);
			}
			return wrapper;
		}
		Wrapper wrapper = this.wrappers.values().stream().filter(Predicate.<Class<?>>same(wrapperClass).map(Object::getClass)).findAny().orElse(null);
		if (wrapper == null) {
			throw CONTAINER.wrapperClassNotFound(wrapperClass);
		}
		return wrapper;
	}

	@Deprecated
	@Override
	public void registerEncoder(org.infinispan.commons.dataconversion.Encoder encoder) {
		short id = encoder.id();
		if (this.encoders.putIfAbsent(id, encoder) != null) {
			throw CONTAINER.duplicateIdEncoder(id);
		}
	}

	@Deprecated
	@Override
	public void registerWrapper(Wrapper wrapper) {
		byte id = wrapper.id();
		if (this.wrappers.putIfAbsent(id, wrapper) != null) {
			throw CONTAINER.duplicateIdWrapper(id);
		}
	}
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.util;

import java.util.AbstractCollection;
import java.util.Collections;
import java.util.Spliterators;

import org.infinispan.CacheCollection;
import org.infinispan.CacheSet;
import org.infinispan.CacheStream;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableSpliterator;
import org.infinispan.commons.util.Closeables;

/**
 * Cache collection utilities.
 * @author Paul Ferraro
 */
public class CacheCollections {

	private CacheCollections() {
		// Hide
	}

	private static final CacheCollection<?> EMPTY_COLLECTION = new EmptyCacheCollection<>();
	private static final CacheSet<?> EMPTY_SET = new EmptyCacheSet<>();

	@SuppressWarnings("unchecked")
	public static <E> CacheCollection<E> emptyCollection() {
		return (CacheCollection<E>) EMPTY_COLLECTION;
	}

	@SuppressWarnings("unchecked")
	public static <E> CacheSet<E> emptySet() {
		return (CacheSet<E>) EMPTY_SET;
	}

	private static class EmptyCacheCollection<E> extends AbstractCollection<E> implements CacheCollection<E> {

		@Override
		public CloseableIterator<E> iterator() {
			return Closeables.iterator(Collections.emptyIterator());
		}

		@Override
		public CloseableSpliterator<E> spliterator() {
			return Closeables.spliterator(Spliterators.emptySpliterator());
		}

		@Override
		public CacheStream<E> stream() {
			return CacheStreams.emptyStream();
		}

		@Override
		public CacheStream<E> parallelStream() {
			return CacheStreams.emptyStream();
		}

		@Override
		public int size() {
			return 0;
		}
	}

	private static class EmptyCacheSet<E> extends EmptyCacheCollection<E> implements CacheSet<E> {
	}
}

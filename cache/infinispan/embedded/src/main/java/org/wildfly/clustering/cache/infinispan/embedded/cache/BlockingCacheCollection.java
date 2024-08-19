/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import org.infinispan.CacheCollection;
import org.infinispan.CacheStream;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableSpliterator;
import org.infinispan.commons.util.Closeables;
import org.wildfly.clustering.cache.infinispan.embedded.util.CacheStreams;
import org.wildfly.clustering.server.util.BlockingExecutor;

/**
 * @author Paul Ferraro
 * @param <E> the collection type
 */
public class BlockingCacheCollection<E> implements CacheCollection<E> {
	private final CacheCollection<E> collection;
	private final BlockingExecutor executor;

	BlockingCacheCollection(CacheCollection<E> collection, BlockingExecutor executor) {
		this.collection = collection;
		this.executor = executor;
	}

	@Override
	public CloseableIterator<E> iterator() {
		return this.executor.block(this.collection::iterator).<CloseableIterator<E>>map(entry -> new CloseableIterator<>() {
			@SuppressWarnings("resource")
			private final CloseableIterator<E> iterator = entry.getKey();
			private final Runnable closeTask = entry.getValue();

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public E next() {
				return this.iterator.next();
			}

			@Override
			public void close() {
				try {
					this.iterator.close();
				} finally {
					this.closeTask.run();
				}
			}
		}).orElse(Closeables.iterator(Collections.emptyIterator()));
	}

	@Override
	public CloseableSpliterator<E> spliterator() {
		return this.executor.block(this.collection::spliterator).<CloseableSpliterator<E>>map(entry -> new CloseableSpliterator<>() {
			@SuppressWarnings("resource")
			private final CloseableSpliterator<E> iterator = entry.getKey();
			private final Runnable closeTask = entry.getValue();

			@Override
			public boolean tryAdvance(Consumer<? super E> action) {
				return this.iterator.tryAdvance(action);
			}

			@Override
			public Spliterator<E> trySplit() {
				return this.iterator.trySplit();
			}

			@Override
			public long estimateSize() {
				return this.iterator.estimateSize();
			}

			@Override
			public int characteristics() {
				return this.iterator.characteristics();
			}

			@Override
			public void close() {
				try {
					this.iterator.close();
				} finally {
					this.closeTask.run();
				}
			}
		}).orElse(Closeables.spliterator(Spliterators.emptySpliterator()));
	}

	@Override
	public int size() {
		return this.executor.execute(this.collection::size).orElse(0);
	}

	@Override
	public boolean isEmpty() {
		return this.executor.execute(this.collection::isEmpty).orElse(true);
	}

	@Override
	public boolean contains(Object object) {
		return this.executor.execute(() -> this.collection.contains(object)).orElse(false);
	}

	@Override
	public Object[] toArray() {
		return this.executor.execute(() -> this.collection.toArray()).orElse(new Object[0]);
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return this.executor.execute(() -> this.collection.toArray(array)).orElse(array);
	}

	@Override
	public boolean add(E e) {
		return this.executor.execute(() -> this.collection.add(e)).orElse(false);
	}

	@Override
	public boolean remove(Object element) {
		return this.executor.execute(() -> this.collection.remove(element)).orElse(false);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return this.executor.execute(() -> this.collection.containsAll(collection)).orElse(collection.isEmpty());
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		return this.executor.execute(() -> this.collection.addAll(collection)).orElse(collection.isEmpty());
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return this.executor.execute(() -> this.collection.removeAll(collection)).orElse(collection.isEmpty());
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return this.executor.execute(() -> this.collection.retainAll(collection)).orElse(collection.isEmpty());
	}

	@Override
	public void clear() {
		this.executor.execute(this.collection::clear);
	}

	@Override
	public CacheStream<E> stream() {
		return this.executor.block(this.collection::stream).map(entry -> entry.getKey().onClose(entry.getValue())).orElse(CacheStreams.emptyStream());
	}

	@Override
	public CacheStream<E> parallelStream() {
		return this.executor.block(this.collection::parallelStream).map(entry -> entry.getKey().onClose(entry.getValue())).orElse(CacheStreams.emptyStream());
	}
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.infinispan.BaseCacheStream;
import org.infinispan.Cache;
import org.infinispan.CacheStream;
import org.infinispan.DoubleCacheStream;
import org.infinispan.IntCacheStream;
import org.infinispan.LongCacheStream;
import org.infinispan.commons.util.IntSet;

/**
 * Cache stream utilities.
 * @author Paul Ferraro
 */
public class CacheStreams {

	private CacheStreams() {
		// Hide
	}

	private static final Runnable EMPTY_RUNNABLE = new Runnable() {
		@Override
		public void run() {
		}
	};

	private static final CacheStream<?> EMPTY_STREAM = new EmptyCacheStream<>(EMPTY_RUNNABLE);
	private static final DoubleCacheStream EMPTY_DOUBLE_STREAM = new EmptyDoubleCacheStream(EMPTY_RUNNABLE);
	private static final IntCacheStream EMPTY_INT_STREAM = new EmptyIntCacheStream(EMPTY_RUNNABLE);
	private static final LongCacheStream EMPTY_LONG_STREAM = new EmptyLongCacheStream(EMPTY_RUNNABLE);

	@SuppressWarnings("unchecked")
	public static <E> CacheStream<E> emptyStream() {
		return (CacheStream<E>) EMPTY_STREAM;
	}

	public static DoubleCacheStream emptyDoubleStream() {
		return EMPTY_DOUBLE_STREAM;
	}

	public static IntCacheStream emptyIntStream() {
		return EMPTY_INT_STREAM;
	}

	public static LongCacheStream emptyLongStream() {
		return EMPTY_LONG_STREAM;
	}

	private abstract static class AbstractEmptyCacheStream<E, S extends BaseStream<E, S>> implements BaseCacheStream<E, S> {
		private final Runnable closeTask;

		AbstractEmptyCacheStream(Runnable closeTask) {
			this.closeTask = closeTask;
		}

		@Override
		public void close() {
			this.closeTask.run();
		}
	}

	private static class EmptyCacheStream<E> extends AbstractEmptyCacheStream<E, Stream<E>> implements CacheStream<E> {

		EmptyCacheStream(Runnable closeTask) {
			super(closeTask);
		}

		@Override
		public CacheStream<E> onClose(Runnable closeHandler) {
			return new EmptyCacheStream<>(closeHandler);
		}

		@Override
		public boolean anyMatch(Predicate<? super E> predicate) {
			return false;
		}

		@Override
		public boolean allMatch(Predicate<? super E> predicate) {
			return true;
		}

		@Override
		public boolean noneMatch(Predicate<? super E> predicate) {
			return true;
		}

		@Override
		public E reduce(E identity, BinaryOperator<E> accumulator) {
			return identity;
		}

		@Override
		public Optional<E> reduce(BinaryOperator<E> accumulator) {
			return Optional.empty();
		}

		@Override
		public <U> U reduce(U identity, BiFunction<U, ? super E, U> accumulator, BinaryOperator<U> combiner) {
			return identity;
		}

		@Override
		public Optional<E> min(Comparator<? super E> comparator) {
			return Optional.empty();
		}

		@Override
		public Optional<E> max(Comparator<? super E> comparator) {
			return Optional.empty();
		}

		@Override
		public long count() {
			return 0;
		}

		@Override
		public Optional<E> findFirst() {
			return Optional.empty();
		}

		@Override
		public Optional<E> findAny() {
			return Optional.empty();
		}

		@Override
		public boolean isParallel() {
			return false;
		}

		@Override
		public CacheStream<E> sequentialDistribution() {
			return this;
		}

		@Override
		public CacheStream<E> parallelDistribution() {
			return this;
		}

		@Override
		public CacheStream<E> filterKeySegments(IntSet segments) {
			return this;
		}

		@Override
		public CacheStream<E> filterKeys(Set<?> keys) {
			return this;
		}

		@Override
		public CacheStream<E> distributedBatchSize(int batchSize) {
			return this;
		}

		@Override
		public CacheStream<E> segmentCompletionListener(SegmentCompletionListener listener) {
			return this;
		}

		@Override
		public CacheStream<E> disableRehashAware() {
			return this;
		}

		@Override
		public CacheStream<E> timeout(long timeout, TimeUnit unit) {
			return this;
		}

		@Override
		public void forEach(Consumer<? super E> action) {
		}

		@Override
		public <K, V> void forEach(BiConsumer<Cache<K, V>, ? super E> action) {
		}

		@Override
		public void forEachOrdered(Consumer<? super E> action) {
		}

		@Override
		public Iterator<E> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public Spliterator<E> spliterator() {
			return Spliterators.emptySpliterator();
		}

		@Override
		public CacheStream<E> sorted() {
			return this;
		}

		@Override
		public CacheStream<E> sorted(Comparator<? super E> comparator) {
			return this;
		}

		@Override
		public CacheStream<E> limit(long maxSize) {
			return this;
		}

		@Override
		public CacheStream<E> skip(long n) {
			return this;
		}

		@Override
		public CacheStream<E> peek(Consumer<? super E> action) {
			return this;
		}

		@Override
		public CacheStream<E> distinct() {
			return this;
		}

		@Override
		public <R1, A> R1 collect(Collector<? super E, A, R1> collector) {
			return collector.finisher().apply(collector.supplier().get());
		}

		@Override
		public <R1> R1 collect(Supplier<R1> supplier, BiConsumer<R1, ? super E> accumulator, BiConsumer<R1, R1> combiner) {
			return supplier.get();
		}

		@Override
		public CacheStream<E> filter(Predicate<? super E> predicate) {
			return this;
		}

		@Override
		public <R1> CacheStream<R1> map(Function<? super E, ? extends R1> mapper) {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream mapToDouble(ToDoubleFunction<? super E> mapper) {
			return null;
		}

		@Override
		public IntCacheStream mapToInt(ToIntFunction<? super E> mapper) {
			return null;
		}

		@Override
		public LongCacheStream mapToLong(ToLongFunction<? super E> mapper) {
			return null;
		}

		@Override
		public <R1> CacheStream<R1> flatMap(Function<? super E, ? extends Stream<? extends R1>> mapper) {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream flatMapToDouble(Function<? super E, ? extends DoubleStream> mapper) {
			return null;
		}

		@Override
		public IntCacheStream flatMapToInt(Function<? super E, ? extends IntStream> mapper) {
			return null;
		}

		@Override
		public LongCacheStream flatMapToLong(Function<? super E, ? extends LongStream> mapper) {
			return null;
		}

		@Override
		public CacheStream<E> parallel() {
			return this;
		}

		@Override
		public CacheStream<E> sequential() {
			return this;
		}

		@Override
		public CacheStream<E> unordered() {
			return this;
		}

		@Override
		public Object[] toArray() {
			return new Object[0];
		}

		@Override
		public <A> A[] toArray(IntFunction<A[]> generator) {
			return generator.apply(0);
		}
	};

	private static class EmptyDoubleCacheStream extends AbstractEmptyCacheStream<Double, DoubleStream> implements DoubleCacheStream {

		EmptyDoubleCacheStream(Runnable closeTask) {
			super(closeTask);
		}

		@Override
		public DoubleCacheStream onClose(Runnable closeHandler) {
			return new EmptyDoubleCacheStream(closeHandler);
		}

		@Override
		public void forEach(DoubleConsumer action) {
		}

		@Override
		public <K, V> void forEach(ObjDoubleConsumer<Cache<K, V>> action) {
		}

		@Override
		public void forEachOrdered(DoubleConsumer action) {
		}

		@Override
		public double[] toArray() {
			return new double[0];
		}

		@Override
		public double reduce(double identity, DoubleBinaryOperator op) {
			return identity;
		}

		@Override
		public OptionalDouble reduce(DoubleBinaryOperator op) {
			return OptionalDouble.empty();
		}

		@Override
		public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
			return supplier.get();
		}

		@Override
		public double sum() {
			return 0L;
		}

		@Override
		public OptionalDouble min() {
			return OptionalDouble.empty();
		}

		@Override
		public OptionalDouble max() {
			return OptionalDouble.empty();
		}

		@Override
		public long count() {
			return 0L;
		}

		@Override
		public OptionalDouble average() {
			return OptionalDouble.empty();
		}

		@Override
		public DoubleSummaryStatistics summaryStatistics() {
			return new DoubleSummaryStatistics();
		}

		@Override
		public boolean anyMatch(DoublePredicate predicate) {
			return false;
		}

		@Override
		public boolean allMatch(DoublePredicate predicate) {
			return true;
		}

		@Override
		public boolean noneMatch(DoublePredicate predicate) {
			return true;
		}

		@Override
		public OptionalDouble findFirst() {
			return OptionalDouble.empty();
		}

		@Override
		public OptionalDouble findAny() {
			return OptionalDouble.empty();
		}

		@Override
		public PrimitiveIterator.OfDouble iterator() {
			return Spliterators.iterator(Spliterators.emptyDoubleSpliterator());
		}

		@Override
		public Spliterator.OfDouble spliterator() {
			return Spliterators.emptyDoubleSpliterator();
		}

		@Override
		public boolean isParallel() {
			return false;
		}

		@Override
		public DoubleCacheStream filterKeySegments(IntSet segments) {
			return this;
		}

		@Override
		public DoubleCacheStream sequentialDistribution() {
			return this;
		}

		@Override
		public DoubleCacheStream parallelDistribution() {
			return this;
		}

		@Override
		public DoubleCacheStream filterKeys(Set<?> keys) {
			return this;
		}

		@Override
		public DoubleCacheStream distributedBatchSize(int batchSize) {
			return this;
		}

		@Override
		public DoubleCacheStream segmentCompletionListener(SegmentCompletionListener listener) {
			return this;
		}

		@Override
		public DoubleCacheStream disableRehashAware() {
			return this;
		}

		@Override
		public DoubleCacheStream timeout(long timeout, TimeUnit unit) {
			return this;
		}

		@Override
		public DoubleCacheStream filter(DoublePredicate predicate) {
			return this;
		}

		@Override
		public DoubleCacheStream map(DoubleUnaryOperator mapper) {
			return this;
		}

		@Override
		public <U> CacheStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
			return emptyStream();
		}

		@Override
		public IntCacheStream mapToInt(DoubleToIntFunction mapper) {
			return emptyIntStream();
		}

		@Override
		public LongCacheStream mapToLong(DoubleToLongFunction mapper) {
			return emptyLongStream();
		}

		@Override
		public DoubleCacheStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
			return this;
		}

		@Override
		public DoubleCacheStream distinct() {
			return this;
		}

		@Override
		public DoubleCacheStream sorted() {
			return this;
		}

		@Override
		public DoubleCacheStream peek(DoubleConsumer action) {
			return this;
		}

		@Override
		public DoubleCacheStream limit(long maxSize) {
			return this;
		}

		@Override
		public DoubleCacheStream skip(long n) {
			return this;
		}

		@Override
		public CacheStream<Double> boxed() {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream sequential() {
			return this;
		}

		@Override
		public DoubleCacheStream parallel() {
			return this;
		}

		@Override
		public DoubleCacheStream unordered() {
			return this;
		}
	}

	private static class EmptyIntCacheStream extends AbstractEmptyCacheStream<Integer, IntStream> implements IntCacheStream {

		EmptyIntCacheStream(Runnable closeTask) {
			super(closeTask);
		}

		@Override
		public IntCacheStream onClose(Runnable closeHandler) {
			return new EmptyIntCacheStream(closeHandler);
		}

		@Override
		public void forEach(IntConsumer action) {
		}

		@Override
		public <K, V> void forEach(ObjIntConsumer<Cache<K, V>> action) {
		}

		@Override
		public void forEachOrdered(IntConsumer action) {
		}

		@Override
		public int[] toArray() {
			return new int[0];
		}

		@Override
		public int reduce(int identity, IntBinaryOperator op) {
			return identity;
		}

		@Override
		public OptionalInt reduce(IntBinaryOperator op) {
			return OptionalInt.empty();
		}

		@Override
		public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
			return supplier.get();
		}

		@Override
		public int sum() {
			return 0;
		}

		@Override
		public OptionalInt min() {
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt max() {
			return OptionalInt.empty();
		}

		@Override
		public long count() {
			return 0L;
		}

		@Override
		public OptionalDouble average() {
			return OptionalDouble.empty();
		}

		@Override
		public IntSummaryStatistics summaryStatistics() {
			return new IntSummaryStatistics();
		}

		@Override
		public boolean anyMatch(IntPredicate predicate) {
			return false;
		}

		@Override
		public boolean allMatch(IntPredicate predicate) {
			return true;
		}

		@Override
		public boolean noneMatch(IntPredicate predicate) {
			return true;
		}

		@Override
		public OptionalInt findFirst() {
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt findAny() {
			return OptionalInt.empty();
		}

		@Override
		public PrimitiveIterator.OfInt iterator() {
			return Spliterators.iterator(Spliterators.emptyIntSpliterator());
		}

		@Override
		public Spliterator.OfInt spliterator() {
			return Spliterators.emptyIntSpliterator();
		}

		@Override
		public boolean isParallel() {
			return false;
		}

		@Override
		public IntCacheStream filterKeySegments(IntSet segments) {
			return this;
		}

		@Override
		public IntCacheStream sequentialDistribution() {
			return this;
		}

		@Override
		public IntCacheStream parallelDistribution() {
			return this;
		}

		@Override
		public IntCacheStream filterKeys(Set<?> keys) {
			return this;
		}

		@Override
		public IntCacheStream distributedBatchSize(int batchSize) {
			return this;
		}

		@Override
		public IntCacheStream segmentCompletionListener(SegmentCompletionListener listener) {
			return this;
		}

		@Override
		public IntCacheStream disableRehashAware() {
			return this;
		}

		@Override
		public IntCacheStream timeout(long timeout, TimeUnit unit) {
			return this;
		}

		@Override
		public IntCacheStream filter(IntPredicate predicate) {
			return this;
		}

		@Override
		public IntCacheStream map(IntUnaryOperator mapper) {
			return this;
		}

		@Override
		public <U> CacheStream<U> mapToObj(IntFunction<? extends U> mapper) {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream mapToDouble(IntToDoubleFunction mapper) {
			return emptyDoubleStream();
		}

		@Override
		public LongCacheStream mapToLong(IntToLongFunction mapper) {
			return emptyLongStream();
		}

		@Override
		public IntCacheStream flatMap(IntFunction<? extends IntStream> mapper) {
			return emptyIntStream();
		}

		@Override
		public IntCacheStream distinct() {
			return this;
		}

		@Override
		public IntCacheStream sorted() {
			return this;
		}

		@Override
		public IntCacheStream peek(IntConsumer action) {
			return this;
		}

		@Override
		public IntCacheStream limit(long maxSize) {
			return this;
		}

		@Override
		public IntCacheStream skip(long n) {
			return this;
		}

		@Override
		public CacheStream<Integer> boxed() {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream asDoubleStream() {
			return emptyDoubleStream();
		}

		@Override
		public LongCacheStream asLongStream() {
			return emptyLongStream();
		}

		@Override
		public IntCacheStream sequential() {
			return this;
		}

		@Override
		public IntCacheStream parallel() {
			return this;
		}

		@Override
		public IntCacheStream unordered() {
			return this;
		}
	}

	private static class EmptyLongCacheStream extends AbstractEmptyCacheStream<Long, LongStream> implements LongCacheStream {

		EmptyLongCacheStream(Runnable closeTask) {
			super(closeTask);
		}

		@Override
		public LongCacheStream onClose(Runnable closeHandler) {
			return new EmptyLongCacheStream(closeHandler);
		}

		@Override
		public void forEach(LongConsumer action) {
		}

		@Override
		public <K, V> void forEach(ObjLongConsumer<Cache<K, V>> action) {
		}

		@Override
		public void forEachOrdered(LongConsumer action) {
		}

		@Override
		public long[] toArray() {
			return new long[0];
		}

		@Override
		public long reduce(long identity, LongBinaryOperator op) {
			return identity;
		}

		@Override
		public OptionalLong reduce(LongBinaryOperator op) {
			return OptionalLong.empty();
		}

		@Override
		public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
			return supplier.get();
		}

		@Override
		public long sum() {
			return 0L;
		}

		@Override
		public OptionalLong min() {
			return OptionalLong.empty();
		}

		@Override
		public OptionalLong max() {
			return OptionalLong.empty();
		}

		@Override
		public long count() {
			return 0L;
		}

		@Override
		public OptionalDouble average() {
			return OptionalDouble.empty();
		}

		@Override
		public LongSummaryStatistics summaryStatistics() {
			return new LongSummaryStatistics();
		}

		@Override
		public boolean anyMatch(LongPredicate predicate) {
			return false;
		}

		@Override
		public boolean allMatch(LongPredicate predicate) {
			return true;
		}

		@Override
		public boolean noneMatch(LongPredicate predicate) {
			return true;
		}

		@Override
		public OptionalLong findFirst() {
			return OptionalLong.empty();
		}

		@Override
		public OptionalLong findAny() {
			return OptionalLong.empty();
		}

		@Override
		public PrimitiveIterator.OfLong iterator() {
			return Spliterators.iterator(Spliterators.emptyLongSpliterator());
		}

		@Override
		public Spliterator.OfLong spliterator() {
			return Spliterators.emptyLongSpliterator();
		}

		@Override
		public boolean isParallel() {
			return false;
		}

		@Override
		public LongCacheStream filterKeySegments(IntSet segments) {
			return this;
		}

		@Override
		public LongCacheStream sequentialDistribution() {
			return this;
		}

		@Override
		public LongCacheStream parallelDistribution() {
			return this;
		}

		@Override
		public LongCacheStream filterKeys(Set<?> keys) {
			return this;
		}

		@Override
		public LongCacheStream distributedBatchSize(int batchSize) {
			return this;
		}

		@Override
		public LongCacheStream segmentCompletionListener(SegmentCompletionListener listener) {
			return this;
		}

		@Override
		public LongCacheStream disableRehashAware() {
			return this;
		}

		@Override
		public LongCacheStream timeout(long timeout, TimeUnit unit) {
			return this;
		}

		@Override
		public LongCacheStream filter(LongPredicate predicate) {
			return this;
		}

		@Override
		public LongCacheStream map(LongUnaryOperator mapper) {
			return this;
		}

		@Override
		public <U> CacheStream<U> mapToObj(LongFunction<? extends U> mapper) {
			return emptyStream();
		}

		@Override
		public IntCacheStream mapToInt(LongToIntFunction mapper) {
			return emptyIntStream();
		}

		@Override
		public DoubleCacheStream mapToDouble(LongToDoubleFunction mapper) {
			return emptyDoubleStream();
		}

		@Override
		public LongCacheStream flatMap(LongFunction<? extends LongStream> mapper) {
			return this;
		}

		@Override
		public LongCacheStream distinct() {
			return this;
		}

		@Override
		public LongCacheStream sorted() {
			return this;
		}

		@Override
		public LongCacheStream peek(LongConsumer action) {
			return this;
		}

		@Override
		public LongCacheStream limit(long maxSize) {
			return this;
		}

		@Override
		public LongCacheStream skip(long n) {
			return this;
		}

		@Override
		public CacheStream<Long> boxed() {
			return emptyStream();
		}

		@Override
		public DoubleCacheStream asDoubleStream() {
			return emptyDoubleStream();
		}

		@Override
		public LongCacheStream sequential() {
			return this;
		}

		@Override
		public LongCacheStream parallel() {
			return this;
		}

		@Override
		public LongCacheStream unordered() {
			return this;
		}
	}
}

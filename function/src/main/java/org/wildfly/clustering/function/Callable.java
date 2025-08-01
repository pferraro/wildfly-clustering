/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.function;

/**
 * An enhanced callable.
 * @author Paul Ferraro
 * @param <T> the result type
 */
public interface Callable<T> extends java.util.concurrent.Callable<T> {
	Callable<?> NULL = new Callable<>() {
		@Override
		public Object call() {
			return null;
		}
	};

	/**
	 * Returns a callable whose result is mapped by the specified function.
	 * @param <R> the mapped result type
	 * @param mapper a mapping function
	 * @return a callable whose result is mapped by the specified function.
	 */
	default <R> Callable<R> map(Function<T, R> mapper) {
		return new Callable<>() {
			@Override
			public R call() throws Exception {
				return mapper.apply(Callable.this.call());
			}
		};
	}

	/**
	 * Returns a callable that returns null.
	 * @return a callable that returns null.
	 */
	@SuppressWarnings("unchecked")
	static <T> Callable<T> empty() {
		return (Callable<T>) NULL;
	}

	/**
	 * Returns a callable that runs the specified runner and returns <code>null</code>.
	 * @param runner a runner
	 * @return a callable that runs the specified runner and returns <code>null</code>.
	 */
	static <T> Callable<T> run(Runnable runner) {
		return (runner != null) && (runner != Runnable.EMPTY) ? new Callable<>() {
			@Override
			public T call() {
				runner.run();
				return null;
			}
		} : empty();
	}

	/**
	 * Returns a callable that delegates to the specified supplier.
	 * @param supplier a supplier
	 * @return the result of the specified supplier.
	 */
	static <T> Callable<T> get(Supplier<T> supplier) {
		return (supplier != null) && (supplier != Supplier.NULL) ? new Callable<>() {
			@Override
			public T call() {
				return supplier.get();
			}
		} : empty();
	}

	/**
	 * Returns a callable that returns the specified value.
	 * @param value the result value
	 * @return a callable that returns the specified value.
	 */
	static <T> Callable<T> of(T value) {
		return (value != null) ? new Callable<>() {
			@Override
			public T call() {
				return value;
			}
		} : empty();
	}

	/**
	 * Returns a callable that throws the provided exception.
	 * @param exceptionProvider a provider of an exception
	 * @return a callable that throws the provided exception.
	 */
	static <T> Callable<T> exceptional(Supplier<? extends Exception> exceptionProvider) {
		return new Callable<>() {
			@Override
			public T call() throws Exception {
				throw exceptionProvider.get();
			}
		};
	}
}

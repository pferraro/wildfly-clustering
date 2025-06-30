/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.function.Consumer;

import org.wildfly.clustering.function.Supplier;

/**
 * Encapsulates some context that is applicable until {@link #close()}.
 * @author Paul Ferraro
 * @param <T> the context value type
 */
public interface Context<T> extends Supplier<T>, AutoCloseable {
	Context<?> EMPTY = new Context<>() {
		@Override
		public Object get() {
			return null;
		}

		@Override
		public void close() {
		}
	};

	@Override
	void close();

	/**
	 * Returns an empty context.
	 * @param <T> the context value type
	 * @return an empty context.
	 */
	@SuppressWarnings("unchecked")
	static <T> Context<T> empty() {
		return (Context<T>) EMPTY;
	}

	/**
	 * Returns a context that provides the specified value and invokes the specified action on close.
	 * @param <T> the context value type
	 * @param value the context value
	 * @param closeAction the action to perform on {@link #close()}.
	 * @return a context that provides the specified value and invokes the specified action on close.
	 */
	static <T> Context<T> of(T value, Consumer<T> closeAction) {
		return new Context<>() {
			@Override
			public T get() {
				return value;
			}

			@Override
			public void close() {
				closeAction.accept(value);
			}
		};
	}
}

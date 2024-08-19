/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.util;

import java.util.concurrent.Executor;

import org.wildfly.common.function.ExceptionRunnable;

/**
 * An exception throwing {@link Executor} variant.
 * @author Paul Ferraro
 */
public interface ExceptionExecutor {

	/**
	 * Executes the specified task.
	 * @param <E> the exception type
	 * @param task a runnable task
	 * @throws E if execution fails
	 */
	<E extends Exception> void execute(ExceptionRunnable<E> task) throws E;
}

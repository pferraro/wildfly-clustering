/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.manager;

import java.util.concurrent.CompletionStage;

/**
 * A restartable service.
 * @author Paul Ferraro
 */
public interface Service {

	/**
	 * Starts this service.
	 */
	void start();

	/**
	 * Stops this service.
	 */
	void stop();

	interface Async extends Service {
		/**
		 * Starts this service asynchronously.
		 * @return a stage that completes when this service starts.
		 */
		CompletionStage<Void> startAsync();

		/**
		 * Stops this service asynchronously.
		 * @return a stage that completes when this service stops.
		 */
		CompletionStage<Void> stopAsync();

		@Override
		default void start() {
			this.startAsync().toCompletableFuture().join();
		}

		@Override
		default void stop() {
			this.stopAsync().toCompletableFuture().join();
		}
	}
}

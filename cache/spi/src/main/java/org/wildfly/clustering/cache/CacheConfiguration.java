/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache;

import org.wildfly.clustering.cache.batch.Batch;
import org.wildfly.clustering.function.Supplier;

/**
 * Encapsulates the generic configuration of a cache.
 * @author Paul Ferraro
 */
public interface CacheConfiguration {

	CacheProperties getCacheProperties();

	Supplier<Batch> getBatchFactory();
}

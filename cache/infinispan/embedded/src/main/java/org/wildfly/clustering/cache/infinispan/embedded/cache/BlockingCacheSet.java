/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.embedded.cache;

import org.infinispan.CacheSet;
import org.wildfly.clustering.server.util.BlockingExecutor;

/**
 * @author Paul Ferraro
 * @param <E> the set type
 */
public class BlockingCacheSet<E> extends BlockingCacheCollection<E> implements CacheSet<E> {

	BlockingCacheSet(CacheSet<E> set, BlockingExecutor executor) {
		super(set, executor);
	}
}

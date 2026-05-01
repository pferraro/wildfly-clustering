/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote.transaction;

import org.wildfly.clustering.cache.infinispan.CacheKey;

/**
 * A cache key for a transaction entry.
 * @author Paul Ferraro
 * @param <K> the decorated cache key
 */
public class TransactionKey<K> extends CacheKey<K> {
	/**
	 * Constructs an transaction key.
	 * @param key the decorated key
	 */
	public TransactionKey(K key) {
		super(key);
	}
}

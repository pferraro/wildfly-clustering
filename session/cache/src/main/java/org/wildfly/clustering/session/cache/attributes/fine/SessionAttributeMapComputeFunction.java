/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.session.cache.attributes.fine;

import java.util.Map;

import org.wildfly.clustering.cache.function.MapComputeFunction;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapComputeFunction<V> extends MapComputeFunction<String, V> {

	public SessionAttributeMapComputeFunction(Map<String, V> operand) {
		super(operand);
	}
}

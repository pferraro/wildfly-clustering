/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.container;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.testcontainers.lifecycle.Startable;

/**
 * Factory for an OCI container.
 * @author Paul Ferraro
 */
public interface ContainerFactory {
	/**
	 * Returns the name of this factory.
	 * @return the name of this factory.
	 */
	String getName();

	/**
	 * Creates an OCI container from the specified properties.
	 * @param properties the container properties
	 * @param references a resolver of container property references.
	 * @return an OCI container
	 */
	Startable createContainer(Map<String, String> properties, UnaryOperator<String> references);
}

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.container;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.kohsuke.MetaInfServices;
import org.testcontainers.lifecycle.Startable;

/**
 * A default container factory.
 * @author Paul Ferraro
 */
@MetaInfServices(ContainerFactory.class)
public class DefaultContainerFactory implements ContainerFactory {

	/**
	 * Constructs a new container factory.
	 */
	public DefaultContainerFactory() {
		// For use by service loader
	}

	@Override
	public String getName() {
		return "OCI";
	}

	@Override
	public Startable createContainer(Map<String, String> properties, UnaryOperator<String> references) {
		return new DefaultContainer(properties, references);
	}

	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return (object instanceof ContainerFactory factory) && this.getName().equals(factory.getName());
	}

	@Override
	public String toString() {
		return this.getName();
	}
}

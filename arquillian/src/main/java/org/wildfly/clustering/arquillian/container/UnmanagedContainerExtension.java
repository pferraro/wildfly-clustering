/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.arquillian.container;

import java.util.ServiceLoader;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.container.ContainerFactory;

/**
 * Arquillian extension for simplifying usage of unmanaged containers.
 * @author Paul Ferraro
 */
@MetaInfServices(LoadableExtension.class)
public class UnmanagedContainerExtension implements LoadableExtension {

	/**
	 * Constructs a new unmanaged container extension.
	 */
	public UnmanagedContainerExtension() {
		// Do nothing
	}

	@Override
	public void register(ExtensionBuilder builder) {
		builder.service(ResourceProvider.class, DeploymentContainerRegistryResourceProvider.class);
		ServiceLoader.load(ContainerFactory.class, ContainerFactory.class.getClassLoader()).stream().forEach(provider -> builder.service(ContainerFactory.class, provider.type()));
	}
}

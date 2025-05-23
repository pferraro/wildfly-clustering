/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session.infinispan.embedded;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.ImmutableSessionMetaData;
import org.wildfly.clustering.session.cache.SessionFactory;

/**
 * Session remover that removes a session if and only if it is expired.
 * @param <SC> the ServletContext specification type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public class ExpiredSessionRemover<SC, MV, AV, LC> implements Predicate<String>, Registrar<Consumer<ImmutableSession>> {
	private static final System.Logger LOGGER = System.getLogger(ExpiredSessionRemover.class.getName());

	private final SessionFactory<SC, MV, AV, LC> factory;
	private final Collection<Consumer<ImmutableSession>> listeners = new CopyOnWriteArraySet<>();

	public ExpiredSessionRemover(SessionFactory<SC, MV, AV, LC> factory) {
		this.factory = factory;
	}

	@Override
	public boolean test(String id) {
		MV metaDataValue = this.factory.getMetaDataFactory().tryValue(id);
		if (metaDataValue != null) {
			ImmutableSessionMetaData metaData = this.factory.getMetaDataFactory().createImmutableSessionMetaData(id, metaDataValue);
			if (metaData.isExpired()) {
				AV attributesValue = this.factory.getAttributesFactory().findValue(id);
				if (attributesValue != null) {
					Map<String, Object> attributes = this.factory.getAttributesFactory().createImmutableSessionAttributes(id, attributesValue);
					ImmutableSession session = this.factory.createImmutableSession(id, metaData, attributes);
					LOGGER.log(System.Logger.Level.TRACE, "Session {0} has expired.", id);
					for (Consumer<ImmutableSession> listener : this.listeners) {
						listener.accept(session);
					}
				}
				try {
					this.factory.remove(id);
					return true;
				} catch (CancellationException e) {
					return false;
				}
			}
			LOGGER.log(System.Logger.Level.TRACE, "Session {0} is not yet expired.", id);
		} else {
			LOGGER.log(System.Logger.Level.TRACE, "Session {0} was not found or is currently in use.", id);
		}
		return false;
	}

	@Override
	public Registration register(Consumer<ImmutableSession> listener) {
		this.listeners.add(listener);
		return () -> this.listeners.remove(listener);
	}
}

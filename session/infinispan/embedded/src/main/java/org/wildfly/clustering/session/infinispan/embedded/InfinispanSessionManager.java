/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session.infinispan.embedded;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.context.Flag;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.cache.batch.Batcher;
import org.wildfly.clustering.cache.infinispan.batch.TransactionBatch;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.expiration.ExpirationMetaData;
import org.wildfly.clustering.server.manager.IdentifierFactory;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.Session;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionStatistics;
import org.wildfly.clustering.session.cache.AbstractSessionManager;
import org.wildfly.clustering.session.cache.SessionFactory;
import org.wildfly.clustering.session.infinispan.embedded.metadata.SessionMetaDataKeyFilter;

/**
 * Generic session manager implementation - independent of cache mapping strategy.
 * @param <DC> the deployment context type
 * @param <MV> the meta-data value type
 * @param <AV> the attributes value type
 * @param <SC> the session context type
 * @author Paul Ferraro
 */
public class InfinispanSessionManager<DC, MV, AV, SC> extends AbstractSessionManager<DC, MV, AV, SC, TransactionBatch> {

	private final Batcher<TransactionBatch> batcher;
	private final Cache<Key<String>, ?> cache;
	private final CacheProperties properties;
	private final IdentifierFactory<String> identifierFactory;
	private final Scheduler<String, ExpirationMetaData> expirationScheduler;
	private final Runnable startTask;
	private final Registrar<SessionManager<SC, TransactionBatch>> registrar;

	private volatile Registration registration;

	public InfinispanSessionManager(InfinispanSessionManagerConfiguration<DC, SC> configuration, SessionFactory<DC, MV, AV, SC> factory) {
		super(configuration, configuration, factory, new Consumer<>() {
			@Override
			public void accept(ImmutableSession session) {
				if (session.isValid()) {
					configuration.getExpirationScheduler().schedule(session.getId(), session.getMetaData());
				}
			}
		});
		this.cache = configuration.getCache();
		this.properties = configuration.getCacheProperties();
		this.identifierFactory = configuration.getIdentifierFactory();
		this.batcher = configuration.getBatcher();
		this.expirationScheduler = configuration.getExpirationScheduler();
		this.registrar = configuration.getRegistrar();
		this.startTask = configuration.getStartTask();
	}

	@Override
	public void start() {
		this.registration = this.registrar.register(this);
		this.identifierFactory.start();
		this.startTask.run();
	}

	@Override
	public void stop() {
		if (!this.properties.isPersistent()) {
			PersistenceConfiguration persistence = this.cache.getCacheConfiguration().persistence();
			// Don't passivate sessions on stop if we will purge the store on startup
			if (persistence.passivation() && !persistence.stores().stream().allMatch(StoreConfiguration::purgeOnStartup)) {
				try (Stream<Key<String>> stream = this.cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD, Flag.SKIP_LOCKING).keySet().stream()) {
					stream.filter(SessionMetaDataKeyFilter.INSTANCE).forEach(this.cache::evict);
				}
			}
		}
		this.identifierFactory.stop();
		this.registration.close();
	}

	@Override
	public Batcher<TransactionBatch> getBatcher() {
		return this.batcher;
	}

	@Override
	public Supplier<String> getIdentifierFactory() {
		return this.identifierFactory;
	}

	@Override
	public CompletionStage<Session<SC>> findSessionAsync(String id) {
		this.expirationScheduler.cancel(id);
		return super.findSessionAsync(id);
	}

	@Override
	public SessionStatistics getStatistics() {
		return this;
	}

	@Override
	public Set<String> getActiveSessions() {
		// Omit remote sessions (i.e. when using DIST mode) as well as passivated sessions
		return this.getSessions(Flag.CACHE_MODE_LOCAL, Flag.SKIP_CACHE_LOAD);
	}

	@Override
	public Set<String> getSessions() {
		// Omit remote sessions (i.e. when using DIST mode)
		return this.getSessions(Flag.CACHE_MODE_LOCAL);
	}

	private Set<String> getSessions(Flag... flags) {
		Locality locality = Locality.forCurrentConsistentHash(this.cache);
		try (Stream<Key<String>> keys = this.cache.getAdvancedCache().withFlags(flags).keySet().stream()) {
			return keys.filter(SessionMetaDataKeyFilter.INSTANCE.and(key -> locality.isLocal(key))).map(key -> key.getId()).collect(Collectors.toSet());
		}
	}
}

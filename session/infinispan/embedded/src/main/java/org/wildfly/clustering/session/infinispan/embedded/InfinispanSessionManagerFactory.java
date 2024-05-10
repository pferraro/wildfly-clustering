/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session.infinispan.embedded;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.infinispan.Cache;
import org.wildfly.clustering.cache.CacheProperties;
import org.wildfly.clustering.cache.infinispan.CacheKey;
import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.cache.infinispan.embedded.distribution.Locality;
import org.wildfly.clustering.cache.infinispan.embedded.listener.ListenerRegistration;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.context.ContextStrategy;
import org.wildfly.clustering.server.expiration.ExpirationMetaData;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.affinity.UnaryGroupMemberAffinity;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.clustering.server.infinispan.expiration.ScheduleWithExpirationMetaDataCommandFactory;
import org.wildfly.clustering.server.infinispan.manager.AffinityIdentifierFactory;
import org.wildfly.clustering.server.infinispan.scheduler.CacheEntryScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerScheduler;
import org.wildfly.clustering.server.infinispan.scheduler.PrimaryOwnerSchedulerConfiguration;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleCommand;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleLocalKeysTask;
import org.wildfly.clustering.server.infinispan.scheduler.ScheduleWithTransientMetaDataCommand;
import org.wildfly.clustering.server.infinispan.scheduler.SchedulerTopologyChangeListener;
import org.wildfly.clustering.server.infinispan.util.CacheInvoker;
import org.wildfly.clustering.server.manager.IdentifierFactory;
import org.wildfly.clustering.server.scheduler.Scheduler;
import org.wildfly.clustering.server.util.Invoker;
import org.wildfly.clustering.session.ImmutableSession;
import org.wildfly.clustering.session.SessionManager;
import org.wildfly.clustering.session.SessionManagerConfiguration;
import org.wildfly.clustering.session.SessionManagerFactory;
import org.wildfly.clustering.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.session.cache.CompositeSessionFactory;
import org.wildfly.clustering.session.cache.ContextualSessionManager;
import org.wildfly.clustering.session.cache.SessionFactory;
import org.wildfly.clustering.session.cache.attributes.IdentityMarshallerSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.session.cache.attributes.MarshalledValueMarshallerSessionAttributesFactoryConfiguration;
import org.wildfly.clustering.session.cache.attributes.SessionAttributesFactory;
import org.wildfly.clustering.session.cache.attributes.coarse.ImmutableSessionActivationNotifier;
import org.wildfly.clustering.session.cache.attributes.coarse.SessionActivationNotifier;
import org.wildfly.clustering.session.cache.attributes.fine.ImmutableSessionAttributeActivationNotifier;
import org.wildfly.clustering.session.cache.attributes.fine.SessionAttributeActivationNotifier;
import org.wildfly.clustering.session.cache.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.session.cache.metadata.coarse.ContextualSessionMetaDataEntry;
import org.wildfly.clustering.session.infinispan.embedded.attributes.CoarseSessionAttributesFactory;
import org.wildfly.clustering.session.infinispan.embedded.attributes.FineSessionAttributesFactory;
import org.wildfly.clustering.session.infinispan.embedded.metadata.InfinispanSessionMetaDataFactory;
import org.wildfly.clustering.session.infinispan.embedded.metadata.SessionMetaDataKeyFilter;
import org.wildfly.clustering.session.spec.SessionEventListenerSpecificationProvider;
import org.wildfly.clustering.session.spec.SessionSpecificationProvider;

/**
 * Factory for creating session managers.
 * @param <C> the session manager context type
 * @param <SC> the session context type
 * @author Paul Ferraro
 */
public class InfinispanSessionManagerFactory<C, SC> implements SessionManagerFactory<C, SC>, Runnable {
	private final Scheduler<String, ExpirationMetaData> scheduler;
	private final SessionFactory<C, ContextualSessionMetaDataEntry<SC>, ?, SC> factory;
	private final BiConsumer<Locality, Locality> scheduleTask;
	private final ListenerRegistration schedulerListenerRegistration;
	private final EmbeddedCacheConfiguration configuration;
	private final Function<SessionManagerConfiguration<C>, Registrar<SessionManager<SC>>> managerRegistrarFactory;

	public <S, L> InfinispanSessionManagerFactory(SessionManagerFactoryConfiguration<SC> configuration, SessionSpecificationProvider<S, C> sessionProvider, SessionEventListenerSpecificationProvider<S, L> listenerProvider, InfinispanSessionManagerFactoryConfiguration infinispan) {
		this.configuration = infinispan;
		SessionAttributeActivationNotifierFactory<S, C, L, SC> notifierFactory = new SessionAttributeActivationNotifierFactory<>(sessionProvider, listenerProvider);
		CacheProperties properties = infinispan.getCacheProperties();
		SessionMetaDataFactory<ContextualSessionMetaDataEntry<SC>> metaDataFactory = new InfinispanSessionMetaDataFactory<>(infinispan);
		this.factory = new CompositeSessionFactory<>(metaDataFactory, this.createSessionAttributesFactory(configuration, sessionProvider, listenerProvider, notifierFactory, infinispan), properties, configuration.getSessionContextFactory());
		ExpiredSessionRemover<C, ?, ?, SC> remover = new ExpiredSessionRemover<>(this.factory);
		this.managerRegistrarFactory = new Function<>() {
			@Override
			public Registrar<SessionManager<SC>> apply(SessionManagerConfiguration<C> managerConfiguration) {
				return new Registrar<>() {
					@Override
					public Registration register(SessionManager<SC> manager) {
						Registration contextRegistration = notifierFactory.register(Map.entry(managerConfiguration.getContext(), manager));
						Registration expirationRegistration = remover.register(managerConfiguration.getExpirationListener());
						return () -> {
							expirationRegistration.close();
							contextRegistration.close();
						};
					}
				};
			}
		};
		Cache<? extends CacheKey<String>, ?> cache = infinispan.getCache();
		CacheEntryScheduler<String, ExpirationMetaData> localScheduler = new SessionExpirationScheduler<>(infinispan.getBatchFactory(), this.factory.getMetaDataFactory(), remover, Duration.ofMillis(cache.getCacheConfiguration().transaction().cacheStopTimeout()));
		CacheContainerCommandDispatcherFactory dispatcherFactory = infinispan.getCommandDispatcherFactory();
		CacheContainerGroup group = dispatcherFactory.getGroup();
		this.scheduler = group.isSingleton() ? localScheduler : new PrimaryOwnerScheduler<>(new PrimaryOwnerSchedulerConfiguration<String, ExpirationMetaData>() {
			@Override
			public String getName() {
				return cache.getName();
			}

			@Override
			public CacheContainerCommandDispatcherFactory getCommandDispatcherFactory() {
				return dispatcherFactory;
			}

			@Override
			public CacheEntryScheduler<String, ExpirationMetaData> getScheduler() {
				return localScheduler;
			}

			@Override
			public Function<String, CacheContainerGroupMember> getAffinity() {
				return new UnaryGroupMemberAffinity<>(cache, group);
			}

			@Override
			public BiFunction<String, ExpirationMetaData, ScheduleCommand<String, ExpirationMetaData>> getScheduleCommandFactory() {
				return properties.isTransactional() ? new ScheduleWithExpirationMetaDataCommandFactory<>() : ScheduleWithTransientMetaDataCommand::new;
			}

			@Override
			public Invoker getInvoker() {
				return CacheInvoker.retrying(cache);
			}
		});

		this.scheduleTask = new ScheduleLocalKeysTask<>(cache, SessionMetaDataKeyFilter.INSTANCE, localScheduler);
		this.schedulerListenerRegistration = new SchedulerTopologyChangeListener<>(cache, localScheduler, this.scheduleTask).register();
	}

	@Override
	public void run() {
		this.scheduleTask.accept(Locality.of(false), Locality.forCurrentConsistentHash(this.configuration.getCache()));
	}

	@Override
	public SessionManager<SC> createSessionManager(SessionManagerConfiguration<C> configuration) {
		IdentifierFactory<String> identifierFactory = new AffinityIdentifierFactory<>(configuration.getIdentifierFactory(), this.configuration.getCache());
		Registrar<SessionManager<SC>> registrar = this.managerRegistrarFactory.apply(configuration);
		InfinispanSessionManagerConfiguration<SC> infinispanConfiguration = new InfinispanSessionManagerConfiguration<>() {
			@Override
			public Scheduler<String, ExpirationMetaData> getExpirationScheduler() {
				return InfinispanSessionManagerFactory.this.scheduler;
			}

			@Override
			public Runnable getStartTask() {
				return InfinispanSessionManagerFactory.this;
			}

			@Override
			public Registrar<SessionManager<SC>> getRegistrar() {
				return registrar;
			}

			@Override
			public <K, V> Cache<K, V> getCache() {
				return InfinispanSessionManagerFactory.this.configuration.getCache();
			}

			@Override
			public IdentifierFactory<String> getIdentifierFactory() {
				return identifierFactory;
			}
		};
		return new ContextualSessionManager<>(new InfinispanSessionManager<>(configuration, infinispanConfiguration, this.factory), this.configuration.getCacheProperties().isTransactional() ? ContextStrategy.UNSHARED : ContextStrategy.SHARED);
	}

	private <S, L> SessionAttributesFactory<C, ?> createSessionAttributesFactory(SessionManagerFactoryConfiguration<SC> configuration, SessionSpecificationProvider<S, C> sessionProvider, SessionEventListenerSpecificationProvider<S, L> listenerProvider, Function<String, SessionAttributeActivationNotifier> detachedPassivationNotifierFactory, EmbeddedCacheConfiguration infinispan) {
		boolean marshalling = infinispan.getCacheProperties().isMarshalling();
		switch (configuration.getAttributePersistenceStrategy()) {
			case FINE: {
				BiFunction<ImmutableSession, C, SessionAttributeActivationNotifier> passivationNotifierFactory = (session, context) -> new ImmutableSessionAttributeActivationNotifier<>(sessionProvider, listenerProvider, session, context);
				return marshalling ? new FineSessionAttributesFactory<>(new MarshalledValueMarshallerSessionAttributesFactoryConfiguration<>(configuration), passivationNotifierFactory, detachedPassivationNotifierFactory, infinispan) : new FineSessionAttributesFactory<>(new IdentityMarshallerSessionAttributesFactoryConfiguration<>(configuration), passivationNotifierFactory, detachedPassivationNotifierFactory, infinispan);
			}
			case COARSE: {
				BiFunction<ImmutableSession, C, SessionActivationNotifier> passivationNotifierFactory = (session, context) -> new ImmutableSessionActivationNotifier<>(sessionProvider, listenerProvider, session, context);
				return marshalling ? new CoarseSessionAttributesFactory<>(new MarshalledValueMarshallerSessionAttributesFactoryConfiguration<>(configuration), passivationNotifierFactory, detachedPassivationNotifierFactory, infinispan) : new CoarseSessionAttributesFactory<>(new IdentityMarshallerSessionAttributesFactoryConfiguration<>(configuration), passivationNotifierFactory, detachedPassivationNotifierFactory, infinispan);
			}
			default: {
				// Impossible
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public void close() {
		this.schedulerListenerRegistration.close();
		this.scheduler.close();
		this.factory.close();
	}
}

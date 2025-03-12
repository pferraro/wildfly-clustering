/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.session.cache.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.wildfly.clustering.cache.CacheEntryMutator;
import org.wildfly.clustering.session.user.UserSessions;

/**
 * Unit test for {@link MutableUserSessions}.
 * @author Paul Ferraro
 */
public class MutableUserSessionsTestCase {

	@Test
	public void getApplications() {
		CacheEntryMutator mutator = mock(CacheEntryMutator.class);
		UserSessions<String, String> sessions = new MutableUserSessions<>(Map.of("deployment", "session"), mutator);

		Set<String> result = sessions.getDeployments();

		assertThat(result).containsExactly("deployment");

		verify(mutator, never()).mutate();
	}

	@Test
	public void getSession() {
		CacheEntryMutator mutator = mock(CacheEntryMutator.class);
		UserSessions<String, String> sessions = new MutableUserSessions<>(Map.of("deployment", "session"), mutator);

		assertThat(sessions.getSession("deployment")).isEqualTo("session");
		assertThat(sessions.getSession("foo")).isNull();

		verify(mutator, never()).mutate();
	}

	@Test
	public void addSession() {
		Map<String, String> deployments = new TreeMap<>();
		CacheEntryMutator mutator = mock(CacheEntryMutator.class);
		UserSessions<String, String> sessions = new MutableUserSessions<>(deployments, mutator);

		sessions.addSession("deployment", "session");

		verify(mutator).mutate();

		sessions.addSession("deployment", "session");

		verifyNoMoreInteractions(mutator);
	}

	@Test
	public void removeSession() {
		Map<String, String> deployments = new TreeMap<>(Map.of("deployment", "session"));
		CacheEntryMutator mutator = mock(CacheEntryMutator.class);
		UserSessions<String, String> sessions = new MutableUserSessions<>(deployments, mutator);

		sessions.removeSession("deployment");

		verify(mutator).mutate();

		sessions.removeSession("deployment");

		verifyNoMoreInteractions(mutator);
	}
}

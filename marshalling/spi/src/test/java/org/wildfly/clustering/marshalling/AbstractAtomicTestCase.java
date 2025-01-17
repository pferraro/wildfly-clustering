/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.assertj.core.api.AtomicBooleanAssert;
import org.junit.jupiter.api.Test;

/**
 * Generic tests for java.util.concurrent.atomic.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractAtomicTestCase {

	private final MarshallingTesterFactory factory;

	public AbstractAtomicTestCase(MarshallingTesterFactory factory) {
		this.factory = factory;
	}

	@Test
	public void testAtomicBoolean() {
		Consumer<AtomicBoolean> tester = this.factory.createTester(AtomicBooleanAssert::new, (assertion, expected) -> {
			if (expected.get()) {
				assertion.isTrue();
			} else {
				assertion.isFalse();
			}
		});
		tester.accept(new AtomicBoolean(false));
		tester.accept(new AtomicBoolean(true));
	}

	@Test
	public void testAtomicInteger() {
		Consumer<AtomicInteger> tester = this.factory.createTester((expected, actual) -> assertThat(actual).hasValue(expected.get()));
		tester.accept(new AtomicInteger());
		tester.accept(new AtomicInteger(Byte.MAX_VALUE));
		tester.accept(new AtomicInteger(Integer.MAX_VALUE));
	}

	@Test
	public void testAtomicLong() {
		Consumer<AtomicLong> tester = this.factory.createTester((expected, actual) -> assertThat(actual).hasValue(expected.get()));
		tester.accept(new AtomicLong());
		tester.accept(new AtomicLong(Short.MAX_VALUE));
		tester.accept(new AtomicLong(Long.MAX_VALUE));
	}

	@Test
	public void testAtomicReference() {
		Consumer<AtomicReference<Object>> tester = this.factory.createTester((expected, actual) -> assertThat(actual).hasValue(expected.get()));
		tester.accept(new AtomicReference<>());
		tester.accept(new AtomicReference<>(Boolean.TRUE));
	}
}

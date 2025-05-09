/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link IndexSerializer}.
 * @author Paul Ferraro
 */
public class IndexSerializerTestCase {

	@Test
	public void test() throws IOException {
		// Test marshalling of incrementing powers of 2
		for (int i = 0; i < Integer.SIZE - 2; ++i) {
			int index = 2 << i;
			test(index - 1);
			test(index);
		}
		test(Integer.MAX_VALUE);
	}

	private static void test(int index) throws IOException {
		IntStream.Builder builder = IntStream.builder();

		try {
			builder.add(size(IndexSerializer.UNSIGNED_BYTE, index));
		} catch (IndexOutOfBoundsException e) {
			assertThat(index).isGreaterThan(Byte.MAX_VALUE - Byte.MIN_VALUE);
		}

		try {
			builder.add(size(IndexSerializer.UNSIGNED_SHORT, index));
		} catch (IndexOutOfBoundsException e) {
			assertThat(index).isGreaterThan(Short.MAX_VALUE - Short.MIN_VALUE);
		}

		builder.add(size(IndexSerializer.VARIABLE, index));

		builder.add(size(IndexSerializer.INTEGER, index));

		// Ensure that our IndexExternalizer.select(...) chooses the optimal externalizer
		assertThat(size(IndexSerializer.select(index), index)).isEqualTo(builder.build().min().getAsInt());
	}

	public static int size(IntSerializer externalizer, int index) throws IOException {

		ByteArrayOutputStream externalizedOutput = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(externalizedOutput)) {
			externalizer.writeInt(output, index);
		}

		byte[] externalizedBytes = externalizedOutput.toByteArray();

		try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(externalizedBytes))) {
			assertThat(externalizer.readInt(input)).isEqualTo(index);
		}

		return externalizedBytes.length;
	}
}

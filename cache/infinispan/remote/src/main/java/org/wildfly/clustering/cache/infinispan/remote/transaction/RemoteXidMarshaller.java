/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.cache.infinispan.remote.transaction;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Optional;

import org.infinispan.client.hotrod.transaction.manager.RemoteXid;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link RemoteXid}.
 * @author Paul Ferraro
 */
public enum RemoteXidMarshaller implements ProtoStreamMarshaller<RemoteXid> {
	/** Singleton instance */
	INSTANCE;

	private static final int FORMAT_INDEX = 1;
	private static final int GLOBAL_INDEX = 2;
	private static final int BRANCH_INDEX = 3;

	private static final int DEFAULT_FORMAT_ID = 0x48525458;

	@SuppressWarnings("removal")
	private static final MethodHandle CONSTRUCTOR = (System.getSecurityManager() != null) ? AccessController.doPrivileged(new PrivilegedAction<>() {
		@Override
		public MethodHandle run() {
			return findConstructor();
		}
	}) : findConstructor();

	private static MethodHandle findConstructor() {
		try {
			return MethodHandles.privateLookupIn(RemoteXid.class, MethodHandles.lookup()).findConstructor(RemoteXid.class, MethodType.methodType(void.class, int.class, byte[].class, byte[].class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Class<? extends RemoteXid> getJavaClass() {
		return RemoteXid.class;
	}

	@Override
	public RemoteXid readFrom(ProtoStreamReader reader) throws IOException {
		int formatId = DEFAULT_FORMAT_ID;
		byte[] globalId = new byte[0];
		byte[] branchId = null;
		while (!reader.isAtEnd()) {
			int tag = reader.readTag();
			switch (WireType.getTagFieldNumber(tag)) {
				case FORMAT_INDEX -> formatId = reader.readSFixed32();
				case GLOBAL_INDEX -> globalId = reader.readByteArray();
				case BRANCH_INDEX -> branchId = reader.readByteArray();
				default -> reader.skipField(tag);
			}
		}
		try {
			return (RemoteXid) CONSTRUCTOR.invokeExact(formatId, globalId, Optional.ofNullable(branchId).orElse(globalId));
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, RemoteXid id) throws IOException {
		int formatId = id.getFormatId();
		if (formatId != DEFAULT_FORMAT_ID) {
			writer.writeSFixed32(FORMAT_INDEX, formatId);
		}
		byte[] globalId = id.getGlobalTransactionId();
		if (globalId.length > 0) {
			writer.writeBytes(GLOBAL_INDEX, globalId);
		}
		byte[] branchId = id.getBranchQualifier();
		if (!Arrays.equals(globalId, branchId)) {
			writer.writeBytes(BRANCH_INDEX, branchId);
		}
	}
}

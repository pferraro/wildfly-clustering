/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for an {@link InetAddress}.
 * @author Paul Ferraro
 */
public enum InetAddressMarshaller implements FieldSetMarshaller.Simple<InetAddress> {
	INSTANCE;

	private static final InetAddress DEFAULT = InetAddress.getLoopbackAddress();

	private static final int HOST_NAME_INDEX = 0;
	private static final int ADDRESS_INDEX = 1;
	private static final int FIELDS = 2;

	@Override
	public InetAddress createInitialValue() {
		return DEFAULT;
	}

	@Override
	public int getFields() {
		return FIELDS;
	}

	@Override
	public InetAddress readFrom(ProtoStreamReader reader, int index, WireType type, InetAddress address) throws IOException {
		switch (index) {
			case HOST_NAME_INDEX:
				String hostName = reader.readString();
				return InetAddress.getByName(hostName);
			case ADDRESS_INDEX:
				return InetAddress.getByAddress(reader.readByteArray());
			default:
				reader.skipField(type);
				return address;
		}
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, InetAddress address) throws IOException {
		// Determine host name without triggering reverse lookup
		String hostName = resolvedHostName(address);
		// Marshal as host name, if possible
		if (hostName != null) {
			if (!hostName.equals(DEFAULT.getHostName())) {
				writer.writeString(HOST_NAME_INDEX, hostName);
			}
		} else {
			byte[] bytes = address.getAddress();
			if (!Arrays.equals(bytes, DEFAULT.getAddress())) {
				writer.writeBytes(ADDRESS_INDEX, address.getAddress());
			}
		}
	}

	private static String resolvedHostName(InetAddress address) {
		InetSocketAddress socketAddress = new InetSocketAddress(address, 0);
		return (socketAddress.toString().lastIndexOf('/') > 0) ? socketAddress.getHostString() : null;
	}
}

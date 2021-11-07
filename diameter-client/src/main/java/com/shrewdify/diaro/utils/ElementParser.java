package com.shrewdify.diaro.utils;
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * erick.svenson@yahoo.coma
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class ElementParser {


	private static final int INT_INET4 = 1;
	private static final int INT32_SIZE = 4;
	private static final int INT_INET6 = 2;
	
	public byte[] addressToBytes(InetAddress address) {
        byte byteAddrOrig[] = address.getAddress();

        byte[] data = new byte[byteAddrOrig.length + 2];

        int addrType = address instanceof Inet4Address ? INT_INET4 : INT_INET6;
        data[0] = (byte) ((addrType >> 8) & 0xFF);
        data[INT_INET4] = (byte) ((addrType >> 0) & 0xFF);
        System.arraycopy(byteAddrOrig, 0, data, 2, byteAddrOrig.length);
        return data;
    }


	public int bytesToInt(byte[] rawData) throws Exception {
		return prepareBuffer(rawData, INT32_SIZE).getInt();
	}

	protected ByteBuffer prepareBuffer(byte[] bytes, int len) throws Exception {
		if (bytes.length != len)
			throw new Exception("Incorrect data length");
		return ByteBuffer.wrap(bytes);
	}

	public String bytesToOctetString(byte[] rawData) {
		try {
			
			return new String(rawData, "iso-8859-1");

		} catch (UnsupportedEncodingException e) {
			System.err.println("Invalid data type"+e.getMessage());
			return null;
		}
	}

	
	public InetAddress bytesToAddress(byte[] rawData) throws Exception {
		InetAddress inetAddress;
		byte[] address;
		try {
			if (rawData[INT_INET4] == INT_INET4) {
				address = new byte[4];
				System.arraycopy(rawData, 2, address, 0, address.length);
				inetAddress = Inet4Address.getByAddress(address);
			} else {
				address = new byte[16];
				System.arraycopy(rawData, 2, address, 0, address.length);
				inetAddress = Inet6Address.getByAddress(address);
			}
		} catch (Exception e) {
			throw new Exception(e);
		}
		return inetAddress;
	}

	public byte[] int32ToBytes(int value) {
		byte[] bytes = new byte[INT32_SIZE];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.putInt(value);
		return bytes;
	}

	public byte[] int64ToBytes(int value) {
		byte[] bytes = new byte[INT32_SIZE*2];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.putInt(value);
		return bytes;
	}

	/**
	 * 
	 * @param buffer
	 * @param shift
	 *            - shift in buffer, for instance for whole message it will have
	 *            non zero value
	 * @return
	 * @throws IOException
	 */
	public Hashtable<Integer, Avp> decodeAvpSet(byte[] buffer, int shift)throws Exception {
		Hashtable<Integer, Avp> hs = new Hashtable<Integer, Avp>();
		int tmp, counter = shift;
		for (int c = 120; c < buffer.length; c++) {
			if (buffer[c] == -2 && buffer[c + 1] == -2 && buffer[c + 2] == -2
					&& buffer[c + 3] == -2 && buffer[c + 4] == -2) {
				buffer = Arrays.copyOfRange(buffer, 0, c);
				break;
			}
		}
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(
				buffer, shift, buffer.length /* - shift ? */));
		
		while (counter < buffer.length) {
			int code = in.readInt();
			tmp = in.readInt();
			int flags = (tmp >> 24) & 0xFF;
			int length = tmp & 0xFFFFFF;
			
			if (length < 0 || counter + length > buffer.length) {
					
					throw new Exception("Not enough data in buffer!");
			}
			long vendor = 0;
			if ((flags & 0x80) != 0) {
				vendor = in.readInt();
			}
			if (length <= 0) {
				throw new Exception("Not enough data in buffer!");
			}
			byte[] rawData = new byte[length - (8 + (vendor == 0 ? 0 : 4))];
			in.read(rawData);
			if (length % 4 != 0) {
				for (int i; length % 4 != 0; length += i) {
					i = (int) in.skip((4 - length % 4));
				}
			}
			hs.put(code, new Avp(code, (short) flags, (int) vendor, rawData));
			counter += length;
		}
		return hs;
	}

	public byte[] encodeAvpSet(HashSet<Avp> avps) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DataOutputStream data = new DataOutputStream(out);
			for (Avp a : avps) {
				data.write(encodeAvp(a));
			}
		} catch (Exception e) {
			System.out.println("Error during encode avps" + e.getMessage());
		}
		return out.toByteArray();
	}

	public byte[] encodeAvp(Avp avp) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			DataOutputStream data = new DataOutputStream(out);
			data.writeInt(avp.getCode());
			int flags = (byte) ((avp.vendor != 0 ? 0x80 : 0)
					| (avp.isMandatory ? 0x40 : 0) | (avp.isEncrypted ? 0x20
					: 0));
			int origLength = avp.rawData.length + 8 + (avp.vendor != 0 ? 4 : 0);
			data.writeInt(((flags << 24) & 0xFF000000) + origLength);
			if (avp.vendor != 0) {
				data.writeInt((int) avp.vendor);
			}
			data.write(avp.rawData);
			if (avp.rawData.length % 4 != 0) {
				for (int i = 0; i < 4 - avp.rawData.length % 4; i++) {
					data.write(0);
				}
			}
		} catch (Exception e) {
			System.out.println("Error during encode avp" + e.getMessage());
		}
		return out.toByteArray();
	}
	
	
	public void printPacket(byte[] b) throws Exception {
		int tmp;
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
		tmp = in.readInt();
		short version = (short) (tmp >> 24);
		if (version != 1) {
			throw new Exception("Illegal value of version " + version);
		}

		if (b.length != (tmp & 0x00FFFFFF)) {
			// throw new ParseException("Wrong length of data: " + (tmp &
			// 0x00FFFFFF));
			throw new Exception("Wrong length of data: " + (tmp & 0x00FFFFFF));
		}

		tmp = in.readInt();
		short flags = (short) ((tmp >> 24) & 0xFF);
		int commandCode = (int) (tmp & 0xFFFFFF);
		long applicationId = ((long) in.readInt() << 32) >>> 32;
		long hopByHopId = ((long) in.readInt() << 32) >>> 32;
		long endToEndId = ((long) in.readInt() << 32) >>> 32;

		System.out.println("Version: " + version);
		System.out.println("Flags: " + flags);
		System.out.println("CommandCode: " + commandCode);
		System.out.println("ApplicationId: " + applicationId);
		System.out.println("HopByHopId: " + hopByHopId);
		System.out.println("EndtoEndId: " + endToEndId);
		Hashtable<Integer, Avp> hs = decodeAvpSet(b, 20);
		for (Iterator<Avp> iterator = hs.values().iterator(); iterator.hasNext();) {
			Avp avp = iterator.next();
			System.out.println(avp.toString());
		}

	}

}
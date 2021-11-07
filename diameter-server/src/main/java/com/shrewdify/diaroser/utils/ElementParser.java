package com.shrewdify.diaroser.utils;

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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * erick.svenson@yahoo.coma
 * 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class ElementParser {

	Logger log=Logger.getLogger(getClass());
	private static final int INT_INET4 = 1;
	private static final int INT32_SIZE = 4;

	/**
	 * 
	 * @param rawData //bytes of integer
	 * @return int value
	 * @throws NoSpaceException
	 * 
	 * Converts the bytes into 32 bit Integer.
	 */
	public int bytesToInt(byte[] rawData) throws NoSpaceException {
		return prepareBuffer(rawData, INT32_SIZE).getInt();
	}

	protected ByteBuffer prepareBuffer(byte[] bytes, int len) throws NoSpaceException  {
		if (bytes.length != len)
			throw new NoSpaceException("Incorrect data length");
		return ByteBuffer.wrap(bytes);
	}

	/**
	 * 
	 * @param rawData //bytes of String
	 * @return
	 * @throws UnsupportedEncodingException
	 * Converts the String to Octet String
	 */
	public String bytesToOctetString(byte[] rawData) throws UnsupportedEncodingException {
		return new String(rawData, "iso-8859-1");
	}


	/**
	 * 
	 * @param rawData
	 * @return
	 * @throws UnknownHostException
	 * 
	 * Converts the Bytes Correspondingly to IPV4 or IPV6 address.
	 */
	public InetAddress bytesToAddress(byte[] rawData) throws UnknownHostException {
		InetAddress inetAddress;
		byte[] address;
		if (rawData[INT_INET4] == INT_INET4) {
			address = new byte[4];
			System.arraycopy(rawData, 2, address, 0, address.length);
			inetAddress = Inet4Address.getByAddress(address);
		} else {
			address = new byte[16];
			System.arraycopy(rawData, 2, address, 0, address.length);
			inetAddress = Inet6Address.getByAddress(address);
		}
		return inetAddress;
	}


	/**
	 *
	 * @param value
	 * @return
	 * 
	 * Converts  32 bit integer to bytes.
	 */
	public byte[] int32ToBytes(int value) {
		byte[] bytes = new byte[INT32_SIZE];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.putInt(value);
		return bytes;
	}


	/**
	 *
	 * @param value
	 * @return
	 * 
	 * Converts  64 bit integer to bytes.
	 */
	public byte[] int64ToBytes(int value) {
		byte[] bytes = new byte[INT32_SIZE];
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
	 * @throws NoSpaceException 
	 * @throws AvpDataException
	 */
	public Hashtable<Integer, Avp> decodeAvpSet(byte[] buffer, int shift)
			throws IOException, NoSpaceException {
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
				throw new NoSpaceException("Not enough data in buffer!");
			}
			long vendor = 0;
			if ((flags & 0x80) != 0) {
				vendor = in.readInt();
			}
			if (length <= 0) {
				throw new NoSpaceException("Not enough data in buffer!");
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

	/**
	 * @param avps
	 * @return
	 * @throws IOException
	 * 
	 * Converts the Avp Set to bytes.
	 */
	public byte[] encodeAvpSet(HashSet<Avp> avps) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		DataOutputStream data = new DataOutputStream(out);
		for (Avp a : avps) {
			data.write(encodeAvp(a));
		}
		return out.toByteArray();
	}

	/**
	 * 
	 * @param avp
	 * @return
	 * @throws IOException
	 * 
	 * Encodes the avp to bytes.
	 */
	public byte[] encodeAvp(Avp avp) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
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
		return out.toByteArray();
	}

	/**
	 * 
	 * @param b
	 * @throws NoSpaceException
	 * @throws IOException
	 * 
	 * Decode the bytes and change them correspondingly to Strings and Print them
	 */
	public void printPacket(byte[] b) throws NoSpaceException, IOException {
		int tmp;
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
		tmp = in.readInt();
		short version = (short) (tmp >> 24);
		if (version != 1) {
			throw new NoSpaceException("Illegal value of version " + version);
		}

		if (b.length != (tmp & 0x00FFFFFF)) {
			// throw new ParseException("Wrong length of data: " + (tmp &
			// 0x00FFFFFF));
			throw new NoSpaceException("Wrong length of data: " + (tmp & 0x00FFFFFF));
		}

		tmp = in.readInt();
		short flags = (short) ((tmp >> 24) & 0xFF);
		int commandCode = (int) (tmp & 0xFFFFFF);
		long applicationId = ((long) in.readInt() << 32) >>> 32;
		long hopByHopId = ((long) in.readInt() << 32) >>> 32;
		long endToEndId = ((long) in.readInt() << 32) >>> 32;

		log.debug("Version: " + version);
		log.debug("Flags: " + flags);
		log.debug("CommandCode: " + commandCode);
		log.debug("ApplicationId: " + applicationId);
		log.debug("HopByHopId: " + hopByHopId);
		log.debug("EndtoEndId: " + endToEndId);
		Hashtable<Integer, Avp> hs = decodeAvpSet(b, 20);
		for (Iterator<Avp> iterator = hs.values().iterator(); iterator
				.hasNext();) {
			Avp avp = iterator.next();
			log.debug(avp.toString());
		}

	}




	public HashSet<Avp> copyforResponse(int type, int code,
			Hashtable<Integer, Avp> hs) {
		HashSet<Avp> avpset = new HashSet<Avp>();
		avpset.add(hs.get(Dictionary.ORIGIN_HOST));
		avpset.add(hs.get(Dictionary.AUTH_APPLICATION_ID));
		avpset.add(hs.get(Dictionary.ORIGIN_REALM));
		if (type == 1) {
			avpset.add(hs.get(Dictionary.CC_REQUEST_TYPE));
			avpset.add(hs.get(Dictionary.CC_REQUEST_NUMBER));
			avpset.add(hs.get(Dictionary.SESSION_ID));
		} else if (type == 0) {
			avpset.add(hs.get(Dictionary.HOST_IP_ADDRESS));
			avpset.add(hs.get(Dictionary.VENDOR_ID));
			avpset.add(hs.get(Dictionary.PRODUCT_NAME));
			avpset.add(new Avp(Dictionary.SUPPORTED_VENDOR_ID, (short) 80, 0, int32ToBytes(10415)));
		}
		Avp a = new Avp(Dictionary.RESULT_CODE, (short) 80, 0, int32ToBytes(code));
		avpset.add(a);
		return avpset;
	}


	/**
	 * 
	 * @param a1
	 * @return
	 * 
	 * Converts bytes to MCC and MNC Code.
	 */
	public String[] mcc_mnc(byte[] a1) {
		int test[] = new int[15];
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] < 0) {
				int p = (Math.abs(a1[i]) - 1);
				int dec = Integer.parseInt(Integer.toBinaryString(p));
				String keww = String.format("%08d", dec);
				test[i] = Integer.parseInt((keww.replaceAll("1", "x")
						.replaceAll("0", "1").replaceAll("x", "0")), 2);
			} else {
				test[i] = a1[i];
			}
		}
		String b1, b2, b3;
		b1 = new StringBuffer(Integer.toHexString(test[1])).reverse()
				.toString();
		b2 = new StringBuffer(Integer.toHexString(test[2])).reverse()
				.toString();
		char b4[] = b2.toCharArray();
		b3 = new StringBuffer(Integer.toHexString(test[3])).reverse()
				.toString();
		if (b4[1] == 'f') {
			return new String[] { b1 + b4[0], b3 };
		} else {
			return new String[] { b1 + b4[0], b3 + b4[1] };
		}
	}


	/**
	 * 
	 * @param input
	 * @return
	 * 
	 * Extracts the number from the String.
	 */
	public String digNumber(String input) {
		input = input.split(":")[1];
		input = (input.startsWith("+")) ? input.substring(1) : input;
		input = (input.split(";").length > 1) ? input.split(";")[0] : input;
		return input;
	}

	/**
	 * 
	 * 
	 * @param hs
	 * @param uri
	 * @return
	 * @throws NoSpaceException
	 * @throws UnsupportedEncodingException
	 * 
	 * Check valid for the given data, it checks for valid Realm Name, Auth application Id, Orgin Host.
	 */
	public int checkValid(Hashtable<Integer, Avp> hs, String uri) throws NoSpaceException, UnsupportedEncodingException {
		String realm = new String(hs.get(Dictionary.ORIGIN_REALM).rawData);
		int authappid =bytesToInt(hs.get(Dictionary.AUTH_APPLICATION_ID).rawData);
		String uri1 = bytesToOctetString(hs.get(Dictionary.ORIGIN_HOST).rawData);
		if(realm.equals(Config.realm)){
			if(authappid == 4){
				if(uri1.equals(uri)){
					return 1;
				}
				else
					return -5;
			}
			else return -2;
		}
		return -1;
	}

	/**
	 * 
	 * @param buf
	 * @param temp
	 * @return
	 * 
	 * It merges both the arrays and repalces the Header information.
	 */
	public byte[] merge(byte[] buf, byte[] temp) {
		byte spack[] = new byte[buf.length + temp.length];
		for (int i = 0; i < temp.length; i++)
			spack[i] = temp[i];
		for (int i = 0; i < buf.length; i++)
			spack[i + temp.length] = buf[i];
		byte t[] = int32ToBytes(spack.length);
		for (int i = 0; i < 3; i++)
			spack[i + 1] = t[i + 1];
		spack[4] = 0;
		return spack;
	}




}
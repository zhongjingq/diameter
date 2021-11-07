package com.shrewdify.diaro.utils;
import java.nio.ByteBuffer;
import java.util.HashSet;

/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 */

public class Avp {

	public int code;
	public short flags;
	public int vendor;
	public byte[] rawData;
	public boolean isMandatory, isEncrypted, isVendorSpecific;
	

	public Avp(int code, short flags, int vendor, byte[] rawData) {

		isMandatory = (flags & 0x40) != 0;
		isEncrypted = (flags & 0x20) != 0;
		isVendorSpecific = (flags & 0x80) != 0;

		this.code = code;
		this.flags = flags;
		this.vendor = vendor;
		this.rawData = rawData;
	}

	Avp(Avp avp) {
		code = avp.getCode();
		vendor = avp.getVendor();
		isMandatory = avp.isMandatory;
		isEncrypted = avp.isEncrypted;
		isVendorSpecific = avp.isVendorSpecific;
		try {
			rawData = avp.getData();
		} catch (Exception e) {
			System.out.println("Can not create Avp" + e.getMessage());
		}
	}

	public int getCode() {
		return code;
	}

	public short getFlags() {
		return flags;
	}

	public int getVendor() {
		return vendor;
	}

	public byte[] getData() {
		return rawData;
	}

	public ElementParser par;
	
	public Avp getAvp(int code){
		try{
		par=new ElementParser();
		return par.decodeAvpSet(this.rawData, 0).get(code);
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public void addAvp(Avp a){
		try{
			par=new ElementParser();
			 this.rawData=par.encodeAvp(a);
			}
			catch(Exception e){
				e.printStackTrace();
			
			}
	}
	
	public void addSetAvp(HashSet<Avp> a){
		try{
			par=new ElementParser();
			 this.rawData=par.encodeAvpSet(a);
			}
			catch(Exception e){
				e.printStackTrace();
			
			}
	}

	public String toString() {

		String str = new String(rawData);
		if (str.length() < 8) {
			ByteBuffer bf = ByteBuffer.wrap(rawData);
			if (code == 257) {
				try {
					par = new ElementParser();
					return "Code:" + code + ";Flags:" + flags + ";Vendor:"
							+ vendor + ";Raw1:" + par.bytesToAddress(rawData);
				} catch (Exception e) {
					// System.out.println("Error:"+e.getMessage());
				}
			}
			return "Code:" + code + ";Flags:" + flags + ";Vendor:" + vendor
					+ ";Raw:" + bf.getInt();
		}
		return "Code:" + code + ";Flags:" + flags + ";Vendor:" + vendor
				+ ";Data:" + (new String(rawData));
	}
}

package com.shrewdify.diaro.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Hashtable;

import com.shrewdify.diaro.utils.ElementParser;
/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */

public abstract class Connector implements Runnable{
	
	protected ElementParser par=new ElementParser();
	protected DataInputStream in;
	protected DataOutputStream out;
	protected Socket s;
	protected String uri;
	protected Hashtable<String, Hashtable<String, String>> sessions;
	public boolean isPeered;
	
	public abstract byte[] sendPeerRequest() throws Exception;
	public abstract void sendCCRequestI(String aNumber,String bNumber,String sessionId) throws Exception;
	public abstract void sendCCRequestU(String sessionId) throws Exception;
	public abstract void sendCCRequestT(String sessionId) throws Exception;
	public abstract void sendCCRequestE(String sessionId) throws Exception;
	public abstract void processBillingResponse(byte[] request);
	
	
	
	
}

package com.shrewdify.diaro.main;
import java.net.InetAddress;
import java.net.Socket;

import com.shrewdify.diaro.impl.ConnectorImpl;
import com.shrewdify.diaro.utils.ElementParser;
/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */


public class ClientIO extends ElementParser {

	public ClientIO(String host,String testCount) {
		try {
			System.out.println("Starting");
			int n=Integer.parseInt(testCount);
			Socket s=new Socket(InetAddress.getByName(host),3868);
			ConnectorImpl p=new ConnectorImpl(s,n);
			while(!p.isPeered);
			Thread t=new Thread(p);
			t.start();
		} catch (Exception e) {
			System.out.println("Error:" + e.getMessage());
		}
	}

	
}

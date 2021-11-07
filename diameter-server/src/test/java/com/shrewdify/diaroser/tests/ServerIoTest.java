package com.shrewdify.diaroser.tests;

import com.shrewdify.diaroser.main.ServerIO;
import org.junit.After;
import org.junit.Test;

import java.net.InetAddress;
import java.net.Socket;

public class ServerIoTest {

	ServerIO sddat;

	@Test
	public void test1() {
		try{
		sddat = new ServerIO("acco.properties");
		}
		catch(Exception e){}
		sddat.stopServer();
	}
	
	@Test
	public void test3(){
		
		class testServer implements Runnable{

			@Override
			public void run() {
				try{
				sddat = new ServerIO("accounts.properties");
				}
				catch(Exception e){}
			}
			
		}
		Thread t=new Thread(new testServer());
		t.start();
		try{
		Socket c=new Socket(InetAddress.getLocalHost(), 3868);
		byte b[]={1, 0, 0, -128, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, 1, 64, 0, 0, 14, 0, 1, 10, 14, 0, -25, 0, 0, 0, 0, 1, 11, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 10, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 13, 0, 0, 0, 19, 116, 114, 117, 68, 105, 97, 109, 101, 116, 101, 114, 0, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101};
		c.getOutputStream().write(b);	
		}
		catch(Exception e){}
	//	sddat.stopServer();
	}

	@After
	public void test2() {

		class testServer implements Runnable{

			@Override
			public void run() {
				try{
				sddat = new ServerIO("accounts.properties");
				}
				catch(Exception e){}
			}
			
		}
		Thread t=new Thread(new testServer());
		t.start();
		try{
		//Thread.sleep(1000);
		
		Socket c=new Socket(InetAddress.getLocalHost(), 3868);
		byte b[]={1, 0, 0, -128, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, 1, 64, 0, 0, 14, 0, 1, 10, 14, 0, -25, 0, 0, 0, 0, 1, 11, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 10, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 13, 0, 0, 0, 19, 116, 114, 117, 68, 105, 97, 109, 101, 116, 101, 114, 0, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101};
		c.getOutputStream().write(b);	
		Thread.sleep(1000);
		sddat.stopServer();
		}
		catch(Exception e){}
		
	}

	
}

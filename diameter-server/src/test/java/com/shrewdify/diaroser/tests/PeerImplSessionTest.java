package com.shrewdify.diaroser.tests;

import com.shrewdify.diaroser.impl.PeerImpl;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class PeerImplSessionTest extends TestCase{

	/*
	@Test
	public void test99() throws Exception{
		try{

			class Temp implements Runnable{
				ServerIO ser=null;
				@Override
				public void run() {
					ser=new ServerIO("accounts.properties");
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				public void stop(){
					try{
						ser.stopServer();
					}catch(Exception e){}
				}

			}
			Temp te=new Temp();
			Thread t=new Thread(te);
			t.start();

			final Socket s=new Socket(InetAddress.getLocalHost(),3868);
			InputStream is = getClass().getClassLoader().getResourceAsStream("accounts.properties");
			if(is != null){
				Properties properties = new Properties();
				try {
					properties.load(is);
				} catch (IOException e) {
					e.printStackTrace();
				}

				final String dbhost = (String) properties.getProperty("dbhost");
				final String dbname = (String) properties.getProperty("dbname");
				final String dbuser = (String) properties.getProperty("dbuser");
				final String dbpassword = (String) properties.getProperty("dbpassword");
				final String className = (String) properties.getProperty("classname");
				class Temp1 implements Runnable{
					public void run() {
						try{
							PeerImpl pe=new PeerImpl(s,5,5,className,dbhost,dbname,dbuser,dbpassword);
						}
						catch(Exception e){
							System.err.println("Err:"+e.getMessage());
						}
					}
				}
				Thread t1=new Thread(new Temp1());
				t1.start();
				byte b[]={1, 0, 0, -128, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, 1, 64, 0, 0, 14, 0, 1, 10, 14, 0, -25, 0, 0, 0, 0, 1, 11, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 10, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 13, 0, 0, 0, 19, 116, 114, 117, 68, 105, 97, 109, 101, 116, 101, 114, 0, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101};
				s.getOutputStream().write(b);
				Thread.sleep(2000);
				te.stop();
			}

		}
		catch(Exception e){

		}


	}
	 */
	@Test
	public void test56() throws Exception{
		final ServerSocket sc=new ServerSocket(9898);
		class tempServer implements Runnable{

			@Override
			public void run() {
				try{
					Socket s=sc.accept();
					PeerImpl p=null;
					try {
						InputStream is = getClass().getClassLoader()
								.getResourceAsStream("accounts.properties");
						if(is != null){
							Properties properties = new Properties();
							try {
								properties.load(is);
							} catch (IOException e) {
								e.printStackTrace();
							}
							final String dbhost = (String) properties.getProperty("dbhost");
							final String dbname = (String) properties.getProperty("dbname");
							final String dbuser = (String) properties.getProperty("dbuser");
							final String dbpassword = (String) properties.getProperty("dbpassword");
							final String className = (String) properties.getProperty("classname");
							final String dbcdr = (String) properties.getProperty("dbcdr");
							p = new PeerImpl(s,5,1,className,dbhost,dbname,dbuser,dbpassword,dbcdr);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 
					Thread t=new Thread(p);
					t.start();
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		Thread t1=new Thread(new tempServer());
		t1.start();

		class tempClient implements Runnable{

			@Override
			public void run() {
				try{
					final Socket s=new Socket(InetAddress.getLocalHost(), 9898);
					byte b[]={1, 0, 0, -128, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, 1, 64, 0, 0, 14, 0, 1, 10, 14, 0, -25, 0, 0, 0, 0, 1, 11, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 10, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 13, 0, 0, 0, 19, 116, 114, 117, 68, 105, 97, 109, 101, 116, 101, 114, 0, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101};
					DataOutputStream out=new DataOutputStream(s.getOutputStream());
					out.write(b);
					Thread.sleep(1000);
					byte b1[]={1, 0, 1, -68, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, -69, 64, 0, 0, 40, 0, 0, 1, -62, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, -68, 64, 0, 0, 19, 49, 56, 54, 50, 50, 52, 50, 57, 49, 50, 56, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101, 0, 0, 1, -96, 64, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, -51, 64, 0, 0, 43, 110, 103, 105, 110, 46, 50, 56, 52, 53, 56, 46, 48, 48, 48, 46, 48, 48, 48, 46, 57, 46, 51, 50, 50, 54, 48, 64, 51, 103, 112, 112, 46, 111, 114, 103, 0, 0, 0, 1, 7, 64, 0, 0, 44, 116, 101, 115, 116, 95, 49, 48, 46, 52, 53, 57, 50, 53, 49, 49, 53, 51, 55, 56, 53, 54, 53, 53, 49, 51, 56, 49, 49, 55, 50, 52, 53, 55, 51, 51, 56, 0, 0, 1, 27, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101, 0, 0, 3, 105, -64, 0, 0, -88, 0, 0, 40, -81, 0, 0, 3, 106, -64, 0, 0, 32, 0, 0, 40, -81, 0, 0, 0, 22, -96, 0, 0, 17, 0, 0, 40, -81, 0, 98, -8, 48, 0, 0, 0, 0, 0, 0, 3, 108, -64, 0, 0, 124, 0, 0, 40, -81, 0, 0, 3, 64, -64, 0, 0, 27, 0, 0, 40, -81, 116, 101, 108, 58, 49, 50, 48, 49, 56, 55, 56, 56, 49, 55, 48, 0, 0, 0, 3, 61, -64, 0, 0, 16, 0, 0, 40, -81, 0, 0, 0, 1, 0, 0, 3, 73, -64, 0, 0, 22, 0, 0, 40, -81, 48, 50, 51, 52, 99, 97, 48, 48, 51, 55, 0, 0, 0, 0, 3, 63, -64, 0, 0, 27, 0, 0, 40, -81, 116, 101, 108, 58, 49, 56, 54, 50, 50, 52, 50, 57, 49, 50, 56, 0, 0, 0, 3, 94, -64, 0, 0, 16, 0, 0, 40, -81, 0, 0, 0, 6, 0, 0, 1, -56, 64, 0, 0, 28, 0, 0, 1, -75, 64, 0, 0, 20, 0, 0, 1, -95, 64, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, -57, 64, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, -97, 64, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0};
					out.write(b1);

				}
				catch(Exception e){
					System.out.println("Error:"+e.getMessage());
				}
			}

		}
		Thread t2=new Thread(new tempClient());
		t2.start();
		try{Thread.sleep(10000);}
		catch(Exception e){}

	}

}
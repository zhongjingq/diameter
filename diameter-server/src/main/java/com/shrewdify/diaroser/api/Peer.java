package com.shrewdify.diaroser.api;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.shrewdify.diaroser.utils.DbHelper;
import com.shrewdify.diaroser.utils.ElementParser;
import com.shrewdify.diaroser.utils.NoSpaceException;
/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */

public abstract class Peer implements Runnable {

	//protected Lock lock=new ReentrantLock();
	protected ElementParser par = new ElementParser();
	protected DataInputStream in;
	protected DataOutputStream out;
	protected Socket s;
	public String uri;
	protected Hashtable<String, Hashtable<String, String>> sessions;
	protected EventQueue requestQueue=new EventQueue();
	protected EventQueue responseQueue=new EventQueue();
	protected FaultQueue faultHandlerQueue=new FaultQueue();
	protected CDRQueue cDRQueue=new CDRQueue();
	protected List<ChildThread> workerPool=new ArrayList<ChildThread>();
	public boolean isPeered;

	public abstract byte[] processPeerRequest(byte[] request) throws IOException, NoSpaceException;

	public abstract void stop() throws IOException;
	
	protected abstract class UnFaultSessionRemover implements Runnable{
		protected DbHelper db=new DbHelper();
	}

	public abstract class ServerFaultHandler implements Runnable{
		protected DbHelper db=new DbHelper();
	}
	
	public abstract class WriterThread implements Runnable{
		protected DataOutputStream out;
	}
	
	public class EventQueue extends LinkedList<byte[]>{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public Lock lock=new ReentrantLock();

		public boolean getLock(){
			return lock.tryLock();
		}

	}
	
	public class FaultQueue extends LinkedList<String>{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public Lock lock=new ReentrantLock();

		public boolean getLock(){
			return lock.tryLock();
		}

	}
	
	public class CDRQueue extends LinkedList<HashMap<String,String>>{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public Lock lock=new ReentrantLock();

		public boolean getLock(){
			return lock.tryLock();
		}

	}

	protected abstract class CDRGenerator implements Runnable{
		protected DbHelper db=new DbHelper();
	}

	protected abstract class ChildThread implements Runnable{

		protected DbHelper db=new DbHelper();
		protected ElementParser par = new ElementParser();
		//protected Lock lock=new ReentrantLock();

		public abstract byte[] processCCRI(byte[] b)throws IOException, NoSpaceException,SQLException,ArithmeticException; 
		public abstract byte[] processCCRU(byte b[])throws IOException, NoSpaceException,SQLException,ArithmeticException;
		public abstract byte[] processCCRT(byte b[])throws IOException, NoSpaceException,SQLException,ArithmeticException;
		public abstract byte[] processCCRE(byte b[])throws IOException, NoSpaceException,SQLException,ArithmeticException;
		public abstract byte[] processBillingRequest(byte[] request)throws IOException, NoSpaceException,SQLException,ArithmeticException;	

	}

}

package com.shrewdify.diaroser.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import com.shrewdify.diaroser.api.Peer;
import com.shrewdify.diaroser.utils.Avp;
import com.shrewdify.diaroser.utils.Config;
import com.shrewdify.diaroser.utils.Constants;
import com.shrewdify.diaroser.utils.Dictionary;
import com.shrewdify.diaroser.utils.NoSpaceException;
import com.shrewdify.diaroser.utils.PeerTable;

/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */

public class PeerImpl extends Peer {
	Logger log=Logger.getLogger(getClass());
	public PeerImpl(String uri){
		this.uri=uri;
		sessions = new Hashtable<String, Hashtable<String, String>>();
	}

	/**
	 * 
	 * @param s socket 
	 * @param cleaningdelay
	 * @param numberOfWorkers
	 * @param className
	 * @param dbhost
	 * @param dbname
	 * @param dbuser
	 * @param dbpassword
	 * @param dbcdr
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSpaceException
	 * 
	 * 
	 * It creates instances of PeerImpl and starts All the threads Worker,Writer,FaultHandler,,GarbageSessionCleaner,CDRGenerator, 
	 * Each thread has only one instance apart from Worker Thread. The no of worker threads initiated are depends on numberofworkers obtained as input for creating this object.
	 * 
	 * 
	 */
	public PeerImpl(Socket s,int cleaningdelay,int numberOfWorkers,String className,String dbhost,String dbname,String dbuser,String dbpassword,String dbcdr)  throws NullPointerException,IOException, NoSpaceException{
		sessions = new Hashtable<String, Hashtable<String, String>>();
		this.s = s;
		this.in = new DataInputStream(s.getInputStream());
		this.out = new DataOutputStream(s.getOutputStream());
		String ip = s.getInetAddress().getHostAddress();
		log.debug("Opening Peer Connection with Client with IP:" + ip);
		int tmp = in.readInt();
		short version = (short) (tmp >> 24);
		int length = (tmp & 0x00FFFFFF);
		byte b[] = new byte[length - 4];
		in.read(b, 0, length - 4);
		byte buf[] = new byte[b.length + 4];
		byte t[] = par.int32ToBytes(length);
		buf[0] = (byte) version;
		for (int i = 0; i < 3; i++) 
			buf[i + 1] = t[i + 1];
		for (int i = 0; i < b.length; i++) 
			buf[t.length + i] = b[i];
		log.debug("Processing Peer Request");
		buf = processPeerRequest(buf);
		par.printPacket(buf);
		if (buf != null) {
			try {
				if(PeerTable.peerTable.containsKey(uri) ){
					s.close();
					log.debug("Peer with uri:"+uri+" already exists");
				}
				else{
					out.write(buf);
					if(flag){
						log.debug("Accepted Peer with Uri:" + uri
								+ " with IP:" + ip);
						for(int i=0;i<numberOfWorkers;i++)
						{
							Worker w=new Worker(className, dbhost, dbname, dbuser, dbpassword);
							Thread wt=new Thread(w);
							wt.start();
							workerPool.add(w);
							log.info("Starting thread:"+(i+1));
						}
						//Writer Thread
						Writer w=new Writer(out);
						Thread tw=new Thread(w);
						tw.start();

						//FaultHandler Thread
						FaultHandler fh=new FaultHandler(className, dbhost, dbname, dbuser, dbpassword);
						tw=new Thread(fh);
						tw.start();

						//Cleaner Thread
						GarbageSessionCleaner gsc=new GarbageSessionCleaner(cleaningdelay, className, dbhost, dbname, dbuser, dbpassword);
						tw=new Thread(gsc);
						tw.start();
						
						//Adding CDR'S
						CDRGeneratorThread cdrAdding = new CDRGeneratorThread(className, dbhost, dbcdr, dbuser, dbpassword);
						//System.out.println(dbcdr);
						tw = new Thread(cdrAdding);
						tw.start();
						
						isPeered = Constants.TR_VAL;
					}
				}
			} catch (Exception e) {
				System.out.println("rejected "+ e);
				log.debug("Rejected Peer with IP:" + ip);
				out.close();
				in.close();
				this.s.close();
			}
		}

	}


	/**
	 * 
	 * @param i
	 * 
	 * It Delays each thread with out processing for given milliseconds.
	 */
	public void delay(int i){
		try{Thread.sleep(i);}catch(Exception e){}
	}

	private boolean flag=false; 

	
	/**
	 * 
	 * 
	 * Returns response for the incoming requests. the request are received in bytes. it separates header from bytes and check for Orgin Host, Realm Name and AuthAPPid.
	 *  When Corresponding Match found it add's success code to content or add's the corresponding error code. The Updated Content is returned with header.
	 */
	@Override
	public byte[] processPeerRequest(byte b[]) throws IOException, NoSpaceException  {
		log.debug("Processing Exchange");
		//System.out.println("------ exchange");
		HashSet<Avp> avpset = new HashSet<Avp>();
		byte temp[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
		Hashtable<Integer, Avp> hs = par.decodeAvpSet(b, Constants.TWENTY);
		String realm = new String(hs.get(Dictionary.ORIGIN_REALM).rawData);
		int authappid = par.bytesToInt(hs.get(Dictionary.AUTH_APPLICATION_ID).rawData);
		uri = new String(hs.get(Dictionary.ORIGIN_HOST).rawData);
		if (realm.equals(Config.realm) && authappid == 4) {

			avpset = par.copyforResponse(Constants.ZERO, Constants.SUCCESS_CODE, hs);
			Avp a = hs.get(Dictionary.ORIGIN_HOST);
			a.rawData = Config.uri.getBytes();
			avpset.add(a);
			HashSet<Avp> grouped = new HashSet<Avp>();
			grouped.add(new Avp(Dictionary.VENDOR_ID, (short) Constants.EIGHTY, Constants.ZERO, par.int32ToBytes(10415)));
			grouped.add(hs.get(Dictionary.AUTH_APPLICATION_ID));
			a = new Avp(Dictionary.VENDOR_SPECIFIC_APPLICATION_ID, (short) Constants.EIGHTY, Constants.ZERO, null);
			a.addSetAvp(grouped);
			avpset.add(a);
			log.debug("Processed Exchange Request");
			flag=true;
			return par.merge(par.encodeAvpSet(avpset), temp);
		}
		else{
			if(!realm.equals(Config.realm)){

				//	System.out.println("error");
				avpset = par.copyforResponse(Constants.ZERO, Constants.UNKNOWNREALM_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);				
			}
			else{
				avpset = par.copyforResponse(Constants.ZERO, Constants.UNKNOWNAUTHID_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);
			}
			
		}
		//	/	return null;
	}

	
	
	@Override
	public void run() {
		try {
			log.debug("Ready to accept CCRs");
			while (!s.isClosed()) {
				int tmp = in.readInt();
				short version = (short) (tmp >> 24);
				int length = (tmp & 0x00FFFFFF);
				byte b[] = new byte[length - 4];
				in.read(b, 0, length - 4);
				byte buf[] = new byte[b.length + 4];
				byte t[] = par.int32ToBytes(length);
				buf[0] = (byte) version;
				for (int i = 0; i < 3; i++) {
					buf[i + 1] = t[i + 1];
				}
				for (int i = 0; i < b.length; i++) {
					buf[t.length + i] = b[i];
				}
				try {
					par.printPacket(buf);
				} catch (Exception e) {
				}
				//lock.lock();
				boolean loc=false;
				try{
					int commandCode=(int)(par.bytesToInt(Arrays.copyOfRange(buf, 4, 8)) & 0xFFFFFF);
					if(commandCode==272){
						//	System.out.println("Got Packet:"+(h++));
						loc=requestQueue.getLock();
						while(!loc){
							loc=requestQueue.getLock();
							//	delay(1);
						}
						if(loc){
							log.debug("Got Packet sending to queue");
							//	System.out.println(Arrays.toString(buf));
							requestQueue.add(buf);

						}
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
				finally{
					if(loc){
						log.debug("Number of requests in queue:"+requestQueue.size());
						requestQueue.lock.unlock();
					}
					delay(1);
				}

				//lock.unlock();

				buf = null;
			}
		} catch (IOException e) {
			String storevale=null;
			for(String s : PeerTable.peerTable.keySet()){
				if(this.equals(PeerTable.peerTable.get(s)))
					storevale =s;
			}
			PeerTable.peerTable.remove(storevale);
			log.debug("Client with uri:" + uri + " closed");
		}
	}

	int h=0;
	static int k=0,k1=0;

	public void stop() throws IOException {
		s.close();
	}

	
/**
 * 
 * It Creates an object which writes to DB for all the successful transactions. 
 *
 */
	public class CDRGeneratorThread extends CDRGenerator{
  
		/**
		 * 
		 * @param className
		 * @param dbhost
		 * @param dbname
		 * @param dbuser
		 * @param dbpassword
		 * @throws ClassNotFoundException
		 * @throws SQLException
		 * 
		 * It initializes the Database handler.
		 */
		public CDRGeneratorThread(String className,String dbhost,String dbname,String dbuser,String dbpassword) throws ClassNotFoundException,SQLException{
		
			//System.out.println(dbname);
			db.init(className, dbhost, dbname, dbuser, dbpassword);
		}

		
		/**
		 * 
		 * 
		 */
		@Override
		public void run() {
			while(true){
				boolean loc=false;
				HashMap<String, String> cdrData= null;
				try{
					if(cDRQueue.size()>0){
						loc=cDRQueue.getLock();
						if(loc)
							cdrData=cDRQueue.remove();						
					}
				}
				catch(Exception e){}
				finally{
					if(loc)
						cDRQueue.lock.unlock();
				}
				if(cdrData!=null){
					try{
						//	db.makeCDREntry(cdrData);
						db.addCdrRecords(cdrData);
					}catch(SQLException e){
						System.out.println("Exception" +  e);
					}
					delay(1);
				}
			}	

		}

	}


	
	/**
	 * It creates an Object to remove All the expired Sessions from the database and from the Local memory sessions.
	 * 
	 *
	 */
	public class GarbageSessionCleaner extends UnFaultSessionRemover{
		int delayInMinutes;
		
		/**
		 * 
		 * @param delayInMinutes
		 * @param className
		 * @param dbhost
		 * @param dbname
		 * @param dbuser
		 * @param dbpassword
		 * @throws SQLException
		 * @throws ClassNotFoundException
		 * 
		 * It initializes the Database handler and the delay time (i.e amount of time it has to wait before deleting the session).
		 */
		public GarbageSessionCleaner(int delayInMinutes,String className,String dbhost,String dbname,String dbuser,String dbpassword) throws SQLException,ClassNotFoundException{
			this.delayInMinutes=delayInMinutes;
			db.init(className, dbhost, dbname, dbuser, dbpassword);
		}
		
		
		/**
		 * It starts a thread which continuously remove sessions from db which are inserted or updated before the delay time, 
		 * those requests are also removed from the session, it is used as garbage collector.
		 *  
		 */
		
		
		@Override
		public void run() {
			while(true){
				try{
					String str[]=db.removeSession(System.currentTimeMillis()-(delayInMinutes*Constants.SIXTY*Constants.THOUSAND));
					synchronized (sessions) {
						for (String string : str) 
							sessions.remove(string);
					}
					Thread.sleep(delayInMinutes*Constants.SIXTY*Constants.THOUSAND);
				}
				catch(Exception e){}
			}
		}
	}


	/**
	 * 
	 * 
	 * It creates an Object to Store All the Sessions to database. 
	 *
	 */
	public class FaultHandler extends ServerFaultHandler{

	/**
	 * 
	 * @param className
	 * @param dbhost
	 * @param dbname
	 * @param dbuser
	 * @param dbpassword
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * 
	 * It initiates database with the parameters passed to it.
	 */
		public FaultHandler(String className,String dbhost,String dbname,String dbuser,String dbpassword) throws ClassNotFoundException,SQLException{
			db.init(className, dbhost, dbname, dbuser, dbpassword);
			//System.out.println("initalizing");
		}
		
		/**
		 * It starts a thread which continuously checks for the queue size, 
		 * if queue contains some requests of Session data  it tries to acquire lock on it removes 
		 * the requests from queue and release the lock and request db handler to write to database..
		 *  
		 */
		@Override
		public void run() {
			//System.out.println("not cmg");
			while(true){
				
				boolean loc=false;
				String sessionData=null;
				try{
					if(faultHandlerQueue.size()>Constants.ZERO){
						loc=faultHandlerQueue.getLock();
						if(loc)
							sessionData=faultHandlerQueue.remove();
					}
				}
				catch(Exception e){}
				finally{
					if(loc)
						faultHandlerQueue.lock.unlock();
				}
				if(sessionData!=null){
					try{
						db.setSeesionData(sessionData.split("~")[0], sessionData.split("~")[1]);
					}catch(SQLException e){}
					delay(1);
				}
			}
		}
	}


	
	/**
	 * 
	 * It creates an object which handles responses and writes back to the client session
	 *
	 */
	public class Writer extends WriterThread{
		/**
		 * 
		 * @param out1
		 * It initializes with Data output Stream.
		 */
		public Writer(OutputStream out1){
			out=new DataOutputStream(out1);
		}
		/**
		 * It starts a thread which continuously checks for the queue size, 
		 * if queue contains some responses it tries to acquire lock on it removes 
		 * the response from queue and release the lock and writes the response to Client.
		 * 
		 */
		@Override
		public void run() {
			while(true){
				boolean loc=false;
				byte[] b=null;
				if(responseQueue.size()>Constants.ZERO ){
					try{
						loc=responseQueue.getLock();
						if(loc){
							log.debug("Number of responses in queue:"+responseQueue.size());
							b=responseQueue.remove();
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
					finally{
						if(loc)
							responseQueue.lock.unlock();
					}
					if(b!=null){
						try {
							out.write(b);
							delay(1);
						} catch (IOException e) {
							try {
								PeerImpl.this.stop();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} 			
					}
				}
				else
					delay(1);
			}
		}
	}

	/**
	 * 
	 * It Creates and object of worker which process the incoming requests from Queue.
	 *
	 */
	public class Worker extends ChildThread{

		/**
		 * 
		 * @param className
		 * @param dbhost
		 * @param dbname
		 * @param dbuser
		 * @param dbpassword
		 * @throws SQLException
		 * @throws ClassNotFoundException
		 * 
		 * It intimates the database with the given inputs for the worker Object.
		 */
		
		public Worker(String className,String dbhost,String dbname,String dbuser,String dbpassword) throws SQLException,ArithmeticException,ClassNotFoundException{
			db.init(className, dbhost, dbname, dbuser, dbpassword);
		}
		/**
		 * It starts a thread which continuously checks for the queue size, 
		 * if queue contains some request it tries to acquire lock on it removes 
		 * the request from queue and release the lock and process the requests.  
		 */
		@Override
		public void run() {
			while(true){
				byte b[]=null;
				boolean loc=false;
				if(requestQueue.size()>Constants.ZERO){
					try{
						loc=requestQueue.getLock();
						if(loc)
							b=requestQueue.remove();
					}
					catch(Exception e){
						b=null;
					}
					finally{
						if(loc)
							requestQueue.lock.unlock();
					}
					if(b!=null){
						try {
							b=processBillingRequest(b);
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (NoSpaceException e1) {
							e1.printStackTrace();
						} catch (SQLException e1){
							e1.printStackTrace();
						}catch (ArithmeticException e1){
							e1.printStackTrace();
							try {
								s.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						
						}

						if(b!=null){
							try{
								//	System.out.println("Added Response:"+(k1++));
								loc=responseQueue.getLock();
								while(!loc){
									loc=responseQueue.getLock();
									delay(1);
								}
								if(loc)
									responseQueue.add(b);
							}
							catch(Exception e){
								e.printStackTrace();
							}
							finally{
								if(loc){
									responseQueue.lock.unlock();
									delay(1);
								}
							}
							log.debug("Processed a packet from queue");
						}
					}
				}else
					delay(1);
			}

		}



		/**
		 * It process the Initial Requests , it extracts Sender No, Receiver No, Mcc Code, Mnc Code 
		 * form this it gets the tariff rates and generates response. Before returning the response it store all the information to session.		 * 
		 *  
		 */
		@Override
		public byte[] processCCRI(byte[] b) throws IOException, NoSpaceException ,SQLException,ArithmeticException{
			log.debug("Processing CCRI Request");

			HashSet<Avp> avpset = null;
			byte header[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
			int resCode;
			int grantedUnits;
			header[4] = Constants.ZERO;
			Hashtable<Integer, Avp> hs = par.decodeAvpSet(b, Constants.TWENTY);
			int validity=par.checkValid(hs, uri);
			if (validity>Constants.ZERO) {
				log.debug("CCRI Request is a valid request");
				Hashtable<String, String> params = new Hashtable<String, String>();

				Avp loc = par.decodeAvpSet(
						par.decodeAvpSet(hs.get(Dictionary.SERVICE_INFORMATION).getData(), 0).get(Dictionary.PS_INFORMATION)
						.getData(), 0).get(Dictionary.TGPP_USER_LOCATION);
				String sessionId = ("" + par
						.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData)) + uri;
				String mncCode = par.mcc_mnc(loc.getData())[1], mccCode = par.mcc_mnc(loc
						.getData())[0];
				String senderNumber = par.digNumber(par
						.bytesToOctetString((par.decodeAvpSet(
								par.decodeAvpSet(hs.get(Dictionary.SERVICE_INFORMATION).getData(), 0)
								.get(Dictionary.IMS_INFORMATION).getData(), 0).get(Dictionary.CALLING_PARTY_ADDRESS).rawData)));
				String receiverNumber = par.digNumber(par
						.bytesToOctetString((par.decodeAvpSet(
								par.decodeAvpSet(hs.get(Dictionary.SERVICE_INFORMATION).getData(), 0)
								.get(Dictionary.IMS_INFORMATION).getData(), 0).get(Dictionary.CALLED_PARTY_ADDRESS).rawData)));
				
				String subscriptionID = par.bytesToOctetString((par.decodeAvpSet(hs.get(Dictionary.SUBSCRIPTION_ID).getData(), 0).get(Dictionary.SUBSCRIPTION_ID_DATA).rawData));
				
				System.out.println("++++++++++++" +subscriptionID + "Anumber" + senderNumber + "Bnumber"+ receiverNumber);
				log.info("CCRI Request "+senderNumber+"---->"+receiverNumber);
				int userBalance = db.readUserBalance(subscriptionID);
				params.put(Constants.RESERVED_BALANCE, "" + userBalance);
				int billingInfo[] = db.getSessionBillingUnits(
						senderNumber, receiverNumber);
				System.out.println(Arrays.toString(billingInfo));
				int balAfterInit = Constants.ZERO;
				if (userBalance >= Constants.ZERO) {
					grantedUnits = Constants.MINUSONE;
					if (mccCode.equals(Config.unBillMcc)
							&& mncCode.equals(Config.unBillMnc)) {
						grantedUnits =  billingInfo[2];
						resCode = Constants.SUCCESS_CODE;
					} else {
						balAfterInit = userBalance - billingInfo[0];
						String results[] = bill(userBalance, billingInfo[1],
								billingInfo[2], 0, 0, subscriptionID, 0);
						grantedUnits = Integer.valueOf(results[2]);
						resCode = Integer.valueOf(results[0]);
						balAfterInit = Integer.valueOf(results[1]);
					}
				} else {
					grantedUnits = (int) Constants.MINUSONE;
					resCode = (int) Constants.UKNOWNUSER_CODE;
				}
				
				avpset = par.copyforResponse(Constants.ONE, (int) resCode, hs);
				Avp a = new Avp(Dictionary.GRANTED_SERVICE_UNITS, (short) Constants.EIGHTY, Constants.ZERO, par.encodeAvp(new Avp(Dictionary.CC_TIME,(short) Constants.EIGHTY, Constants.ZERO, par.int32ToBytes((int) grantedUnits))));
				a = new Avp(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL, (short) Constants.EIGHTY, Constants.ZERO, par.encodeAvp(a));
				avpset.add(a);
				byte spack[] = par.merge(par.encodeAvpSet(avpset), header);
				if (resCode == Constants.SUCCESS_CODE) {
					params.put(Constants.SUBSCRIPTIONID, subscriptionID);
					params.put(Constants.SESSION_STATE, "1");
					params.put(Constants.TIME_PERIOD, "" + (int) billingInfo[2]);
					params.put(Constants.CHARGE_PER_PERIOD, "" + billingInfo[1]);
					params.put(Constants.REMAINING_BALANCE, "" + balAfterInit);
					params.put(Constants.ANUMBER, senderNumber);
					params.put(Constants.BNUMBER, "" + receiverNumber);
					params.put(Constants.MCC, "" + mccCode);
					params.put(Constants.MNC, "" + mncCode);
					params.put(Constants.GRANTED_UNITS, "" + grantedUnits);
					params.put(Constants.INITIAL_CHARGE, "" + billingInfo[0]);
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					params.put(Constants.START_TIME,sdf.format(new Date(System.currentTimeMillis())));
					params.put(Constants.LASTACCESSTIME, "" + System.currentTimeMillis());
					synchronized(sessions){
						sessions.put(sessionId, params);
					};
					handleFault(sessionId, params.toString());
				}
				log.info("Result for CCRI "+senderNumber+"----->"+receiverNumber+" is "+resCode);
				log.debug("CCRI Request Processing is done and CCA Sent");
				return spack;
			}
			else{
				return handleError(validity,hs,header);

			}

		}
		
		/**
		 * 
		 * @param errorType
		 * @param hs
		 * @param temp
		 * @return
		 * @throws IOException
		 * 
		 * It generates response for all the errors. It receives its header and error code. Based on this It generates the error response and gives back.
		 */

		public byte[] handleError(int errorType,Hashtable<Integer, Avp>hs, byte[] temp) throws IOException{
			HashSet<Avp> avpset = null;
			switch(errorType){
			case -1:
				avpset = par.copyforResponse(Constants.ONE,Constants.UNKNOWNREALM_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);				
			case -2:
				avpset = par.copyforResponse(Constants.ONE,Constants.UNKNOWNAUTHID_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);					
			case -4:
				avpset = par.copyforResponse(Constants.ONE,Constants.SESSIONEXITS_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);					
			case -5:
				avpset = par.copyforResponse(Constants.ONE,Constants.UNKNOWN_SESSION_CODE, hs);
				return par.merge(par.encodeAvpSet(avpset), temp);					
			default:
				return null;

			}
		}

		
		/**
		 * It process the Update Requests , it extracts Sender No, Receiver No, Mcc Code, Mnc Code from the stored sessions, tariff rates and
		 *  before calculating the units to allocate it updates it balance with comparing reserved units and used units  then allocates units to requests and generate response.
		 *   Before returning the response it store all the information to session.		 * 
		 *  
		 */
		@Override
		public byte[] processCCRU(byte[] b) throws IOException, NoSpaceException,SQLException,ArithmeticException {
			log.debug("Processing CCRU Request");
			HashSet<Avp> avpset = new HashSet<Avp>();
			byte temp[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
			Hashtable<Integer, Avp> hs = par.decodeAvpSet(b, Constants.TWENTY);
			int validity=par.checkValid(hs, uri);
			if (validity>Constants.ZERO) {
				log.debug("Processing Valid CCRU Request");
				String sessionid = ("" + par
						.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData)) + uri;
				
				Hashtable<String, String> params = null;
				synchronized (sessions) {
					params=sessions.get(sessionid);	
				}
				String mncCode = params.get(Constants.MCC), mccCode = params
						.get(Constants.MNC);
				;
				
				int balance = Integer.valueOf(params
						.remove(Constants.REMAINING_BALANCE));
				int reservedUnit = Integer.valueOf(params
						.remove(Constants.RESERVED_BALANCE));
				int chargePerUnit = Integer.valueOf(params
						.get(Constants.CHARGE_PER_PERIOD));
				int grantedUnits = Integer.valueOf(params
						.get(Constants.GRANTED_UNITS));
				int timePeriod = Integer.valueOf(params
						.get(Constants.TIME_PERIOD));
				int rescode;

				int wrresUnits = (int) par.bytesToInt((par.decodeAvpSet((par
						.decodeAvpSet(hs.get(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL).getData(), 0)).get(Dictionary.USED_SERVICE_UNIT)
						.getData(), 0)).get(Dictionary.CC_TIME).getData());

				if (mccCode.equals(Config.unBillMcc)
						&& mncCode.equals(Config.unBillMnc)) {
					grantedUnits = timePeriod;
					rescode = Constants.SUCCESS_CODE;
				} else {
					String results[] = bill(balance, chargePerUnit, timePeriod,
							(int) grantedUnits, (int) wrresUnits,
							params.get(Constants.SUBSCRIPTIONID), 0);
					grantedUnits = Integer.parseInt(results[2]);
					rescode = Integer.parseInt(results[0]);
					if (rescode != Constants.SUCCESS_CODE) {
						sessions.remove(sessionid);
					}
					//DecimalFormat df = new DecimalFormat("#.##");
					balance = Integer.valueOf(results[1]);
					reservedUnit += Integer.valueOf(results[3]);
				}
				params.put(Constants.RESERVED_BALANCE, "" + reservedUnit);
				params.put(Constants.REMAINING_BALANCE, "" + balance);
				params.put(Constants.GRANTED_UNITS, "" + grantedUnits);
				params.put(Constants.LASTACCESSTIME, "" + System.currentTimeMillis());
				avpset = par.copyforResponse(1, (int) rescode, hs);
				Avp a = new Avp(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL, (short) 0, 0, par.encodeAvp(new Avp(Dictionary.GRANTED_SERVICE_UNITS,
						(short) 0, 0, par.encodeAvp(new Avp(Dictionary.CC_TIME, (short) 0, 0,
								par.int32ToBytes((int) grantedUnits))))));
				avpset.add(a);
				synchronized (sessions) {
					sessions.put(sessionid, params);
				}
				byte spack[] = par.merge(par.encodeAvpSet(avpset), temp);
				handleFault(sessionid, params.toString());
				log.info("Result for CCRU "+params.get(Constants.ANUMBER)+"----->"+params.get(Constants.BNUMBER)+" is "+rescode);
				log.debug("Processed CCRU Request and generated CCA");
				return spack;
			}
			else{
				return handleError(validity,hs,temp);
			}

		}

		
		/**
		 * It process the Terminate Requests , it extracts Sender No,Reserved Units,Charge Per Block, Time Per BLock from session and Used units from its bytes
		 * and updates balance corresponding to sender no and calculates response.
		 * Before returning the response it store all the information to session.		  
		 *  
		 */
		@Override
		public byte[] processCCRT(byte[] b) throws IOException, NoSpaceException,SQLException,ArithmeticException {
			log.debug("Processing CCRT Request");
			HashSet<Avp> avpset = null;
			byte temp[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
			Hashtable<Integer, Avp> hs = par.decodeAvpSet(b, Constants.TWENTY);
			int validity=par.checkValid(hs, uri);
			if (validity>Constants.ZERO) {
				int finalbal = Constants.ZERO;
				String sessionid = par.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData)
						+ uri;
				Hashtable<String, String> params=null;
				synchronized(sessions){
					params= sessions.get(sessionid);
				};

				int wrresUnits = (int) par.bytesToInt((par.decodeAvpSet((par
						.decodeAvpSet(hs.get(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL).getData(), 0)).get(Dictionary.USED_SERVICE_UNIT)
						.getData(), 0)).get(Dictionary.CC_TIME).getData());
				
				int balance = Integer.valueOf(params.get(Constants.REMAINING_BALANCE));
				int reservedUnit = Integer.valueOf(params.get(Constants.RESERVED_BALANCE));
				int getUnitPerPerios = Integer.valueOf(params
						.get(Constants.TIME_PERIOD));

				if (wrresUnits > Constants.ZERO) {
					int intialAmountBuff = Integer.valueOf(params
							.get(Constants.INITIAL_CHARGE));
					String respons[]=bill(balance, Integer.valueOf(params.get(Constants.CHARGE_PER_PERIOD)),getUnitPerPerios, Integer.parseInt(params
							.get(Constants.GRANTED_UNITS)), wrresUnits, params.get(Constants.SUBSCRIPTIONID), 1);
					if (params.get(Constants.MCC).equals(Config.unBillMcc)
							&& params.get(Constants.MNC).equals(
									Config.unBillMnc)) {
						balance = reservedUnit;
					} else {
						balance = Integer.valueOf(respons[1]) - intialAmountBuff;
						finalbal = reservedUnit - balance;
						//finalbal = Integer.valueOf(finalbal);
						
					}
				} else
					finalbal = reservedUnit;
				boolean result = db.updateUserBalance(params.get(Constants.SUBSCRIPTIONID), reservedUnit, finalbal);
				int resCode = Constants.SUCCESS_CODE;
				if (!result)
					resCode = Constants.UNKNOWN_ERROR_CODE;
				avpset = par.copyforResponse(Constants.ONE, resCode, hs);
				Avp a = new Avp(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL, (short) 0, 0, par.encodeAvp(new Avp(Dictionary.GRANTED_SERVICE_UNITS,
						(short) 0, 0, par.encodeAvp(new Avp(Dictionary.CC_TIME, (short) 0, 0,
								par.int32ToBytes(-1))))));
				avpset.add(a);
				byte spack[] = par.merge(par.encodeAvpSet(avpset), temp);
				synchronized(sessions){
					sessions.remove(sessionid);
				};
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				HashMap<String,String> cdrData = new HashMap<String, String>();
				cdrData.put(Constants.SRC_USERNAME, params.get(Constants.ANUMBER));
				cdrData.put(Constants.SRC_HOSTNAME, "truphone");
				cdrData.put(Constants.BNUMBER, params.get(Constants.BNUMBER));
				cdrData.put(Constants.START_TIME, params.get(Constants.START_TIME));
				cdrData.put(Constants.END_TIME,sdf.format(new Date(System.currentTimeMillis())));
				cdrData.put(Constants.ACCOUNT,"abcdef");
				cdrData.put(Constants.AMOUNTCONSUMED,"" + finalbal);
				cdrData.put(Constants.RATECARD,"" + 1);
				cdrData.put(Constants.INITIAL_CHARGE,params.get(Constants.INITIAL_CHARGE));
				cdrData.put(Constants.CHARGEMODE,"" + 1);
//				/try{
				System.out.println(params.get(Constants.CHARGE_PER_PERIOD));
				long consumedtime  = (long)(Integer.valueOf(finalbal)/Integer.valueOf(params.get(Constants.CHARGE_PER_PERIOD))) * Long.valueOf(params.get(Constants.TIME_PERIOD));
				cdrData.put(Constants.TIMECONSUMED, ""+consumedtime);
				
				//cdrData.put(Constants.TIMECONSUMED, ""+consumedtime);
				boolean loc=false;
				try {
					loc=cDRQueue.getLock();
					while(!loc){
						loc=cDRQueue.getLock();
						delay(1);
					}
					if(loc)
						cDRQueue.add(cdrData);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				finally {
					if(loc){
						cDRQueue.lock.unlock();
						delay(1);
					}
				}
				
				return spack;
			}
			else
				return handleError(validity,hs,temp);
		}


		/**
		 * 
		 * It returns the response for Credit Control Request Event by processing from bytes. 
		 * It separates the header from bytes and process the request from it and Sends response.   
		 * 
		 */
		@Override
		public byte[] processCCRE(byte[] b) throws IOException, NoSpaceException,SQLException,ArithmeticException {
			byte temp[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
			HashSet<Avp> avpset = null;
			Hashtable<Integer, Avp> hs = par.decodeAvpSet(b, Constants.TWENTY);
			int validity=par.checkValid(hs, uri);
			if(validity>0){
				String senderNumber = par.digNumber(par.bytesToOctetString((par.decodeAvpSet(par.decodeAvpSet(par.decodeAvpSet(hs.get(Dictionary.SERVICE_INFORMATION).getData(), 0).get(877).getData(), 0).get(886).rawData,0).get(897).rawData)));
				String receiverNumber = par.digNumber(par.bytesToOctetString((par.decodeAvpSet(par.decodeAvpSet(par.decodeAvpSet(hs.get(Dictionary.SERVICE_INFORMATION).getData(), 0).get(877).getData(), 0).get(1201).rawData,0).get(897).rawData)));
				int userBalance = db.readUserBalance(senderNumber);
				int chargeAmount = db.getEventBillingUnits(senderNumber,
						receiverNumber);
				int getUnittoMess = Constants.MINUSONE;
				int resCode = Constants.NOBALANCE_CODE;
				if (userBalance > Constants.ZERO) {
					if (!(userBalance <= Constants.ZERO || userBalance < chargeAmount)) {
						userBalance = userBalance - chargeAmount;
						boolean balUpd = db.updateUserBalance(senderNumber,
								0, userBalance);
						if (balUpd) {
							getUnittoMess = Constants.ONE;
							resCode = Constants.SUCCESS_CODE;
						} else {
							resCode = Constants.UNKNOWN_ERROR_CODE;
						}
					}
				}
				System.out.println("Message request");
				avpset = par.copyforResponse(1, resCode, hs);
				Avp a = new Avp(Dictionary.GRANTED_SERVICE_UNITS, (short) Constants.EIGHTY, Constants.ZERO, par.encodeAvp(new Avp(Dictionary.CC_TIME,
						(short) Constants.EIGHTY, Constants.ZERO, par.int32ToBytes(getUnittoMess))));
				a = new Avp(Dictionary.MULTIPLE_SERVICES_CREDIT_CONTROL, (short) Constants.EIGHTY, Constants.ZERO, par.encodeAvp(a));
				avpset.add(a);
				byte spack[] = par.merge(par.encodeAvpSet(avpset), temp);
				log.info("Result for CCRE "+senderNumber+"----->"+receiverNumber+" is "+resCode +" and total charge is "+userBalance);
				log.debug("Processed CCRE Request");
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				HashMap<String,String> cdrData = new HashMap<String, String>();
				cdrData.put(Constants.SRC_USERNAME, senderNumber);
				cdrData.put(Constants.SRC_HOSTNAME, "truphone");
				cdrData.put(Constants.BNUMBER, receiverNumber);
				cdrData.put(Constants.START_TIME, sdf.format(new Date(System.currentTimeMillis())));
				cdrData.put(Constants.END_TIME, sdf.format(new Date(System.currentTimeMillis())));
				cdrData.put(Constants.ACCOUNT, "abcdef");
				cdrData.put(Constants.AMOUNTCONSUMED, "" + chargeAmount);
				cdrData.put(Constants.RATECARD, "" + 1);
				cdrData.put(Constants.INITIAL_CHARGE, "" + 0);
				cdrData.put(Constants.CHARGEMODE, "" + 2);
				cdrData.put(Constants.TIMECONSUMED, ""+ 1);
				boolean loc=false;
				try {
					loc=cDRQueue.getLock();
					while(!loc){
						loc=cDRQueue.getLock();
						delay(1);
					}
					if(loc)
						cDRQueue.add(cdrData);
				}
				catch(Exception e){
					e.printStackTrace();
				}
				finally{
					if(loc){
						cDRQueue.lock.unlock();
						delay(1);
					}
				}	
				return spack;
			}else{
				return handleError(validity,hs,temp);
			}
		}

		/**
		 * 
		 * @param balance
		 * @param chargperblock
		 * @param timeperBlock
		 * @param wereReserved
		 * @param usedUnits
		 * @param Anum
		 * @param type
		 * @return
		 * @throws SQLException
		 * 
		 * It reserves units for all initial and Update request's to the user based on his balance , reserved and used units. 
		 * It returns back corresponding units of time allocated and result code in the array.
		 */
		public String[] bill(int balance, int chargperblock,
				int timeperBlock, int wereReserved, int usedUnits, String Anum, int type) throws SQLException{
			int unittoAll = Constants.MINUSONE;
			int resCode = Constants.ZERO;
			if (wereReserved > Constants.ZERO) {
				
				int wereUsed = Constants.ZERO;
				if (wereReserved >= Constants.ZERO)
					wereUsed = usedUnits;
				int remaining = wereReserved - wereUsed;
				int leftBal = Constants.ZERO;
				if (remaining > Constants.ZERO)
					leftBal =  (chargperblock  * (remaining/timeperBlock));
				balance += leftBal;
				if(type==1){
					return new String[]{""+resCode,""+balance};
				}
			}

			int reservedUnit = Constants.ZERO;
			if (balance == Constants.ZERO) {
				balance = db.readUserBalance(Anum);
				reservedUnit = reservedUnit + balance;
			}
			if (balance > Constants.ZERO) {
				if (timeperBlock > Constants.ONE) {
					unittoAll = (int) ((((int) (chargperblock / balance) == Constants.ZERO) ? Constants.ONE
							: (chargperblock / balance)) * timeperBlock);
					balance -= chargperblock;
					resCode = Constants.SUCCESS_CODE;
				} else {
					resCode = Constants.NOBALANCE_CODE;
				}

			} else if (balance ==Constants.ZERO){
				resCode = Constants.NOBALANCE_CODE;
			}else{
				resCode =Constants.UKNOWNUSER_CODE;
			}

			return new String[] { "" + resCode, "" + balance, "" + unittoAll,
					"" + reservedUnit };

		}


		/**
		 * 
		 * @param sessionId
		 * @param phoneData
		 * @return
		 * @throws SQLException
		 * 
		 * It writes all the session information  periodically to Queue to use it in Fault Tolerance by getting the lock of queue. 
		 * once it writes the info to queue it release the lock. 
		 */
		public boolean handleFault(String sessionId, String phoneData) throws SQLException{
			if (phoneData != null){
				boolean loc=Constants.FL_VAL;
				try{
					while(!loc)
						loc=faultHandlerQueue.getLock();
					if(loc)
						faultHandlerQueue.add(sessionId+"~"+phoneData);
				}
				catch(Exception e){}
				finally{
					if(loc)
						faultHandlerQueue.lock.unlock();
				}
				return Constants.TR_VAL;
			}
			else {
				Hashtable<String, String> loadedSession = db
						.loadSessionData(sessionId);
				if (loadedSession != null && !loadedSession.isEmpty()) {
					synchronized(sessions){
						sessions.put(sessionId, loadedSession);
					};

					return Constants.TR_VAL;
				}
				return Constants.FL_VAL;
			}

		}

		
		/**
		 * 
		 * 
		 * It Returns responses for billing requests, It Handles four types of requests CCRI(Credit Control Request Initial),
		 * 	CCRU(Credit Control Request Update), CCRT(Credit Control Request Terminate), CCRE(Credit Control Request Event).
		 * For all the above requests it checks for valid session and process the respone Correspondingly.
		 * 
		 */
		@Override
		public byte[] processBillingRequest(byte[] b) throws IOException, NoSpaceException,SQLException,ArithmeticException {
			byte temp[] = Arrays.copyOfRange(b, Constants.ZERO, Constants.TWENTY);
			//HashSet<Avp> avpset = new HashSet<Avp>();
			Hashtable<Integer, Avp> hs = null;
			hs = par.decodeAvpSet(b, Constants.TWENTY);
			int state = Constants.ZERO;
			state = par.bytesToInt(hs.get(Dictionary.CC_REQUEST_TYPE).rawData);
			boolean existSession, loadedSession = true;
			String sessionId = null;
			switch (state) {
			case 1:
				log.debug("Got CCRI Request");
				sessionId = ("" + par.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData))+ uri;


				synchronized (sessions) {

					existSession = sessions.containsKey(sessionId);
					if(!existSession){
						sessions.put(sessionId, new Hashtable<String, String>());	
					}
				}
				if (existSession) {
					return handleError(-4,hs,temp);
				}

				return processCCRI(b);
			case 2:
				log.debug("Got CCRU Request");
				sessionId = ("" + par.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData))
						+ uri;
				existSession = sessions.containsKey(sessionId);

				if (!existSession)
					loadedSession = handleFault(sessionId, null);
				if (loadedSession || existSession)
					return processCCRU(b);
				return handleError(-5,hs,temp);
			case 3:
				log.debug("Got CCRT Request");
				sessionId = ("" + par.bytesToOctetString(hs.get(Dictionary.SESSION_ID).rawData))
						+ uri;
				existSession = sessions.containsKey(sessionId);
				if (!existSession)
					loadedSession = handleFault(sessionId, null);
				if (loadedSession || existSession)
					return processCCRT(b);
				return handleError(-5,hs,temp);
			case 4:
				log.debug("Got CCRE Request");
				return processCCRE(b);

			}
			return null;
		}

	}
}
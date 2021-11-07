package com.shrewdify.diaroser.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.shrewdify.diaroser.api.Peer;
import com.shrewdify.diaroser.impl.PeerImpl;
import com.shrewdify.diaroser.utils.DbHelper;
import com.shrewdify.diaroser.utils.ElementParser;
import com.shrewdify.diaroser.utils.PeerTable;

/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */


public class ServerIO extends ElementParser {
	ServerSocket sc;
	Logger log=Logger.getLogger(getClass());
	/**
	 * 
	 *Returns an Instance of Server IO by loading values from configuration file and Starts Server listening  on 
	 * port obtained from configuration's
	 * based on count it generates instances for each request from the client which are Accepted by server.
	 * @param configFile
	 */
	public ServerIO(String configFile) {
		try {
			log.info("<---------------Starting Diameter Server-------------->");
			InputStream is = DbHelper.class.getClassLoader() .getResourceAsStream(configFile);
			Properties properties = new Properties();
			if (is != null) {
				properties.load(is);
				String dbhost = (String) properties.getProperty("dbhost");
				String dbname = (String) properties.getProperty("dbname");
				String dbuser = (String) properties.getProperty("dbuser");
				String dbcdr = (String) properties.getProperty("dbcdr");
				String dbpassword = (String) properties.getProperty("dbpassword");
				int numberOfWorkers = Integer.parseInt(properties.getProperty("concurrency"));
				int cleaningDelay = Integer.parseInt(properties.getProperty("cleaningtime"));
				String className = (String) properties.getProperty("classname");
				//log.debug("Trying to connect to db with details \n\tDB Host:" + dbhost + "\n\tDB Name:" + dbname + "\n\tDB User:"+ dbuser + "\n\tDB Password:" + dbpassword);
				sc = new ServerSocket(Integer.parseInt(properties.getProperty("port")));
				///log.debug("Database Initialization Successful");
				PeerTable.peerTable = new Hashtable<String, Peer>();
				while (true) {
					Socket c = sc.accept();
					try{
					Peer p = new PeerImpl(c,cleaningDelay,numberOfWorkers,className, dbhost, dbname, dbuser, dbpassword,dbcdr);
					if (p.isPeered) {
						PeerTable.peerTable.put(p.uri,p);
						Thread t = new Thread(p);
						t.start();
					}
					}
					catch(Exception e){
						log.error("Got error:",e);
					}

				}
			}
		} catch (IOException e) {
			log.error("Error in ServerIO", e);
		}
	}

	
	/**
	 * 
	 * It Releases all the peers corresponding to this ServerIO Object by iterating through each value of Peer Table.
	 * 
	 * Finally the Server socket is closed.
	 */
	public void stopServer() {
		try {
			for (Peer peer : PeerTable.peerTable.values()) {
				peer.stop();
			}
			PeerTable.peerTable.clear();
			sc.close();
			sc = null;
			log.info("<----------------------------Stopped Diameter Server---------------------->");
		} catch (Exception e) {
			log.error("Error in ServerIO", e);
		}
	}

}

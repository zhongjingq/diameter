package com.shrewdify.diaroser.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */


public class DbHelper {

	Logger log=Logger.getLogger(DbHelper.class);
	public Connection connection = null;
	public PreparedStatement psgetBalance,psCdrRecords;
	public PreparedStatement psUpdBalance, psStaUpd2, psSta;
	public PreparedStatement psSta1, psSta3, psUpdUserFinaBal,
	psActiveCallLis, psStaUpd, psStaUpd1,psGetExpiredSessions;
	
	
	/**
	 * 
	 * @param className   // Contains the type of Driver to be loaded
	 * @param dbhost 	  // Contains host address of Data Base.
	 * @param dbname 	  // Contains DataBase Name 
	 * @param dbuser 	  // User Name Used to connect.
	 * @param dbpassword  // Password for the user to connect.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * 
	 * It initializes the data base connection With above parameters and creates all the prepared statements used for dbOperations.  
	 */
	
	public void init(String className, String dbhost, String dbname,
			String dbuser, String dbpassword) throws SQLException,ClassNotFoundException{
			Class.forName(className);
			connection = DriverManager.getConnection("jdbc:mysql://" + dbhost
					+ ":3306/" + dbname, dbuser, dbpassword);
			psSta3 = connection
					.prepareStatement("SELECT connectcharge,ratetariff,blockperiod FROM charging_data_rates ccr WHERE ? LIKE CONCAT(ccr.dialprefix,'%') and ccr.idtariffplan = (SELECT a.idtariffmapper from charging_plans a where a._id = (select chargingplanid from userinfos where userid=?)) order by length(ccr.dialprefix) desc limit 1");
			psSta = connection
					.prepareStatement("SELECT * FROM activecallsdata where SessionId =?");
			psStaUpd = connection
					.prepareStatement("update activecallsdata set phoneData=? where SessionId=?");
			psStaUpd1 = connection
					.prepareStatement("insert into activecallsdata(SessionId,PhoneData)values(?,?)");
			psStaUpd2 = connection
					.prepareStatement("delete from activecallsdata where LastModified <= ?");
			psgetBalance = connection
					.prepareStatement("select * from userinfos where userid = ?");
			psUpdBalance = connection
					.prepareStatement("update userinfos set ReservedUnits= ? where userid = ?");
			psUpdUserFinaBal = connection
					.prepareStatement("update userinfos set ReservedUnits=ReservedUnits- ?, Units=Units- ? where userid = ?");
			psSta1 = connection
					.prepareStatement("SELECT mprice FROM charging_data_rates ccr WHERE ? LIKE CONCAT(ccr.dialprefix,'%') and ccr.idtariffplan = (SELECT a.idtariffmapper from charging_plans a where a._id = (select chargingplanid from userinfos where userid=?)) order by length(ccr.dialprefix) desc limit 1");
			psActiveCallLis = connection
					.prepareStatement("SELECT * FROM activecallsdata WHERE sessionId =? ");
			psCdrRecords = connection.prepareStatement("INSERT INTO pp_cdrecords(dlgid, stepnumber,src_username,"
					+ "src_hostname, dest_addr, start_time,end_time,account, cost, consumed_time, charge_mode, "
					+ "rate_card, connect_charge) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
			psGetExpiredSessions= connection.prepareStatement("Select sessionid from activecallsdata where LastModified <= ?");
			
	}

	/**
	 * 
	 * @param tdsValues  // It Contains all the Values to be inserted in CDR's Table.
	 * @throws SQLException
	 */
	public void addCdrRecords(HashMap<String,String> tdsValues) throws SQLException{
		if(connection != null){
			psCdrRecords.setString(1, "cdrRecords");
			psCdrRecords.setInt(2, 1);
			psCdrRecords.setString(3, tdsValues.get(Constants.SRC_USERNAME));
			psCdrRecords.setString(4, tdsValues.get(Constants.SRC_HOSTNAME));
			psCdrRecords.setString(5, tdsValues.get(Constants.BNUMBER));
			psCdrRecords.setString(6, tdsValues.get(Constants.START_TIME));
			psCdrRecords.setString(7, tdsValues.get(Constants.END_TIME));
			psCdrRecords.setString(8, tdsValues.get(Constants.ACCOUNT));	
			psCdrRecords.setInt(9, Integer.valueOf(tdsValues.get(Constants.AMOUNTCONSUMED)));
			psCdrRecords.setLong(10, Long.valueOf(tdsValues.get(Constants.TIMECONSUMED)));
			psCdrRecords.setInt(11, Integer.parseInt(tdsValues.get(Constants.CHARGEMODE)));
			psCdrRecords.setInt(12, Integer.parseInt(tdsValues.get(Constants.RATECARD)));
			psCdrRecords.setInt(13, Integer.valueOf(tdsValues.get(Constants.INITIAL_CHARGE)));
			psCdrRecords.execute();
		
		}
	}
	
	/**
	 * 
	 * @param sessionId  // Session ID used to identify session uniquely
	 * @param phoneData // contains all the information of session to restore it in Failure.
	 * @return
	 * @throws SQLException
	 * 
	 * It Updates the Session Data based on Session id. 
	 * If session id already exists it updates it's value else it creates new records with those values.
	 */
	public  boolean setSeesionData(String sessionId, String phoneData) throws SQLException{
		if (connection != null) {
				psSta.setString(1, sessionId);
				ResultSet resultSet = psSta.executeQuery();
				if (resultSet.next()) {
					psStaUpd.setString(1, phoneData);
					psStaUpd.setString(2, sessionId);
					psStaUpd.executeUpdate();
					

				} else {
					psStaUpd1.setString(1, sessionId);
					psStaUpd1.setString(2, phoneData);
					psStaUpd1.executeUpdate();
				}
				resultSet.close();
				return Constants.TR_VAL;
					}
		return Constants.FL_VAL;
	}

	/**
	 * 
	 * @param lastAccessTime // Last accessed time.
	 * @return the Array of session Id removed from database.
	 * @throws SQLException
	 * 
	 * It removes all the session Values which are having last access time less then the LastAccessTime.
	 */
	public  String[] removeSession(long lastAccessTime) throws SQLException{
		List<String> list=new ArrayList<String>(); 
		if (connection != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date d=new Date(lastAccessTime);
			psGetExpiredSessions.setString(1, sdf.format(d));
			ResultSet resultSet = psGetExpiredSessions.executeQuery();
			while (resultSet.next()) 
				list.add(resultSet.getString(1));
			psStaUpd2.setString(1, sdf.format(d));
			psStaUpd2.executeUpdate();
			resultSet.close();
		}
		return list.toArray(new String[]{});
	}

	/**
	 * It closes the data base connection of the corresponding Object.
	 * @throws SQLException
	 */
	public  void close() throws SQLException{
		connection.close();
	}

	
	/**
	 * 
	 * @param username  //Account no of the user. 
	 * @return  //Returns balance allocated to User.
	 * 
	 * @throws SQLException
	 * 
	 * It reads the user balance based on his name and allocates the buffer amount to the user.
	 */
	
	
	public  int readUserBalance(String username) throws SQLException{
		int balance = Constants.MINUSONE;
		int reservedUnits = Constants.ZERO;
		int reservedBuff = 5000;
		if (connection != null) {
			psgetBalance.setString(1, username);
			boolean userexist = Constants.FL_VAL;
			ResultSet resultSet = psgetBalance.executeQuery();
			while (resultSet.next()) { // retrieve data
				balance =  resultSet.getInt(3);
				reservedUnits = resultSet.getInt(4);
				userexist = true;
			}

			if ((reservedUnits + reservedBuff) <= balance) {
				reservedUnits = reservedUnits + reservedBuff;
				psUpdBalance.setInt(1,  reservedUnits);
				psUpdBalance.setString(2, username);
				int rowsUpdates = psUpdBalance.executeUpdate();

				if (rowsUpdates == Constants.ONE)
					balance = reservedBuff;
			} else if (userexist) {
				balance = Constants.ZERO;
			} else {

				balance = Constants.MINUSONE;
			}
			resultSet.close();
		}
		return balance;

	}

	
	/**
	 * 
	 * @param username //account name of the user.
	 * @param reservedbalance // Total balance Reserved for user.
	 * @param balance //Total amount used from the balance.
	 * @return
	 * @throws SQLException
	 * 
	 * Update user account with final balance after the complete transaction.
	 */
	public  boolean updateUserBalance(String username,
			int reservedbalance, int balance) throws SQLException{
		boolean data = Constants.FL_VAL;
		if (connection != null) {
			// float reservedUnits = 0.0f;
			psUpdUserFinaBal.setInt(1, reservedbalance);
			psUpdUserFinaBal.setInt(2, balance);
			psUpdUserFinaBal.setString(3, username);
			int rowsUpdates = psUpdUserFinaBal.executeUpdate();
			if (rowsUpdates == 1)
				data = Constants.TR_VAL;
		}

		return data;

	}

	/**
	 * 
	 * @param callingPartyNumber //Contains the Caller Phone No.
	 * @param calledPartyNumber //Contains the Receiver Phone No.
	 * @return
	 * @throws SQLException
	 * It gets call the billing details from data base based on the profile.
	 * 
	 */
	public  int[] getSessionBillingUnits(String callingPartyNumber,
			String calledPartyNumber) throws SQLException{
		int[] info = new int[3];
		
		if (connection != null) {
			psSta3.setString(1, calledPartyNumber);
			psSta3.setString(2, callingPartyNumber);
			ResultSet resultSet = psSta3.executeQuery();
			if (resultSet.next()) {
			 
				info[0] = (int)(resultSet.getDouble(1) * 1000);
				info[1] = (int)(resultSet.getDouble(2) * 1000);
				info[2] = resultSet.getInt(3);
			}
			resultSet.close();
		}
		return info;
	}
	
	/**
	 * 
	 * @param callingPartyNumber //Contains the Caller Phone No.
	 * @param calledPartyNumber //Contains the Receiver Phone No.
	 * @return
	 * @throws SQLException
	 * It gets the Message billing details from data base based on the profile.
	 * 
	 */

	public  int getEventBillingUnits(String callingPartyNumber,
			String calledPartyNumber) throws SQLException{
		int messCharge = Constants.ZERO;
		if (connection != null) {
			psSta1.setString(1, calledPartyNumber);
			psSta1.setString(2, callingPartyNumber);
			ResultSet resultSet = psSta1.executeQuery();
			while (resultSet.next()) 
				messCharge = (int)(resultSet.getDouble(1) * 1000);
			resultSet.close();
		} 
		return messCharge;
	}

	/**
	 * 
	 * @param sessionId //SessionId of the session to be loaded
	 * @return
	 * @throws SQLException
	 * 
	 * It checks the entry for corresponding session in Db and loads them back to Hash stable and returns it.
	 */
	public  Hashtable<String, String> loadSessionData(String sessionId) throws SQLException{
		Hashtable<String, String> phoneData = new Hashtable<String, String>();
		if (connection != null) {
			psActiveCallLis.setString(1, sessionId);
			ResultSet resultSet = psActiveCallLis.executeQuery();
			while (resultSet.next()) {
				Pattern p = Pattern.compile("[\\{\\}\\=\\, ]");
				String[] phonData = p.split(resultSet.getString(3));
				for (int i = 1; i < phonData.length; i += 3) {
					phoneData.put(phonData[i], phonData[i + 1]);
				}

			}
			resultSet.close();
		}

		return phoneData;

	}
	
	
}

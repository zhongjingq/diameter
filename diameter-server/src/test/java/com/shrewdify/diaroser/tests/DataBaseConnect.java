package com.shrewdify.diaroser.tests;

import com.mysql.jdbc.PreparedStatement;
import com.shrewdify.diaroser.utils.DbHelper;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;

public class DataBaseConnect extends TestCase{

	
	DbHelper db=null;


	@Override
	protected void setUp() throws SQLException,ClassNotFoundException {
		InputStream is = DataBaseConnect.class.getClassLoader()
				.getResourceAsStream("accounts.properties");
		if(is != null){
			Properties properties = new Properties();
			try {
				properties.load(is);
		 	} catch (IOException e) {
				e.printStackTrace();
			}
			
			String dbhost = (String) properties.getProperty("dbhost");
			String dbname = (String) properties.getProperty("dbname");
			String dbuser = (String) properties.getProperty("dbuser");
			String dbpassword = (String) properties
					.getProperty("dbpassword");
			String className = (String) properties.getProperty("classname");
			db=new DbHelper();
			db.init(className,dbhost, dbname, dbuser, dbpassword);
			PreparedStatement pds = (PreparedStatement) db.connection.prepareStatement("delete from userinfos"
					+ " where userid='99999'");
			
			PreparedStatement pds2 = (PreparedStatement) db.connection.prepareStatement("delete from userinfos"
					+ " where userid='99998'");
			
			PreparedStatement pdsDelPlan = (PreparedStatement) db.connection.prepareStatement("delete from charging_plans"
					+ " where _id=12345");
			
			PreparedStatement pdsDelCdaRates = (PreparedStatement) db.connection.prepareStatement("delete from charging_data_rates"
					+ " where dialprefix='18622429128'");
			
			PreparedStatement pdsSessionData  = (PreparedStatement)db.connection.prepareStatement("insert into activecallsdata(SessionId,PhoneData)values('abcd','123')");
			pdsSessionData.execute();
			pds.execute();
			pds2.execute();
			pdsDelPlan.execute();
			pdsDelCdaRates.execute();

			 pds = (PreparedStatement) db.connection.prepareStatement("insert into userinfos"
					+ "(userid,units,reservedunits,chargingplanid) values('99999','5000',0,12345)");
			
			pds2 = (PreparedStatement) db.connection.prepareStatement("insert into userinfos"
					+ "(userid,units,reservedunits,chargingplanid) values('99998','0',0,12345)");
			
			
			PreparedStatement pdsUpdatePlan = (PreparedStatement) db.connection.prepareStatement("insert into charging_plans"
					+ "(_id,tariffname,description,currency_code,idtariffmapper) values(12345,'Flat Test','testing','USD',27)");

			PreparedStatement pdsCdaRates = (PreparedStatement) db.connection.prepareStatement("INSERT INTO  charging_data_rates"
			+ "( idtariffplan , dialprefix , destination ,ratetariff,blockperiod,connectcharge,startdate,stopdate,starttime,endtime,dialPrefixLength,MPrice,MDataSize)"
			+"VALUES "
			+"( 27,  '18622429128',  'US Mobile',  0.03,  60,  0.25, CURRENT_TIMESTAMP ,  '2013-12-19 00:00:00',0, 10079, 0, 0.4,  160)");
		
			pds.execute();
			pds2.execute();
			pdsUpdatePlan.execute();
			pdsCdaRates.execute();
			
		}

	}


	@Override
	protected void tearDown() throws Exception{
		PreparedStatement pds = (PreparedStatement) db.connection.prepareStatement("delete from userinfos"
				+ " where userid='99999'");
		
		PreparedStatement pds2 = (PreparedStatement) db.connection.prepareStatement("delete from userinfos"
				+ " where userid='99998'");
		
		PreparedStatement pdsDelPlan = (PreparedStatement) db.connection.prepareStatement("delete from charging_plans"
				+ " where _id=12345");
		
		PreparedStatement pdsDelCdaRates = (PreparedStatement) db.connection.prepareStatement("delete from charging_data_rates"
				+ " where dialprefix='18622429128'");
		PreparedStatement pdsDelSession = (PreparedStatement) db.connection.prepareStatement("delete from activecallsdata where Sessionid='abcd'");
	pdsDelSession.execute();
		pds.execute();
		pds2.execute();
		pdsDelPlan.execute();
		pdsDelCdaRates.execute();
		db.close();
	}
	
	@Test
	public void testDbOperations(){
		try{
			testReadUserBalance();
			testAmountToCharge();
			testMessCharg();
			testSetSessionData();
			testLoadSessionData();
			testUpdateUserBal();
			testReadUserBalanceZero();
		}
		catch(Exception e){
			System.out.println("Error:"+e.getMessage());
		}
	}

	public void testReadUserBalance() throws SQLException{
		assertEquals("Balance Obtained", 5000,db.readUserBalance("99999"));
	}

	public void testAmountToCharge() throws SQLException{
		Assert.assertArrayEquals("Received Right Values", new int[]{250,30,60},db.getSessionBillingUnits("99999",
				"18622429128"));
	}
	
	
	public void testMessCharg() throws SQLException{
		assertEquals("Received Right Values", 400,db.getEventBillingUnits("99999",
				"18622429128"));
		;
	}
	
	public void testUpdateUserBal() throws SQLException{
		assertEquals("User BAlance updated", true, db.updateUserBalance("99999", 0, 5));
	}
	
	public void testReadUserBalanceZero() throws SQLException{
		db.updateUserBalance("99999", 0, 5);
		assertEquals("User BAlance updated", 0, db.readUserBalance("99999"));
	}
	
	public void testSetSessionData() throws SQLException{
		assertEquals("User Session Setting", true, db.setSeesionData("99999", "{a=1, b=2, c=3}"));
		assertEquals("User Session updated", true, db.setSeesionData("99999", "{a=1, b=2, c=3}"));
	}
	
	public void testLoadSessionData() throws SQLException{
		Hashtable<String, String> ht=new Hashtable<String,String>();
		ht.put("a", "1");
		ht.put("b", "2");
		ht.put("c", "3");
		assertEquals("User Session updated", true, db.setSeesionData("99999", "{a=1, b=2, c=3}"));
		assertEquals("Checking  session loading Values", ht,db.loadSessionData("99999"));
	}
	
	public void testReadUserBalanceNotExist() throws SQLException{
		assertEquals("User BAlance updated", -1, db.readUserBalance("99995"));
	}
	
	public void testRemoveSessionId() throws SQLException{
		String bal[] =db.removeSession(System.currentTimeMillis());
		System.out.println(Arrays.toString(bal));
		assertEquals("User BAlance updated", new String[]{"abcd"}, bal);
	}


}

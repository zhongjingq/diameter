package com.shrewdify.diaroser.tests;

import com.mysql.jdbc.PreparedStatement;
import com.shrewdify.diaroser.utils.DbHelper;
import com.shrewdify.diaroser.utils.ElementParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class ElementParserTEst extends TestCase{


	String className = null;
	String dbhost = null;
	String dbname = null;
	String dbuser = null;
	String dbpassword = null;
	DbHelper db;
	ElementParser par=new ElementParser();
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
			dbhost = (String) properties.getProperty("dbhost");
			dbname = (String) properties.getProperty("dbname");
			dbuser = (String) properties.getProperty("dbuser");
			dbpassword = (String) properties
					.getProperty("dbpassword");
			className = (String) properties.getProperty("classname");
			db=new DbHelper();
			db.init(className,dbhost, dbname, dbuser, dbpassword);
			PreparedStatement pds = (PreparedStatement) db.connection.prepareStatement("insert into userinfos"
					+ "(userid,units,reservedunits,chargingplanid) values('99999','5',0,12345)");

			PreparedStatement pds2 = (PreparedStatement) db.connection.prepareStatement("insert into userinfos"
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

		pds.execute();
		pds2.execute();
		pdsDelPlan.execute();
		pdsDelCdaRates.execute();
		db.close();
	}


	@Test
	public void testMccMnc() throws SQLException,ClassNotFoundException{
		byte a[] = new byte[]{0,98,(byte)248,48,0};
		Assert.assertArrayEquals(new String[]{"268","03"},par.mcc_mnc(a));
	}
	
	@Test
	public void testMergeArrays() {
		byte a1[] = new byte[5];
		a1[0] = 2;
		a1[1] = 15;
		a1[2] = 10;
		byte a2[] = new byte[5];
		a2[0] = 1;
		a2[1] = 48;
		a2[2] = 80;
		Assert.assertArrayEquals(new byte[]{ 1, 0, 0, 10, 0, 2, 15, 10, 0, 0 },par.merge(a1, a2));
	}

}

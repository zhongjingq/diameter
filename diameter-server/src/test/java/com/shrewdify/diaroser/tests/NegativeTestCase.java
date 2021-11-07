package com.shrewdify.diaroser.tests;

import com.shrewdify.diaroser.utils.DbHelper;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

public class NegativeTestCase extends TestCase {


	DbHelper db=null;
	String dbhost,dbname ,dbuser ,dbpassword,className;
	@Override
	protected void setUp() throws Exception {
		InputStream is = getClass().getClassLoader()
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
			db.init(className, dbhost, dbname, dbuser, dbpassword);
			
		}

	}


	@Test
	public void testWrongClassName() throws SQLException{
		try{
			db.init(className+"1313", dbhost, dbname, dbuser, dbpassword);
			assertFalse(true);	
		}
		catch(ClassNotFoundException e){
			assertFalse(false);	
		}
	}
	
	@Test
	public void testUnknowCreds() throws ClassNotFoundException{
		try{
		db.init(className, dbhost+"kkkk", dbname, dbuser, dbpassword);
		assertFalse(true);	
		}catch(SQLException e){
			assertFalse(false);
		}
	}

	@Test
	public void testUnknownUser() throws ClassNotFoundException,SQLException{
		assertEquals("Balance checking", -1.0,db.readUserBalance("999991"));
		assertEquals("Balnace Update", false,db.updateUserBalance("999991", 5, 0));
	}

}

package com.shrewdify.diaroser.tests;

import com.mysql.jdbc.PreparedStatement;
import com.shrewdify.diaroser.impl.PeerImpl;
import com.shrewdify.diaroser.utils.Avp;
import com.shrewdify.diaroser.utils.DbHelper;
import com.shrewdify.diaroser.utils.ElementParser;
import com.shrewdify.diaroser.utils.NoSpaceException;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;

public class PeerImplTest extends TestCase {

	//ServerIO sddat = new ServerIO("accounts.properties");
	ElementParser par = new ElementParser(); 

	String aNumber= "99999";
	String bNumber="18622429128";
	String sessionId="test_"+(Math.random()*1000)+System.currentTimeMillis();
	String service_context = "ngin.28458.000.000.9.32260@3gpp.org";

	String className = null;
	String dbhost = null;
	String dbname = null;
	String dbuser = null;
	String dbpassword = null;
	DbHelper db;

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


	@Before
	public void testUserBalanceZero() throws SQLException,ClassNotFoundException{
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		Assert.assertArrayEquals(new String[]{"4012","0","-1","0"},w.bill(0, 2, 60, 0, 0, "dcpip", 0));
	}


	@Test
	public void testAllocatedAmount() throws SQLException,ClassNotFoundException{
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		Assert.assertArrayEquals(new String[]{"2001","3","60","0"},w.bill(5, 2, 60, 0, 0, "12019931674", 0));
	}

	@Test
	public void testUserBalanceEmpty() throws SQLException,ClassNotFoundException{
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		String[] resCodes = w.bill(0, 2, 60, 0, 0, "12019931234", 0);
		boolean data = false;
	//System.out.println(Arrays.toString(resCodes));
		if (resCodes[0].equals("4012") && resCodes[1].equals("0")
				&& resCodes[2].equals("-1") && resCodes[3].equals("0"))
			data = true;
		assertEquals("Balnace Update", true, data);
	}


	@Test
	public void test6() throws SQLException,ClassNotFoundException{

		
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);

		String[] resCodes = w.bill(0, 2, 1, 0, 0, "12019931235", 0);
		boolean data = false;
//		/System.out.println(Arrays.toString(resCodes));
		if (resCodes[0].equals("5030") && resCodes[1].equals("-1") && resCodes[2].equals("-1") && resCodes[3].equals("-1"))
			data = true;
		assertEquals("Balnace Update", true, data);
	}

	@Test
	public void testAllocatedBAlance() throws SQLException,ClassNotFoundException {
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		Assert.assertArrayEquals(new String[]{"2001","3","60","0"}, w.bill(5, 2, 60, 0, 5, "99999", 0));
	}

	public HashSet<Avp> createSet(){
		String uri = "myserveruri";
		String realm = "truphone";
		HashSet<Avp> avpset=new HashSet<Avp>();
		avpset.add(new Avp(264, (short)80, 0, uri.getBytes()));
		avpset.add(new Avp(296,(short)80,0 , realm.getBytes()));
		avpset.add(new Avp(258,(short)80,0 , par.int32ToBytes(4)));
		return avpset;
	}

	public byte[] merge(byte[] buf,byte[] temp){
		byte spack[] = new byte[buf.length + temp.length];
		for (int i = 0; i < temp.length; i++)
			spack[i] = temp[i];
		for (int i = 0; i < buf.length; i++)
			spack[i + temp.length] = buf[i];
		byte t[] = par.int32ToBytes(spack.length);
		for (int i = 0; i < 3; i++) 
			spack[i + 1] = t[i + 1];
		return spack;
	}

	/*
	@Test
	public void test54() throws Exception {
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		DbHelper.init(className, dbhost, dbname, dbuser, dbpassword);
		PeerImpl peerDat = new PeerImpl("myserveruri");
		byte b[]={1, 0, 0, -128, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0, 0, 0, 1, 1, 64, 0, 0, 14, 0, 1, 10, 14, 0, -25, 0, 0, 0, 0, 1, 11, 0, 0, 0, 12, 0, 0, 0, 1, 0, 0, 1, 10, 64, 0, 0, 12, 0, 0, 0, 0, 0, 0, 1, 2, 64, 0, 0, 12, 0, 0, 0, 4, 0, 0, 1, 13, 0, 0, 0, 19, 116, 114, 117, 68, 105, 97, 109, 101, 116, 101, 114, 0, 0, 0, 1, 8, 64, 0, 0, 19, 109, 121, 115, 101, 114, 118, 101, 114, 117, 114, 105, 0, 0, 0, 1, 40, 64, 0, 0, 16, 116, 114, 117, 112, 104, 111, 110, 101};
		b=peerDat.processPeerRequest(b);
		Hashtable<Integer, Avp> hs=par.decodeAvpSet(b, 20);
		assertEquals("Testing Peer Request",par.bytesToInt(hs.get(268).rawData),2001);
	} */


	
	// Test for worn Relam Name
	@Test
	public void test347() throws Exception{
		PeerImpl.Worker w=(new PeerImpl("myserveruri")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		String uri = "myserveruri";
		String realm = "tru";
		HashSet<Avp> avpset=new HashSet<Avp>();
		avpset.add(new Avp(264, (short)80, 0, uri.getBytes()));
		avpset.add(new Avp(296,(short)80,0 , realm.getBytes()));
		avpset.add(new Avp(258,(short)80,0 , par.int32ToBytes(4)));
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, service_context.getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		HashSet<Avp> t=new HashSet<Avp>();
		t.add(new Avp(450, (short)80, 0, par.int32ToBytes(0)));
		t.add(new Avp(444, (short)80, 0, aNumber.getBytes()));
		avpset.add(new Avp(443, (short)80, 0, par.encodeAvpSet(t)));
		avpset.add(new Avp(455, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437,(short)80,0,par.encodeAvp(new Avp(417,(short)80,0,par.int32ToBytes(1)))))));
		t.clear();
		HashSet<Avp> t1=new HashSet<Avp>();
		t1.add(new Avp(829, (short)80, 10415, par.int32ToBytes(1)));
		t1.add(new Avp(862, (short)80, 10415, par.int32ToBytes(6)));
		t1.add(new Avp(831, (short)80, 10415, ("tel:"+aNumber).getBytes()));
		t1.add(new Avp(832, (short)80, 10415, ("tel:"+bNumber).getBytes()));
		t1.add(new Avp(841, (short)80, 10415, "0234ca0037".getBytes()));
		t.add(new Avp(876, (short)80, 10415, par.encodeAvpSet(t1)));
		byte mncmcc[]={0,98,(byte)248,48,0};
		t.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)40, 10415, mncmcc))));
		avpset.add(new Avp(873,(short)80,10415,par.encodeAvpSet(t)));
		byte packet[]=merge(par.encodeAvpSet(avpset),temp);
		byte resulPac[]=null;

		resulPac = w.processBillingRequest(packet);
		Hashtable<Integer, Avp> hs;
		int resCode=0;
		try{
			hs = par.decodeAvpSet(resulPac, 20);

			resCode=par.bytesToInt(hs.get(268).rawData);
			System.out.println(resCode);
			assertEquals(3003,par.bytesToInt(hs.get(268).rawData));

	
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	
	
	//Test for  wrong  Auth Application id.
		@Test
		public void test349() throws Exception{
			PeerImpl.Worker w=(new PeerImpl("myserveruri")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
			byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
			String uri = "myserveruri";
			String realm = "truphone";
			HashSet<Avp> avpset=new HashSet<Avp>();
			avpset.add(new Avp(264, (short)80, 0, uri.getBytes()));
			avpset.add(new Avp(296,(short)80,0 , realm.getBytes()));
			avpset.add(new Avp(258,(short)80,0 , par.int32ToBytes(9)));
			avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
			avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
			avpset.add(new Avp(461, (short)80, 0, service_context.getBytes()));
			avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(1)));
			avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
			HashSet<Avp> t=new HashSet<Avp>();
			t.add(new Avp(450, (short)80, 0, par.int32ToBytes(0)));
			t.add(new Avp(444, (short)80, 0, aNumber.getBytes()));
			avpset.add(new Avp(443, (short)80, 0, par.encodeAvpSet(t)));
			avpset.add(new Avp(455, (short)80, 0, par.int32ToBytes(1)));
			avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437,(short)80,0,par.encodeAvp(new Avp(417,(short)80,0,par.int32ToBytes(1)))))));
			t.clear();
			HashSet<Avp> t1=new HashSet<Avp>();
			t1.add(new Avp(829, (short)80, 10415, par.int32ToBytes(1)));
			t1.add(new Avp(862, (short)80, 10415, par.int32ToBytes(6)));
			t1.add(new Avp(831, (short)80, 10415, ("tel:"+aNumber).getBytes()));
			t1.add(new Avp(832, (short)80, 10415, ("tel:"+bNumber).getBytes()));
			t1.add(new Avp(841, (short)80, 10415, "0234ca0037".getBytes()));
			t.add(new Avp(876, (short)80, 10415, par.encodeAvpSet(t1)));
			byte mncmcc[]={0,98,(byte)248,48,0};
			t.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)40, 10415, mncmcc))));
			avpset.add(new Avp(873,(short)80,10415,par.encodeAvpSet(t)));
			byte packet[]=merge(par.encodeAvpSet(avpset),temp);
			byte resulPac[]=null;

			resulPac = w.processBillingRequest(packet);
			Hashtable<Integer, Avp> hs;
			int resCode=0;
			try{
				hs = par.decodeAvpSet(resulPac, 20);

				resCode=par.bytesToInt(hs.get(268).rawData);
				System.out.println(resCode);
				assertEquals(3007,par.bytesToInt(hs.get(268).rawData));

		
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	
	
		
		//Testing for Duplicate Session
		@Test
		public void test350() throws Exception{
			PeerImpl.Worker w=(new PeerImpl("myserveruri")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
			byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
			String uri = "myserveruri";
			String realm = "truphone";
			HashSet<Avp> avpset=new HashSet<Avp>();
			avpset.add(new Avp(264, (short)80, 0, uri.getBytes()));
			avpset.add(new Avp(296,(short)80,0 , realm.getBytes()));
			avpset.add(new Avp(258,(short)80,0 , par.int32ToBytes(9)));
			avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
			avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
			avpset.add(new Avp(461, (short)80, 0, service_context.getBytes()));
			avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(1)));
			avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
			HashSet<Avp> t=new HashSet<Avp>();
			t.add(new Avp(450, (short)80, 0, par.int32ToBytes(0)));
			t.add(new Avp(444, (short)80, 0, aNumber.getBytes()));
			avpset.add(new Avp(443, (short)80, 0, par.encodeAvpSet(t)));
			avpset.add(new Avp(455, (short)80, 0, par.int32ToBytes(1)));
			avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437,(short)80,0,par.encodeAvp(new Avp(417,(short)80,0,par.int32ToBytes(1)))))));
			t.clear();
			HashSet<Avp> t1=new HashSet<Avp>();
			t1.add(new Avp(829, (short)80, 10415, par.int32ToBytes(1)));
			t1.add(new Avp(862, (short)80, 10415, par.int32ToBytes(6)));
			t1.add(new Avp(831, (short)80, 10415, ("tel:"+aNumber).getBytes()));
			t1.add(new Avp(832, (short)80, 10415, ("tel:"+bNumber).getBytes()));
			t1.add(new Avp(841, (short)80, 10415, "0234ca0037".getBytes()));
			t.add(new Avp(876, (short)80, 10415, par.encodeAvpSet(t1)));
			byte mncmcc[]={0,98,(byte)248,48,0};
			t.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)40, 10415, mncmcc))));
			avpset.add(new Avp(873,(short)80,10415,par.encodeAvpSet(t)));
			byte packet[]=merge(par.encodeAvpSet(avpset),temp);
			byte resulPac[]=null;

			resulPac = w.processBillingRequest(packet);
			resulPac = w.processBillingRequest(packet);
			Hashtable<Integer, Avp> hs;
		//	int resCode=0;
			try{
				hs = par.decodeAvpSet(resulPac, 20);

//			/	resCode=par.bytesToInt(hs.get(268).rawData);
				//System.out.println("---------------" +resCode);
				assertEquals(5004,par.bytesToInt(hs.get(268).rawData));

		
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		
		
	@Test
	public void test34() throws Exception{
		PeerImpl.Worker w=(new PeerImpl("myserveruri")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, service_context.getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		HashSet<Avp> t=new HashSet<Avp>();
		t.add(new Avp(450, (short)80, 0, par.int32ToBytes(0)));
		t.add(new Avp(444, (short)80, 0, aNumber.getBytes()));
		avpset.add(new Avp(443, (short)80, 0, par.encodeAvpSet(t)));
		avpset.add(new Avp(455, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437,(short)80,0,par.encodeAvp(new Avp(417,(short)80,0,par.int32ToBytes(1)))))));
		t.clear();
		HashSet<Avp> t1=new HashSet<Avp>();
		t1.add(new Avp(829, (short)80, 10415, par.int32ToBytes(1)));
		t1.add(new Avp(862, (short)80, 10415, par.int32ToBytes(6)));
		t1.add(new Avp(831, (short)80, 10415, ("tel:"+aNumber).getBytes()));
		t1.add(new Avp(832, (short)80, 10415, ("tel:"+bNumber).getBytes()));
		t1.add(new Avp(841, (short)80, 10415, "0234ca0037".getBytes()));
		t.add(new Avp(876, (short)80, 10415, par.encodeAvpSet(t1)));
		byte mncmcc[]={0,98,(byte)248,48,0};
		t.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)40, 10415, mncmcc))));
		avpset.add(new Avp(873,(short)80,10415,par.encodeAvpSet(t)));
		byte packet[]=merge(par.encodeAvpSet(avpset),temp);
		byte resulPac[]=null;

		resulPac = w.processBillingRequest(packet);
		Hashtable<Integer, Avp> hs;
		int resCode=0;
		try{
			hs = par.decodeAvpSet(resulPac, 20);

			resCode=par.bytesToInt(hs.get(268).rawData);
			//System.out.println(resCode);
			if(resCode == 2001){
				HashSet<Avp> avpsetUpd=createSet();
				avpsetUpd.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
				avpsetUpd.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
				avpsetUpd.add(new Avp(461, (short)80, 0, service_context.getBytes()));
				avpsetUpd.add(new Avp(416, (short)80, 0, par.int32ToBytes(2)));//
				avpsetUpd.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
				HashSet<Avp> tu=new HashSet<Avp>();
				HashSet<Avp> tu1=new HashSet<Avp>();
				tu1.add(new Avp(420, (short)80, 0, par.int32ToBytes(5)));
				tu1.add(new Avp(417,(short)80,0,par.int32ToBytes(1)));
				tu.add(new Avp(446,(short)80,0,par.encodeAvpSet(tu1)));
				tu.add(new Avp(872,(short)80,10415,par.int32ToBytes(3)));
				avpsetUpd.add(new Avp(456, (short)80, 0, par.encodeAvpSet(tu)));


				byte[] resultcode =w.processBillingRequest(merge(par.encodeAvpSet(avpsetUpd),temp));


				int resUpCode=0;
				hs = par.decodeAvpSet(resultcode, 20);

				resUpCode=par.bytesToInt(hs.get(268).rawData);

				if(resUpCode == 2001){

					HashSet<Avp> avpsett=createSet();
					avpsett.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
					avpsett.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
					avpsett.add(new Avp(461, (short)80, 0, service_context.getBytes()));
					avpsett.add(new Avp(416, (short)80, 0, par.int32ToBytes(3)));//
					avpsett.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
					HashSet<Avp> tt=new HashSet<Avp>();
					HashSet<Avp> tt1=new HashSet<Avp>();
					tt1.add(new Avp(420, (short)80, 0, par.int32ToBytes(5)));
					tt1.add(new Avp(417,(short)80,0,par.int32ToBytes(1)));
					tt.add(new Avp(446,(short)80,0,par.encodeAvpSet(tu1)));
					tt.add(new Avp(872,(short)80,10415,par.int32ToBytes(2)));
					avpsett.add(new Avp(456, (short)80, 0, par.encodeAvpSet(tu)));
					//out.write(merge(par.encodeAvpSet(avpsett),temp));


					Hashtable<Integer, Avp> hdts;
					try{
						resulPac = w.processBillingRequest(merge(par.encodeAvpSet(avpsett),temp));
						hdts = par.decodeAvpSet(resulPac, 20);
						assertEquals(2001,par.bytesToInt(hdts.get(268).rawData));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		

				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	// Session Not Exist Testing for Message
	public void test345() throws SQLException,ClassNotFoundException,IOException,NoSpaceException{
		PeerImpl.Worker w=(new PeerImpl("ggg")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, "dskljhfsdlkjfh".getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(4)));//
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437, (short)80, 0, par.encodeAvp(new Avp(417, (short)80, 0, par.int64ToBytes(1)))))));

		HashSet<Avp> e = new HashSet<Avp>();
		HashSet<Avp> et = new HashSet<Avp>();
		HashSet<Avp> ted = new HashSet<Avp>();
		{
			{

				{
					{
						e.add(new Avp(897, (short)192, 10415,  "tel:18622429128".getBytes()));
						e.add(new Avp(899, (short)192, 10415,  par.int32ToBytes(1)));
						e.add(new Avp(898, (short)192, 10415,  par.encodeAvp(new Avp(8, (short)192, 10415, "20404".getBytes()))));
					}
					et.add(new Avp(886, (short)192, 10415, par.encodeAvpSet(e)));

					{
						e.clear();
						e.add(new Avp(899, (short)192, 10415,  par.int32ToBytes(1)));
						e.add(new Avp(897, (short)192, 10415,  "tel:12018788170".getBytes()));	
					}
					et.add(new Avp(1201, (short)192, 10415, par.encodeAvpSet(e)));

					et.add(new Avp(1210, (short)192, 10415, "abcdefg".getBytes()));

				}
				ted.add(new Avp(877, (short)192, 10415, par.encodeAvpSet(et)));
			}

			{
				et.clear();
				e.clear();
				{
					e.add(new Avp(2017, (short)192, 10415,  "home alone".getBytes()));
					e.add(new Avp(2002, (short)192, 10415,  par.encodeAvp(new Avp(2006,(short)192,10415,par.int32ToBytes(2)))));
					e.add(new Avp(2009, (short)192, 10415,  par.encodeAvp(new Avp(2006, (short)192, 10415, par.int32ToBytes(1)))));
				}
				ted.add(new Avp(2000, (short)192, 10415, par.encodeAvp(new Avp(2017, (short)192, 10415, par.encodeAvpSet(e)))));
			}

			{
				byte mncmcc[]={0,98,(byte)248,48,(byte)255,(byte)255,(byte)255,(byte)255};
				ted.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)80, 10415, mncmcc))));
			}

			avpset.add(new Avp(873, (short)192, 10415,par.encodeAvpSet(ted)));			
		}


		byte packet[]=merge(par.encodeAvpSet(avpset),temp);
		byte[] resulPac = w.processCCRE(packet);
		Hashtable<Integer, Avp> hds = par.decodeAvpSet(resulPac, 20);
		assertEquals(5002,par.bytesToInt(hds.get(268).rawData));

	}
	
	// Session True Testing for Message
		public void testMessafe() throws SQLException,ClassNotFoundException,IOException,NoSpaceException{
			PeerImpl.Worker w=(new PeerImpl("myserveruri")).new Worker(className, dbhost, dbname, dbuser, dbpassword);
			byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
			HashSet<Avp> avpset=createSet();
			avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
			avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
			avpset.add(new Avp(461, (short)80, 0, "dskljhfsdlkjfh".getBytes()));
			avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(4)));//
			avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
			avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437, (short)80, 0, par.encodeAvp(new Avp(417, (short)80, 0, par.int64ToBytes(1)))))));

			HashSet<Avp> e = new HashSet<Avp>();
			HashSet<Avp> et = new HashSet<Avp>();
			HashSet<Avp> ted = new HashSet<Avp>();
			{
				{

					{
						{
							e.add(new Avp(897, (short)192, 10415,  "tel:18622429128".getBytes()));
							e.add(new Avp(899, (short)192, 10415,  par.int32ToBytes(1)));
							e.add(new Avp(898, (short)192, 10415,  par.encodeAvp(new Avp(8, (short)192, 10415, "20404".getBytes()))));
						}
						et.add(new Avp(886, (short)192, 10415, par.encodeAvpSet(e)));

						{
							e.clear();
							e.add(new Avp(899, (short)192, 10415,  par.int32ToBytes(1)));
							e.add(new Avp(897, (short)192, 10415,  "tel:12018788170".getBytes()));	
						}
						et.add(new Avp(1201, (short)192, 10415, par.encodeAvpSet(e)));

						et.add(new Avp(1210, (short)192, 10415, "abcdefg".getBytes()));

					}
					ted.add(new Avp(877, (short)192, 10415, par.encodeAvpSet(et)));
				}

				{
					et.clear();
					e.clear();
					{
						e.add(new Avp(2017, (short)192, 10415,  "home alone".getBytes()));
						e.add(new Avp(2002, (short)192, 10415,  par.encodeAvp(new Avp(2006,(short)192,10415,par.int32ToBytes(2)))));
						e.add(new Avp(2009, (short)192, 10415,  par.encodeAvp(new Avp(2006, (short)192, 10415, par.int32ToBytes(1)))));
					}
					ted.add(new Avp(2000, (short)192, 10415, par.encodeAvp(new Avp(2017, (short)192, 10415, par.encodeAvpSet(e)))));
				}

				{
					byte mncmcc[]={0,98,(byte)248,48,(byte)255,(byte)255,(byte)255,(byte)255};
					ted.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)80, 10415, mncmcc))));
				}

				avpset.add(new Avp(873, (short)192, 10415,par.encodeAvpSet(ted)));			
			}


			byte packet[]=merge(par.encodeAvpSet(avpset),temp);
			byte[] resulPac = w.processCCRE(packet);
			Hashtable<Integer, Avp> hds = par.decodeAvpSet(resulPac, 20);
			assertEquals(2001,par.bytesToInt(hds.get(268).rawData));



		}

		

		
		
}





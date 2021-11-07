package com.shrewdify.diaro.impl;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import com.shrewdify.diaro.api.Connector;
import com.shrewdify.diaro.utils.Avp;
import com.shrewdify.diaro.utils.Config;
import com.shrewdify.diaro.utils.ElementParser;
/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */

public class ConnectorImpl extends Connector{

	int n=0;
	long starttime,endtime;
	public ConnectorImpl(Socket s,int n) {
		try {
			this.n=n;
			this.s = s;
			this.in = new DataInputStream(s.getInputStream());
			this.out = new DataOutputStream(s.getOutputStream());
			par = new ElementParser();
			byte[] buf = sendPeerRequest();
			if (buf != null) {
				try {
					out.write(buf);
					sessions = new Hashtable<String, Hashtable<String, String>>();
					isPeered=true;
				} catch (Exception e) {
					System.out.println("Rejected Peer with IP:");
					out.close();
					in.close();
					this.s.close();
				}
			}
		} catch (Exception e) {
			System.out.println("Error:" + e.getMessage());
		}
	}

	boolean start=false;

	@Override
	public byte[] sendPeerRequest() throws Exception {
		//264,296,257,266,269,258,267;

		byte temp[]={1, 0, 0, -92, -128, 0, 1, 1, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};

		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(257,(short)80,0 , par.addressToBytes(InetAddress.getLocalHost())));
		avpset.add(new Avp(266,(short)80,0 , par.int32ToBytes(0)));
		avpset.add(new Avp(269,(short)0,0 , "truDiameter".getBytes()));
		avpset.add(new Avp(267,(short)0,0 , par.int32ToBytes(1)));
		return merge(par.encodeAvpSet(avpset), temp);

	}

	public HashSet<Avp> createSet(){
		HashSet<Avp> avpset=new HashSet<Avp>();
		avpset.add(new Avp(264, (short)80, 0, Config.uri.getBytes()));
		avpset.add(new Avp(296,(short)80,0 , Config.realm.getBytes()));
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

	@Override
	public void run() {
		try {
			while (!s.isClosed()) {
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
				processBillingResponse(buf);
				buf = null;
			}
		}  catch (IOException e) {
			System.out.println("Client with uri:"+uri+" closed");
		}
	}
	boolean ready=false;
	int q=0,q1=0;;
	int term=0;
	@Override
	public void processBillingResponse(byte[] request) {
		try{
			Hashtable<Integer, Avp> hs=par.decodeAvpSet(request, 20);
			int rescode=par.bytesToInt(hs.get(268).rawData);
			if(rescode==2001){
				int type=(int)(par.bytesToInt(Arrays.copyOfRange(request, 4, 8)) & 0xFFFFFF);
				switch(type){
				case 257:
					ready=true;
					starttime=System.currentTimeMillis();
					term=n;
					for(int i=0;i<n;i++)
					{
					//	System.out.println("NumberSent:"+i);
						sendCCRequestI("18622429128", "12018788170", "test_"+(Math.random()*1000)+System.currentTimeMillis());
						//sendCCRequestE("test_"+(Math.random()*1000)+System.currentTimeMillis());

					}
					break;
				default:
					int cc_type=par.bytesToInt(hs.get(416).rawData);
					String session=par.bytesToOctetString(hs.get(263).rawData);
					switch(cc_type){
					case 1:
					//	System.out.println("Update:"+(q++));
						sendCCRequestU(session);
						break;
					case 2:
						sendCCRequestT(session);
						break;
					case 3:
						term--;
						//System.out.println("term:"+(q1++));
						if(term==0) 
						{
							endtime=System.currentTimeMillis();
							System.out.println("Total time:"+(int)((endtime-starttime)));
							System.exit(0);
						}
						break;
					}
				case 4:
					//System.out.println("Message Received.");			
					break;
				}
			}	
		}
		catch(Exception e){
			System.out.println("Error:"+e.getMessage());
		}
	}

	@Override
	public void sendCCRequestI(String aNumber, String bNumber, String sessionId)
			throws Exception {
		//264,296,258
		//263,283,461,416,415,//456
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, Config.service_context.getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		HashSet<Avp> t=new HashSet<Avp>();
		t.add(new Avp(450, (short)80, 0, par.int32ToBytes(0)));
		t.add(new Avp(444, (short)80, 0, aNumber.getBytes()));
		avpset.add(new Avp(443, (short)80, 0, par.encodeAvpSet(t)));
		avpset.add(new Avp(455, (short)80, 0, par.int32ToBytes(1)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvp(new Avp(437,(short)80,0,par.encodeAvp(new Avp(417,(short)80,0,par.int64ToBytes(1)))))));
		t.clear();
		HashSet<Avp> t1=new HashSet<Avp>();
		t1.add(new Avp(829, (short)80, 10415, par.int32ToBytes(1)));
		t1.add(new Avp(862, (short)80, 10415, par.int32ToBytes(6)));
		t1.add(new Avp(831, (short)80, 10415, ("tel:"+aNumber).getBytes()));
		t1.add(new Avp(832, (short)80, 10415, ("tel:"+bNumber).getBytes()));
		t1.add(new Avp(841, (short)80, 10415, "0234ca0037".getBytes()));
		t.add(new Avp(876, (short)80, 10415, par.encodeAvpSet(t1)));
		byte mncmcc[]={0,98,(byte)248,48,(byte)255,(byte)255,(byte)255,(byte)255};
		t.add(new Avp(874, (short)80, 10415, par.encodeAvp(new Avp(22, (short)40, 10415, mncmcc))));
		avpset.add(new Avp(873,(short)80,10415,par.encodeAvpSet(t)));
		out.write(merge(par.encodeAvpSet(avpset),temp));

	}

	@Override
	public void sendCCRequestU(String sessionId) throws Exception {
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, Config.service_context.getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(2)));//
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		HashSet<Avp> t=new HashSet<Avp>();
		HashSet<Avp> t1=new HashSet<Avp>();
		t1.add(new Avp(420, (short)80, 0, par.int32ToBytes(5)));
		t1.add(new Avp(417,(short)80,0,par.int64ToBytes(1)));
		t.add(new Avp(446,(short)80,0,par.encodeAvpSet(t1)));
		t.add(new Avp(872,(short)80,10415,par.int32ToBytes(3)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvpSet(t)));
		out.write(merge(par.encodeAvpSet(avpset),temp));

	}

	@Override
	public void sendCCRequestT(String sessionId) throws Exception {
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, Config.service_context.getBytes()));
		avpset.add(new Avp(416, (short)80, 0, par.int32ToBytes(3)));//
		avpset.add(new Avp(415, (short)80, 0, par.int32ToBytes(1)));
		HashSet<Avp> t=new HashSet<Avp>();
		HashSet<Avp> t1=new HashSet<Avp>();
		t1.add(new Avp(420, (short)80, 0, par.int32ToBytes(5)));
		t1.add(new Avp(417,(short)80,0,par.int64ToBytes(1)));
		t.add(new Avp(446,(short)80,0,par.encodeAvpSet(t1)));
		t.add(new Avp(872,(short)80,10415,par.int32ToBytes(2)));
		avpset.add(new Avp(456, (short)80, 0, par.encodeAvpSet(t)));
		out.write(merge(par.encodeAvpSet(avpset),temp));
	}

	@Override
	public void sendCCRequestE(String sessionId) throws Exception {
		byte temp[]={1, 0, 0, -92, -128, 0, 1, 16, 0, 0, 0, 0, 117, 127, 19, -116, 41, -96, 0, 0};
		HashSet<Avp> avpset=createSet();
		avpset.add(new Avp(263, (short)80, 0, sessionId.getBytes()));
		avpset.add(new Avp(283, (short)80, 0, "truphone".getBytes()));
		avpset.add(new Avp(461, (short)80, 0, Config.service_context.getBytes()));
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


		out.write(merge(par.encodeAvpSet(avpset),temp));
	}

}

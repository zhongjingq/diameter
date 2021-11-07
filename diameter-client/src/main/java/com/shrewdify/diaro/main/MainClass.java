package com.shrewdify.diaro.main;

/**
 * 
 * @author Nitin Panuganti
 * @version 1.0
 * @company Shrewdify Technologies Pvt Ltd.
 * @year 2015
 */


public class MainClass {

	public static void main(String args[]) {
		
		//arg[0]- server host
		//arg[1]- count
		
		new ClientIO(args[0],args[1]);
	}

}

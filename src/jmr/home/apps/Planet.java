package jmr.home.apps;

import jmr.home.comm.SerialConnector;

public class Planet {

	private final SerialConnector connector;
	
	private String strName;
	
	public Planet(	final SerialConnector connector ) {
		this.connector = connector;
		this.strName = "<unknown>";
	}
	
	public void close() {
		this.connector.close();
	}
	
	public String getName() {
		return this.strName;
	}
	
	public String getStatus() {
		return "<unknown>";
	}
	
}

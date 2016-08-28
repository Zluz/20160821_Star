package jmr.home.apps;

import jmr.home.comm.SerialConnector;

public class Planet {

	public final SerialConnector connector;
	
	private String strName;
	private String strStatus;
	
	public Planet(	final SerialConnector connector ) {
		this.connector = connector;
		this.strName = "<unknown>";
		this.strStatus = "<unknown>";
	}
	
	public void close() {
		this.connector.close();
	}
	
	public String getName() {
		return this.strName;
	}
	
	public String getStatus() {
		return this.strStatus;
	}

	public void setName( final String strName ) {
		this.strName = strName;
	}

	public void setStatus( final String strStatus ) {
		if ( null==strStatus ) return;
		this.strStatus = strStatus;
	}
	
}

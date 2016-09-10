package jmr.home.model;

import java.util.Date;

import jmr.home.database.StarTable;
import jmr.util.Util;

public class Star {

	private final long seq;
	
	private final String strHostname;
	private final String strIP;
	private final Date dateStart = new Date();
	private final Date dateBuild;
	
	private static Star instance = new Star();
	
	@SuppressWarnings("deprecation") //TODO fix this..
	private Star() {
		this.strHostname = Util.getHostname();
		this.strIP = Util.getHostIP();
		this.dateBuild = new Date( Date.parse( "09/10/2016" ) );
		
		this.seq = new StarTable().write( this );
	}
	
	public static Star get() {
		if ( null==instance ) {
			instance = new Star();
		}
		return instance;
	}
	
	public String getHostname() {
		return this.strHostname;
	}
	
	public String getIP() {
		return this.strIP;
	}
	
	public Date getStartTime() {
		return this.dateStart;
	}
	
	public Date getBuildDate() {
		return this.dateBuild;
	}
	
	public long getSeq() {
		return this.seq;
	}
	
	public static void main( final String[] args ) {
		final Star star = Star.get();
		final long lSeq = star.getSeq();
		System.out.println( "Star seq = " + lSeq );
	}
	
}

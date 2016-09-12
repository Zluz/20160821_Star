package jmr.home.database;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BaseTable {

	final static SimpleDateFormat format = 
					new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	
	public static String format( final Object obj ) {
		if ( obj instanceof Date ) {
			final Date date = (Date)obj;
			final String strDate = "\"" + format.format( date ) + "\"";
			return strDate;
		} else if ( obj instanceof String ) {
			String strWorking = obj.toString();
			strWorking = strWorking.replaceAll( "\"", "\\\"" );
			return "\"" + strWorking + "\"";
		} else if ( null!=obj ) {
			return "\"" + obj.toString() + "\""; 
		} else {
			return "null";
		}
	}
	
}

package jmr.home;

import jmr.home.apps.Planet;
import jmr.home.database.DatabaseLogger;
import jmr.home.model.Atom;

public class Log {


	public static void log(	final String strText,
							final Planet planet,
							final Atom atom ) {
		DatabaseLogger.log( strText, planet, atom );
	}


	public static void log(	final String strText,
							final Atom atom ) {
		log( strText, null, atom );
	}


	public static void log(	final String strText,
							final Planet planet ) {
		log( strText, planet, null );
	}


	public static void log( final String strText ) {
		log( strText, null, null );
	}

}

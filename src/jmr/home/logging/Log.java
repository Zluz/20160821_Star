package jmr.home.logging;

import jmr.home.apps.Planet;
import jmr.home.database.DatabaseLogger;
import jmr.home.model.Atom;

public class Log {


	public static void log(	final EventType type,
							final String strText,
							final Planet planet,
							final Atom atom,
							final Class<? extends Exception> exception ) {
		DatabaseLogger.log( type, strText, planet, atom, exception );
	}


	public static void log(	final EventType type,
							final String strText,
							final Atom atom ) {
		log( type, strText, null, atom, null );
	}


	public static void log(	final EventType type,
							final String strText,
							final Planet planet ) {
		log( type, strText, planet, null, null );
	}


	public static void log( final EventType type,
							final String strText ) {
		log( type, strText, null, null, null );
	}

	public static void log(	final String strText,
							final Class<? extends Exception> exception ) {
		final EventType type;
		if ( null!=exception ) {
			type = EventType.getEventType( exception );
		} else {
			type = EventType.EX_GENERAL;
		}
		log( type, strText, null, null, exception );
	}

	public static void log(	final String strText,
							final Exception exception ) {
		final EventType type;
		final Class<? extends Exception> classException;
		if ( null!=exception ) {
			classException = exception.getClass();
			type = EventType.getEventType( classException );
		} else {
			classException = null;
			type = EventType.EX_GENERAL;
		}
		log( type, strText, null, null, classException );
	}

}

package jmr.home.logging;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jmr.home.database.BaseTable;
import jmr.home.database.ConnectionProvider;

public enum EventType {
/*
 *   0-99 	- testing
 * 100-899 	- general purpose
 * 800-999	- exceptions
 * 
 * 1000-9999 - reserved for planet
 * 
 * xxx00 - general
 * xxx12 - instantiated
 * xxx14 - starting
 * xxx16 - ready
 * 
 * xxx24 - paused
 * xxx26 - resumed
 * 
 * xxx30 - in processing
 * 
 * xxx94 - stopping
 * xxx96 - stopped
 * xxx98 - deallocated
 * 
 * 10100 - application
 * 10200 - universe
 * 10300 - galaxy
 * 10400 - star
 * 10500 - planet
 * 10600 - moon:servo/sensor
 *
 * 11200 - service:http server
 * 
 */
	
	DIAGNOSTIC_TEST(	10, "Diagnostic test" ),
	
	IATOMCONSUMER_REGISTERED(		100, "IAtomConsumer registered" ),
	IATOMCONSUMER_UNREGISTERED(		101, "IAtomConsumer unregistered" ),
	INIT_COMM_TO_PLANET( 			102, "Initializing communication with planet" ),
	MISSING_SCHEDULED_COMM_FROM_PLANET(	103, "No communication from planet within expected time (schedule plus grace period)" ),

	EX_GENERAL(	800, Exception.class ),
	EX_SQL(		810, SQLException.class ),

	SEND_CODE_NODE_INIT(		2000, "Atom from planet, SEND_CODE_NODE_INIT" ),
	SEND_CODE_SCHEDULED(		2001, "Atom from planet, SEND_CODE_SCHEDULED" ),
	SEND_CODE_DIGITAL_CHANGE(	2002, "Atom from planet, SEND_CODE_DIGITAL_CHANGE" ),
	SEND_CODE_ANALOG_CHANGE(	2003, "Atom from planet, SEND_CODE_ANALOG_CHANGE" ),
	SEND_CODE_TRIGGER(			2004, "Atom from planet, SEND_CODE_TRIGGER" ),
	SEND_CODE_REQUESTED(		2005, "Atom from planet, SEND_CODE_REQUESTED" ),

	APP_STARTING(	11014, "Application starting" ),
	APP_READY(		11016, "Application ready" ),
	APP_ENDING(		11094, "Application ending" ),

//	ATOM_


	SERVICE_HTTP_STARTING(	11214, "Service[HTTP server] starting" ),
	SERVICE_HTTP_READY(		11216, "Service[HTTP server] ready" ),

	SERVICE_HTTP_HANDLE_ATOM( 11241, "Service[HTTP server] Atom submitted" ),

	SERVICE_HTTP_STOPPING(	11294, "Service[HTTP server] stopping" ),
	SERVICE_HTTP_STOPPED(	11296, "Service[HTTP server] stopped" ),

	;
	
	
	private final long lSeq;
	private final String strText;
	private final Class<? extends Exception> exception;
	
	private EventType(	final long seq,
						final String strText,
						final Class<? extends Exception> e ) {
		this.lSeq = seq;
		this.strText = strText;
		this.exception = e;
	}
	
	private EventType( 	final long seq,
						final String strText ) {
		this( seq, strText, null );
	}
	
	private EventType( 	final long seq,
						final Class<? extends Exception> e ) {
		this( seq, null, e );
	}
	
	public long getSeq() {
		return this.lSeq;
	}

	
	public String getText() {
		return this.strText;
	}


	public static EventType getEventType( final Class<? extends Exception> e ) {
		final List<EventType> list = 
				new LinkedList<EventType>( Arrays.asList( EventType.values() ) );
		Collections.reverse( list );
		
		for ( final EventType type : list ) {
			if ( null!=type.exception ) {
				if ( type.exception.isInstance( e ) ) {
					return type;
				}
			}
		}
		return null;
	}
	

	public static EventType getEventType( final long lSeq ) {
		for ( final EventType type : EventType.values() ) {
			if ( lSeq==type.getSeq() ) {
				return type;
			}
		}
		return null;
	}
	
	
	
	private static void doUploadEventTypesToDatabase() {
		
		System.out.print( "Loading EventTypes.." );
		
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {

				final String strDeleteLog = "DELETE FROM galaxy.log WHERE seq>0;";
				stmt.execute( strDeleteLog );
				final String strDeleteEvent = "DELETE FROM galaxy.event WHERE seq>0;";
				stmt.execute( strDeleteEvent );

				for ( final EventType type : EventType.values() ) {

					final String strExceptionText;
					if ( null!=type.exception ) {
						strExceptionText = type.exception.getName();
					} else {
						strExceptionText = null; // "null";
					}

					final String strInsertEvent = 
							"INSERT INTO Event "
							+ "( seq, Name, Description, Exception ) "
							+ "VALUES ( "
									+ type.getSeq() + ", " 
									+ "\"" + type.name() + "\", " 
									+ BaseTable.format( type.getText() ) + ", "
									+ BaseTable.format( strExceptionText ) + " )";
					stmt.execute( strInsertEvent );

					System.out.print( "." );
				}
			}

			System.out.println( "Done." );

		} catch ( final SQLException e ) {
			System.out.println();
			e.printStackTrace();
		}
	}
	
	public static void main( String[] args ) {
		doUploadEventTypesToDatabase();
	}
	
}

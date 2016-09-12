package jmr.home.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import jmr.home.apps.Planet;
import jmr.home.logging.EventType;
import jmr.home.model.Atom;
import jmr.home.model.Star;

public class DatabaseLogger extends BaseTable {

	private static DatabaseLogger instance;
	
	private DatabaseLogger() {};
	
	public static DatabaseLogger get() {
		if ( null==instance ) {
			instance = new DatabaseLogger();
		}
		return instance;
	}
	
	/*
	 * NOTE: Only one of EventType OR Class<Exception> will be used
	 */
	public static void log(	final EventType type,
							final String strText,
							final Planet planet,
							final Atom atom, 
							final Class<? extends Exception> exception ) {
//		if ( null==strText ) return;
//		if ( strText.isEmpty() ) return;
		
		final Date date = new Date();
		
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {
				
//				INSERT INTO table_name (column1,column2,column3,...)
//				VALUES (value1,value2,value3,...);
				
				final String strStarSeq;
				if ( null!=Star.get() ) {
					strStarSeq = Long.toString( Star.get().getSeq() );
				} else {
					strStarSeq = "null";
				}
				
				final String strAtomSeq;
				if ( null!=atom && null!=atom.getSeq() ) {
					strAtomSeq = Long.toString( atom.getSeq() );
				} else {
					strAtomSeq = "null";
				}
				
				final String strPlanetSeq;
				if ( null!=planet && null!=planet.getSeq() ) {
					strPlanetSeq = Long.toString( planet.getSeq() );
				} else {
					strPlanetSeq = "null";
				}
				
				final EventType typeException;
				if ( null!=exception ) {
					typeException = EventType.getEventType( exception );
				} else {
					typeException = null;
				}
				
				final String strTypeSeq;
				if ( null!=type ) {
					strTypeSeq = Long.toString( type.getSeq() );
				} else if ( null!=typeException ) {
					strTypeSeq = Long.toString( typeException.getSeq() );
				} else {
					strTypeSeq = "null";
				}
				
				final String strSQL = 
						"INSERT INTO Log "
								+ "( Date, Time, seq_Type, seq_Star, seq_Planet, seq_Atom, Text ) "
						+ "VALUES ( " 
								+ format( date ) + ", " 
								+ date.getTime() + ", "
								+ strTypeSeq + ", "
								+ strStarSeq + ", "
								+ strPlanetSeq + ", "
								+ strAtomSeq + ", "
								+ format( strText ) + " )";
				stmt.execute( strSQL, Statement.RETURN_GENERATED_KEYS );
			}
			
		} catch ( final SQLException e ) {
			e.printStackTrace();
		}
	}

	public static void main( final String[] args ) {
//		Log.log( "test" );
	}
	
}

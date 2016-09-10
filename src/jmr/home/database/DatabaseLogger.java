package jmr.home.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import jmr.home.Log;
import jmr.home.apps.Planet;
import jmr.home.model.Atom;

public class DatabaseLogger extends BaseTable {

	private static DatabaseLogger instance;
	
	private DatabaseLogger() {};
	
	public static DatabaseLogger get() {
		if ( null==instance ) {
			instance = new DatabaseLogger();
		}
		return instance;
	}
	
	public static void log(	final String strText,
							final Planet planet,
							final Atom atom ) {
		if ( null==strText ) return;
		if ( strText.isEmpty() ) return;
		
		final Date date = new Date();
		
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {
				
//				INSERT INTO table_name (column1,column2,column3,...)
//				VALUES (value1,value2,value3,...);
				
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
				
				final String strSQL = 
						"INSERT INTO Log "
								+ "( Date, Time, seq_Planet, seq_Atom, Text ) "
						+ "VALUES ( \"" 
								+ format( date ) + "\", " 
								+ date.getTime() + ", "
								+ strPlanetSeq + ", "
								+ strAtomSeq + ", "
								+ "\"" + strText + "\" )";
				stmt.execute( strSQL, Statement.RETURN_GENERATED_KEYS );
			}
			
		} catch ( final SQLException e ) {
			e.printStackTrace();
		}
	}

	public static void main( final String[] args ) {
		Log.log( "test" );
	}
	
}

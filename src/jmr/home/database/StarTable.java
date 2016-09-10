package jmr.home.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jmr.home.model.Star;

public class StarTable extends BaseTable implements ITable<Star> {

	@Override
	public Long write( final Star star ) {
		if ( null==star ) return null;
		
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {
				
//				INSERT INTO table_name (column1,column2,column3,...)
//				VALUES (value1,value2,value3,...);
				
				final String strSQL = 
						"INSERT INTO Star ( Hostname, IP, Start, Build) "
						+ "VALUES ( "
								+ "\"" + star.getHostname() + "\", " 
								+ "\"" + star.getIP() + "\", " 
								+ "\"" + format( star.getStartTime() ) + "\", " 
								+ "\"" + format( star.getBuildDate() ) + "\" )";
				stmt.execute( strSQL, Statement.RETURN_GENERATED_KEYS );
				final ResultSet keys = stmt.getGeneratedKeys();
				if ( keys.next() ) {
					final long lSeq = keys.getLong(1);
					return lSeq;
				}
			}
			
		} catch ( final SQLException e ) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Star read( final long seq ) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main( final String[] args ) {
		final Star star = Star.get();
		final long lSeq = star.getSeq();
		System.out.println( "Star seq = " + lSeq );
	}

}

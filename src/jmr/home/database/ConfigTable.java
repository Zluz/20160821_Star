package jmr.home.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class ConfigTable 
				extends HashMap<String,String> 
				implements ITable<String> {

	private static final long serialVersionUID = 6316750981590312399L;
	
	private static ConfigTable instance = null;
	
//	private final Map<String,String> map = new HashMap<>();
	
	private ConfigTable() {
		this.load();
	}
	
	@Override
	public Long write( final String element ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String read( final long seq ) {
		// TODO Auto-generated method stub
		return null;
	}

	public static ConfigTable get() {
		if ( null==instance ) {
			instance = new ConfigTable();
		}
		return instance;
	}
	
	private void load() {
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {
				
//				INSERT INTO table_name (column1,column2,column3,...)
//				VALUES (value1,value2,value3,...);
				
				final String strSQL = "SELECT Name, Value FROM Config;";
				final ResultSet rs = stmt.executeQuery( strSQL );
				while ( rs.next() ) {
					final String strName = rs.getString( 1 );
					final String strValue = rs.getString( 2 );
//					this.map.put( strName, strValue );
					this.put( strName, strValue );
				}
			}
		} catch ( final SQLException e ) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void main( final String[] args ) {
		System.out.println( ConfigTable.get().keySet() );
	}

}

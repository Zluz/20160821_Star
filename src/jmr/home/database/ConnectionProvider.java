package jmr.home.database;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionProvider {

	private static final String CONNECTION_STRING = 
					"jdbc:mysql://192.168.1.200:3306/galaxy?useSSL=false";
	
	private static ConnectionProvider instance;
	
	private ConnectionProvider() {
		// see http://www.javatips.net/blog/c3p0-connection-pooling-example
		cpds = new ComboPooledDataSource();
       	try {
			cpds.setDriverClass("com.mysql.jdbc.Driver");
		} catch ( final PropertyVetoException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //loads the jdbc driver
       	
//       	cpds.setJdbcUrl("jdbc:mysql://localhost/test");
       	cpds.setJdbcUrl( CONNECTION_STRING );
	    cpds.setUser("planet");
	    cpds.setPassword("planet");
	    
//        // the settings below are optional -- c3p0 can work with defaults
//        cpds.setMinPoolSize(5);
//        cpds.setAcquireIncrement(5);
//        cpds.setMaxPoolSize(20);
//        cpds.setMaxStatements(180);
	};
	
	private Connection conn;
	
	private final ComboPooledDataSource cpds;
	
	public static ConnectionProvider getInstance() {
		if ( null==instance ) {
			instance = new ConnectionProvider();
		}
		return instance;
	}
	
	public void initialize() {
//        final Connection conn = DriverManager.getConnection(
//                "jdbc:mysql://localhost:3306/ebookshop?useSSL=false", "myuser", "xxxx"); // MySQL
	}
	
	public static Connection get() {
		final Connection connOriginal = getInstance().conn;
		try {
			if ( null==connOriginal || connOriginal.isClosed() ) {
				final Connection connNew = getInstance().cpds.getConnection();
				getInstance().conn = connNew;
			}
//		        final Connection connNew = DriverManager.getConnection(
//		                CONNECTION_STRING, "planet", "planet" );
//		        getInstance().conn = connNew;
//			}
		} catch ( final SQLException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getInstance().conn;
		
//		try {
//			final Connection conn = getInstance().cpds.getConnection();
//			return conn;
//		} catch ( final SQLException e ) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return null;
	}
	
	public static Statement createStatement() {
		final Connection conn = get();
		if ( null!=conn ) {
			try {
				return conn.createStatement();
			} catch ( final SQLException e ) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}
	
	
	public static void main( final String[] args ) throws SQLException {
		final Connection conn = ConnectionProvider.get();
		if ( null==conn ) return;
		
		final Statement stmt = conn.createStatement();
		if ( null==stmt ) return;
		
		stmt.executeQuery( "select 1;" );
	}
	
	
}

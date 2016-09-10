package jmr.home.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionProvider {

	private static final String CONNECTION_STRING = 
					"jdbc:mysql://192.168.1.210:3306/galaxy?useSSL=false";
	
	private static ConnectionProvider instance;
	
	private ConnectionProvider() {};
	
	private Connection conn;
	
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
		        final Connection connNew = DriverManager.getConnection(
		                CONNECTION_STRING, "planet", "planet" );
		        getInstance().conn = connNew;
			}
		} catch ( final SQLException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return getInstance().conn;
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

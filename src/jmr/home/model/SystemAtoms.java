package jmr.home.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class SystemAtoms {

	final static public String NAME_PROPERTIES = "Properties";
	final static public String NAME_ENVVARS = "Environment";

	public static Atom generateJavaProperties() {
		final Atom atom = new Atom( Atom.Type.SYSTEM, NAME_PROPERTIES, null );
		final Properties properties = System.getProperties();

		final List<String> listKeys = new LinkedList<String>();
		for ( final Entry<Object, Object> entry : properties.entrySet() ) {
			final String strName = entry.getKey().toString();
//			final String strValue = entry.getValue().toString();
//			atom.put( strName, strValue );
			
			listKeys.add( strName );
		}
		Collections.sort( listKeys );
		for ( final String strKey : listKeys ) {
//			final String strName = entry.getKey().toString();
//			final String strValue = entry.getValue().toString();
			final String strValue = properties.getProperty( strKey );
			atom.put( strKey, strValue );
		}
		return atom;
	}

	public static Atom generateEnvironmentVariables() {
		final Atom atom = new Atom( Atom.Type.SYSTEM, NAME_ENVVARS, null );
		final Map<String, String> map = System.getenv();
		
//		for ( final Entry<String, String> entry : properties.entrySet() ) {
//			final String strName = entry.getKey();
//			final String strValue = entry.getValue();
//			atom.put( strName, strValue );
//		}
//		return atom;
		
		final List<String> listKeys = new LinkedList<String>( map.keySet() );
		Collections.sort( listKeys );
		for ( final String strKey : listKeys ) {
//			final String strName = entry.getKey().toString();
//			final String strValue = entry.getValue().toString();
			final String strValue = map.get( strKey );
			atom.put( strKey, strValue );
		}
		return atom;

	}
	
	public static void main( final String[] args ) {
		
	}

}

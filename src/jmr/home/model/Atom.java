package jmr.home.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Atom extends HashMap<String,String> {

	public static enum Type { SYSTEM, STATUS, EVENT };

	public final static String VAL_FIELD_DELIM = "\n";
	
	public final static String VAR_TIME = "Atom.Time";
	
	private static final long serialVersionUID = 1L;
	
	public String strName;
	
	private final long lTime = System.currentTimeMillis();
	
	public final Type type;
	
	/** Ordered list of keys */
	private final List<String> listKeys = new LinkedList<String>();
	
	public Atom( final Type type ) {
		this.type = type;
		this.put( VAR_TIME, Long.toString( lTime ) );
	};
	
	public Atom(	final Type type,
					final String strName ) {
		this( type );
		this.strName = strName;
	}
	
	@Override
	public String put(	final String key, 
						final String value ) {
		if ( !isValidString( key ) ) return null;
		
		listKeys.add( key );
		return super.put(key, value);
	}
	
	public List<String> getOrderedKeys() {
		return new ArrayList<String>( this.listKeys );
	}
	
	public void setName( final String strName ) {
		this.strName = strName;
	}
	
	public String getName() {
		return strName;
	}
	
	public static boolean isValidString( final String str ) {
		if ( null==str ) return false;
		if ( str.isEmpty() ) return false;
		
		final char[] chars = str.toCharArray();
		for ( final char c : chars ) {
			if ( !isValidChar(c) ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isValidChar( final char c ) {
		if ( Character.isJavaIdentifierPart( c ) ) return true;
		if ( "()[]<>_/\\\\.".indexOf(c)>-1 ) return true;
		return false;
	}
	
	public void load( final String strLine ) {
		final String[] parts = strLine.split( "=" );
		if ( parts.length<2 ) return;
		
		String strName = parts[0].trim();
		String strValue = parts[1].trim();
		
		while ( !strName.isEmpty() && !isValidChar( strName.charAt(0) ) ) {
			strName = strName.substring( 1 );
		}
		while ( !strValue.isEmpty() 
				&& !isValidChar( strValue.charAt(strValue.length()-1) ) ) {
			strValue = strValue.substring( 0, strValue.length()-1 ) ;
		}
		
		strName = strName.trim();
		strValue = strValue.trim();
		if ( strName.isEmpty() ) {
//			strAtomName = strValue;
			this.setName( strValue );
		} else {
			this.put( strName, strValue );
		}

	}
}

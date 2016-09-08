package jmr.integrate.arduino;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jmr.util.Util;

public class SketchDefines {

	private final String strDefineFilename = 
			"C:\\Development\\Git\\Star__20160821\\arduino\\EthernetNode\\DefineMessages.h";
	
	private static SketchDefines instance;
	
	private Map<String,Integer> map = new HashMap<>();
	
	private SketchDefines() {
		this.load();
		instance = this;
	}
	
	private void load() {
		final File file = new File( strDefineFilename );
		final String strContent = Util.getTextFromFile( file );
		
		final String strDefine = "#define";
		
		for ( final String strLine : strContent.split( "\n" ) ) {
			if ( strLine.startsWith( strDefine ) ) {
				String strTarget = strLine;
				strTarget = strTarget.substring( strDefine.length() );
				strTarget = strTarget.trim();
				strTarget = strTarget.replaceAll( "[\\s]+", " " );
				final String[] parts = strTarget.split( " " );
				if ( parts.length<2 ) break;
				try {
					final int iValue = Integer.parseInt( parts[1] );
					final String strKey = parts[0].trim();
					map.put( strKey, iValue );
				} catch ( final NumberFormatException e ) {
					break;
				}
			}
		}
	}
	
	public static SketchDefines getInstance() {
		if ( null==instance ) {
			new SketchDefines();
		}
		return SketchDefines.instance;
	}
	
	public static int get( final String strName ) {
		SketchDefines.getInstance();
		
		if ( instance.map.containsKey( strName ) ) {
			return instance.map.get( strName );
		}
		final String strError = "Unknown Define: " + strName;
		throw new IllegalStateException( strError );
	}
	
	
	
	public static void main( final String[] args ) {
		final int value = SketchDefines.get( "MSG_OP_UNKNOWN" );
		assert( value>0 );
	}
	
}

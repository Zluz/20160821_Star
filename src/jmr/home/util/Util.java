package jmr.home.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;

public class Util {

	
	public static final String UNKNOWN = "(unknown)";
	
	
	public static File extract( final String strInnerFile ) {
		try {
			final File file = File.createTempFile( "temporary_", ".jar" );
			file.delete();
		    final Class<? extends String> clazz = strInnerFile.getClass();
			final InputStream link = clazz.getResourceAsStream( strInnerFile );
			if ( null==link ) {
				System.err.println( "Null stream, file: " + strInnerFile );
				return null;
			}
			Files.copy( link, file.getAbsoluteFile().toPath() );
			file.deleteOnExit();
			return file;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getHostname() {
		try {
			final InetAddress host = InetAddress.getLocalHost();
			final String strHostname = host.getHostName();
			return strHostname;
		} catch ( final UnknownHostException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return UNKNOWN;
	}
	
}

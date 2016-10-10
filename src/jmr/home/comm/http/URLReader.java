package jmr.home.comm.http;

import java.net.*;
import java.io.*;

/*
 * This is copied: 
 * 	StarApp: jmr.home.comm.http.URLReader
 *  UI_RCP: jmr.home.comm.http.URLReader
 */
public class URLReader {
	
	final String strURL;
	
	public URLReader( final String strURL ) {
		this.strURL = strURL;
	}
	
	/**
	 * Returns the content at the URL.
	 * Returns NULL if there was any problem.
	 * @return
	 */
	public String getContent() {
		System.out.println( "Opening URL: " + strURL );
		final URL url;
		try {
			url = new URL( strURL );
		} catch ( final MalformedURLException e ) {
			return null;
		}
		final URLConnection conn;
		try {
			conn = url.openConnection();
			conn.setConnectTimeout( 3000 );
			conn.setReadTimeout( 4000 );
		} catch ( final IOException e1) {
			return null;
		}
		try ( final InputStreamReader isr = new InputStreamReader( conn.getInputStream() );
				final BufferedReader in = new BufferedReader( isr ) ) {
//			final URL url = new URL( strURL );
//	        final InputStreamReader isr = new InputStreamReader(url.openStream());
//			final BufferedReader in = new BufferedReader( isr );
	
			final StringBuffer strbuf = new StringBuffer();
	        String strLine;
	        while ((strLine = in.readLine()) != null)
	            strbuf.append( strLine );
	        in.close();
	        
	        return strbuf.toString();
		} catch ( final Exception e ) {
			return null;
		}
	}
	
    public static void main(String[] args) throws Exception {
        URL oracle = new URL("http://www.oracle.com/");
        BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            System.out.println(inputLine);
        in.close();
    }
}
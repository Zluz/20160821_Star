package jmr.home.comm;

import java.net.*;
import java.io.*;

public class URLReader {
	
	final String strURL;
	
	public URLReader( final String strURL ) {
		this.strURL = strURL;
	}
	
	public String getContent() {
		try {

			System.out.println( "Opening URL: " + strURL );

			final URL url = new URL( strURL );
	        final InputStreamReader isr = new InputStreamReader(url.openStream());
			final BufferedReader in = new BufferedReader( isr );
	
			final StringBuffer strbuf = new StringBuffer();
	        String strLine;
	        while ((strLine = in.readLine()) != null)
	            strbuf.append( strLine );
	        in.close();
	        
	        return strbuf.toString();
		} catch ( final Exception e ) {
			return "";
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
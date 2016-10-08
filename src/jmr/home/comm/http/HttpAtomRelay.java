package jmr.home.comm.http;

import java.net.URLEncoder;

import jmr.home.api.comm.IConstants;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;

public class HttpAtomRelay implements IAtomConsumer, IConstants {

	@Override
	public void consume( final Atom atom ) {
		
		final String strMessage = atom.toString();
		
		final String strURL = 
				"http://" + HOST__UI_RAP + ":" + PORT__UI_RAP 
					+ HTTP_SERVICE__UI_RAP__SEND + "?text=" 
						+ URLEncoder.encode( strMessage );
		
		final URLReader reader = new URLReader( strURL );
		@SuppressWarnings("unused")
		final String strValue = reader.getContent();

		// POST
//		// see also
//		// http://stackoverflow.com/questions/4205980/java-sending-http-parameters-via-post-method-easily
//		
//		String urlParameters  = "param1=a&param2=b&param3=c";
//		byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
//		int    postDataLength = postData.length;
//		String request        = "http://example.com/index.php";
//		URL    url            = new URL( request );
//		HttpURLConnection conn= (HttpURLConnection) url.openConnection();           
//		conn.setDoOutput( true );
//		conn.setInstanceFollowRedirects( false );
//		conn.setRequestMethod( "POST" );
//		conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
//		conn.setRequestProperty( "charset", "utf-8");
//		conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
//		conn.setUseCaches( false );
//		try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
//		   wr.write( postData );
//		}
		
	}

}

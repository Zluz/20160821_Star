package jmr.home.comm.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map.Entry;

import jmr.home.api.comm.IConstants;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;
import jmr.util.Util;

public class HttpAtomRelay implements IAtomConsumer, IConstants {

	
	public static String enc( final String strUnencoded ) {
		if ( null==strUnencoded ) return "";
		try {
			final String strEncoded = 
						URLEncoder.encode( strUnencoded, Util.UTF8 );
			return strEncoded;
		} catch ( final UnsupportedEncodingException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	
	@Override
	public void consume( final Atom atom ) {
		if ( null==atom ) return;
		if ( !Atom.Type.TO_UI.equals( atom.getType() ) ) return;
		
//		final String strMessage = atom.toString();
		
//		String strMessage = "";
		final StringBuffer strbuf = new StringBuffer();

		strbuf.append( "http://" + HOST__UI_RAP + ":" + PORT__UI_RAP 
					+ HTTP_SERVICE__UI_RAP__SEND + "?" );

		for ( final Entry<String, String> entry : atom.entrySet() ) {
			final String strField = entry.getKey();
			final String strValue = entry.getValue();
			if ( null!=strField && !strField.isEmpty() ) {
				strbuf.append( enc( strField ) );
				strbuf.append( "=" );
				strbuf.append( enc( strValue ) );
				strbuf.append( "&" );
			}
		}
		
//		final String strField = atom.get( VAR_UI_FIELD );
//		final String strValue = atom.get( VAR_UI_VALUE );
//		
//		if ( null==strField ) return;
		
//		final String strMessage = strField + "=" + strValue;
		
//		final String strURL = 
//				"http://" + HOST__UI_RAP + ":" + PORT__UI_RAP 
//					+ HTTP_SERVICE__UI_RAP__SEND + "?text=" 
//						+ URLEncoder.encode( strMessage );
		
		final String strURL = strbuf.toString();
		final URLReader reader = new URLReader( strURL );
		@SuppressWarnings("unused")
		final String strResponse = reader.getContent();

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

package jmr.home.comm;

// from
// http://www.rgagnon.com/javadetails/java-have-a-simple-http-server.html

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

//import static java.nio.charset.StandardCharsets.UTF_8;

import jmr.home.engine.Relay;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;
import jmr.util.Util;

/*
 *	http://localhost:8000/atom?name01=value01&name02=value02&name03=value03
 */


public class HttpServerAtomProducer implements IAtomConsumer {
	
	final static private HttpServerAtomProducer 
			instance = new HttpServerAtomProducer();
	private HttpServer server;
	
	public final static int PORT_HOSTED = 80;
	
//	public final List<String> listPlanets = new LinkedList<>();
	public final Map<String,String> mapPlanets = new HashMap<>();
	
	public HttpServerAtomProducer() {
		Relay.get().registerConsumer( this );
	}
	
	public static HttpServerAtomProducer get() {
		return instance;
	}
	
	final public static String UTF8 = StandardCharsets.UTF_8.name();
	
	public boolean open() {
	    try {
			server = HttpServer.create(new InetSocketAddress( PORT_HOSTED ), 0);
		    server.createContext("/atom", new AtomHandler());
		    server.createContext("/info", new InfoHandler());
		    server.createContext("/get", new GetHandler());
		    server.setExecutor(null); // creates a default executor
		    server.start();
		    return true;
		} catch ( final IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return false;
	}
	
	private static Map<String,String> extractParameters( final URI uri ) {
	    final Map<String, String> map = new LinkedHashMap<String, String>();
	    final String query = uri.getQuery();
	    final String[] pairs = query.split( "&" );
	    for ( final String pair : pairs ) {
	        final int idx = pair.indexOf( "=" );
	        try {
				final String strKey = URLDecoder.decode( pair.substring(0, idx), UTF8 );
				final String strValue = URLDecoder.decode( pair.substring(idx + 1), UTF8 );
				map.put( strKey, strValue );
			} catch ( final UnsupportedEncodingException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return map;
	}
	
	private class AtomHandler implements HttpHandler {
		@Override
		public void handle( final HttpExchange exchange ) throws IOException {

			System.out.println( "Incoming request: " + exchange );
			final InetAddress addrRemote = exchange.getRemoteAddress().getAddress();
			System.out.println( "\tfrom: " + addrRemote );
			System.out.println( "\tURI: " + exchange.getRequestURI() );
			
			final Atom atom = new Atom(	Atom.Type.EVENT, 
										HttpServerAtomProducer.class.getSimpleName(), 
										Long.toString( PORT_HOSTED ) );
			
			// add parameters to atom
			final URI uri = exchange.getRequestURI();
			final Map<String, String> map = extractParameters( uri );
			for ( final Entry<String, String> entry : map.entrySet() ) {
				atom.put( entry.getKey(), entry.getValue() );
			}

			final String strPlanetIP = addrRemote.getHostAddress();
			atom.put( VAR_PLANET_IP, strPlanetIP );
			atom.put( VAR_STAR_IP, Util.getHostIP() );
			
			final String strSerNo = atom.get( VAR_SERIAL_NUMBER );
			if ( null!=strSerNo && !strSerNo.isEmpty() ) {
				mapPlanets.put( strSerNo, strPlanetIP );
			}
			
			// add headers to atom
//			final Headers headers = exchange.getRequestHeaders();
//			
//			for ( final Entry<String, List<String>> entry : headers.entrySet() ) {
//				final String strKey = entry.getKey();
//				final List<String> list = entry.getValue();
//				if ( !list.isEmpty() ) {
//					String strValue = list.get(0);
//					for ( int i=1; i<list.size(); i++ ) {
//						strValue = "\n" + list.get( i );
//					}
//					atom.put( strKey, strValue );
//				}
//			}
			
			Relay.get().consume( atom );

			final String strResponse = "Atom consumed.\n" + atom.report();
			
			System.out.println( strResponse );
			
			exchange.sendResponseHeaders( 200, strResponse.length() );
			final OutputStream os = exchange.getResponseBody();
			os.write( strResponse.getBytes() );
			os.close();
		}
	}
	
	
	

  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/info", new InfoHandler());
    server.createContext("/get", new GetHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class InfoHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      String response = "Use /get to download a PDF";
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
  }

  static class GetHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {

      // add the required response header for a PDF file
      Headers h = t.getResponseHeaders();
      h.add( "Content-Type", "application/pdf" );

      // a PDF (you provide your own!)
      File file = new File ("c:/temp/doc.pdf");
      byte [] bytearray  = new byte [(int)file.length()];
      FileInputStream fis = new FileInputStream(file);
      
      @SuppressWarnings("resource")
      BufferedInputStream bis = new BufferedInputStream(fis);
      bis.read(bytearray, 0, bytearray.length);

      // ok, we are ready to send the response.
      t.sendResponseHeaders(200, file.length());
      OutputStream os = t.getResponseBody();
      os.write(bytearray,0,bytearray.length);
      os.close();
    }
  }



	
  @Override
  public void consume( final Atom atom ) {
	  if ( null==atom ) return;
	  
	  final String strDestSerNo = atom.get( VAR_DEST_SERNO );
	  final String strCommand = atom.get( VAR_COMMAND );
	  if ( mapPlanets.keySet().contains( strDestSerNo ) ) {
		  final String strIP = mapPlanets.get( strDestSerNo );
		  
		  final String strURL = "http://" + strIP + strCommand;
		  final URLReader reader = new URLReader( strURL );
		  reader.getContent();
	  }
  }
  
}
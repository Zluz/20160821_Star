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
import jmr.home.logging.EventType;
import jmr.home.logging.Log;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;
import jmr.util.Util;

/*
 *	http://localhost:8000/atom?name01=value01&name02=value02&name03=value03
 */


public class HttpServerAtomProducer implements IAtomConsumer {
	
	private static final String URL_TEST = "http://127.0.0.1/test";

	final static private HttpServerAtomProducer 
			instance = new HttpServerAtomProducer();
	
	private HttpServer server;
	
	/** When the HTTP server is expected to be accepting requests */
	private boolean bOnline;
	
	public final static int PORT_HOSTED = 80;
	
	public final static int MAX_CONNECTIONS_OPEN = 3;
	
	public static int iConnectionsOpen;
	
//	public final List<String> listPlanets = new LinkedList<>();
	public final Map<String,String> mapPlanets = new HashMap<>();
	
	private final Thread threadHealthMonitor;
	
	public HttpServerAtomProducer() {
		bOnline = false;
		Relay.get().registerConsumer( this );
		
		final String strName = HttpServerAtomProducer.class.getSimpleName() 
						+ " - Server health monitor";
		this.threadHealthMonitor = new Thread( strName ) {
			public void run() {
				try {
					for (;;) {
						Thread.sleep( 1000 );

						if ( bOnline && (
								( iConnectionsOpen>MAX_CONNECTIONS_OPEN )
								|| !testServer() ) ) {
							System.out.println( "HTTP Server must restart." );
							bOnline = false;
							final Thread threadHTTPServerRestart = 
									new Thread( "HTTP server restart" ) {
								@Override
								public void run() {
									final boolean bResult = 
											HttpServerAtomProducer.this.doServerStart();
									System.out.println( 
											"HTTP Server restart result: " + bResult );
								}
							};
							threadHTTPServerRestart.start();
							Thread.sleep( 5000 );
							bOnline = true;
						}

						
//						final HttpServerImpl hsi;
//						if ( server instanceof HttpServerImpl ) {
//							hsi = (HttpServerImpl)server;
//						} else {
//							hsi = null;
//						}
						
						String strMessage = null;
						if ( null==server ) {
							strMessage = "Server is null";
						} else if ( null==server.getAddress() ) {
							strMessage = "Server getAddress() is null";
						}
						
						if ( null!=strMessage ) {
							System.err.println( 
									HttpServerAtomProducer.class.getSimpleName() 
									+ " - " + strMessage );
						}
					}
				} catch ( final InterruptedException e ) {
//					System.out.println( "Interrupted." );
					// just exit
				}
			};
		};
		this.threadHealthMonitor.start();
	}
	
	public static HttpServerAtomProducer get() {
		return instance;
	}
	
	final public static String UTF8 = StandardCharsets.UTF_8.name();
	
	
	// maybe also see (applies to java 6): 
	// http://stackoverflow.com/questions/3519887/sun-java-httpserver-has-a-bug-how-to-fix-it
	private void killServer() {
		if ( null==server ) return;
		
    	System.out.println( "Stopping server." );
		Log.log( EventType.SERVICE_HTTP_STOPPING, null );

		try {
			server.stop( 0 ); // does not always stop server?

	    	try {
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.log( EventType.SERVICE_HTTP_STOPPED, null );

		} catch ( final Exception e ) {
			// handle it somehow?
			e.printStackTrace();
		}
	}
	
	
	private boolean testServer() {
		final String strTestURL = URL_TEST;
		final URLReader reader = new URLReader( strTestURL );
		final String strContent = reader.getContent();
		final boolean bResult = ( null!=strContent && !strContent.isEmpty() );
		if ( !bResult ) {
			System.err.println( "HTTP Server is unresponsive. Restarting." );
		}
		return bResult;
	}
	
	
	public boolean doServerStart() {
	    try {
	    	killServer();

			Log.log( EventType.SERVICE_HTTP_STARTING, null );

	    	System.out.print( "Starting server..." );
			server = HttpServer.create( 
						new InetSocketAddress( PORT_HOSTED ), 0 );
		    server.createContext("/atom", new AtomHandler());
		    server.createContext("/test", new TestHandler());
		    server.createContext("/get", new GetHandler());
		    server.setExecutor(null); // creates a default executor
		    server.start();
	    	System.out.println( "Done." );
	    	bOnline = true;
			Log.log( EventType.SERVICE_HTTP_READY, null );
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
	    if ( null!=query && !query.isEmpty() ) {
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
	    }
	    return map;
	}
	
	private class AtomHandler implements HttpHandler {
		@SuppressWarnings("unused")
		@Override
		public void handle( final HttpExchange exchange ) {
			if ( null==exchange ) return;
			
			final InetAddress addrRemote = exchange.getRemoteAddress().getAddress();
			final URI uri = exchange.getRequestURI();
			final String strURI = exchange.getRequestURI().toString();

			final String[] response = { 
					Integer.toString( strURI.getBytes().length ) 
					+ " byte(s) received." };
			
			final Thread threadProcessAtomRequest = new Thread( "HTTP Request - Atom" ) {
				public void run() {
					Log.log( EventType.SERVICE_HTTP_HANDLE_ATOM, null );
					
					try {
						iConnectionsOpen++;
						
						if ( iConnectionsOpen>MAX_CONNECTIONS_OPEN ) return;

//						final String strResponse;

						if ( !addrRemote.isLoopbackAddress() ) {
			
							System.out.println( "Incoming request: " + exchange );
							System.out.println( "\tfrom: " + addrRemote );
							System.out.println( "\tURI: " + uri );
							
							final Atom atom = new Atom(	Atom.Type.EVENT, 
														HttpServerAtomProducer.class.getSimpleName(), 
														Long.toString( PORT_HOSTED ) );
//							String strAtomName = 
//									HttpServerAtomProducer.class.getSimpleName();
							
							// add parameters to atom
							final Map<String, String> map = extractParameters( uri );
							for ( final Entry<String, String> entry : map.entrySet() ) {
								final String strName = entry.getKey();
								final String strValue = entry.getValue();
								atom.put( strName, strValue );
//								if ( VAR_SERIAL_NUMBER.equals( strName ) ) {
//									strAtomName = strValue;
//								}
							}
							
//							atom.setName( strAtomName );
				
							final String strPlanetIP = addrRemote.getHostAddress();
							atom.put( VAR_PLANET_IP, strPlanetIP );
							atom.put( VAR_STAR_IP, Util.getHostIP() );
							
							final String strSerNo = atom.get( VAR_SERIAL_NUMBER );
							if ( null!=strSerNo && !strSerNo.isEmpty() ) {
								atom.setName( strSerNo );
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
							
							final Thread thread = new Thread( "Consume atom from HTTP request" ) {
								@Override
								public void run() {
									Relay.get().consume( atom );
								}
							};
							thread.start();
				
							response[0] = "Atom consumed.\n" + atom.report();
							
//							System.out.println( strResponse );
				
						} else {

							response[0] = "Loopback test acknowledged.";

//							try ( final OutputStream os = exchange.getResponseBody() ) {
//								
////								exchange.sendResponseHeaders( 200, strResponse.length() );
////								os.write( strResponse.getBytes() );
////								os.close();
//								
//							} catch ( final IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
						}
					} catch ( final Exception e ) {
						e.printStackTrace();
					}
				}
			};
			
			if ( true ) { // threaded
				threadProcessAtomRequest.start();
			} else {
				threadProcessAtomRequest.run();
			}
			
			try ( final OutputStream os = exchange.getResponseBody() ) {
				
				final byte[] bytes = response[0].getBytes();
				exchange.sendResponseHeaders( HTTP_OK_STATUS, bytes.length );
				os.write( bytes );
				os.close();

			} catch ( final IOException e ) {
//					final String strMessage = e.toString();
				String strMessage = e.getMessage();
				if ( null==strMessage ) strMessage = e.toString();
				if ( ( "An established connection was aborted by the "
						+ "software in your host machine" ).equals( strMessage ) ) {
					// ignore
				} else if ( ( "insufficient bytes written to stream" ).equals( strMessage ) ) {
					// ignore
				} else if ( ( "stream is closed" ).equals( strMessage ) ) {
					// ignore
				} else if ( ( "java.nio.channels.AsynchronousCloseException" ).equals( strMessage ) ) {
					// ignore
				} else if ( ( "java.nio.channels.ClosedChannelException" ).equals( strMessage ) ) {
					// Suppressed: java.io.IOException: insufficient bytes written to stream
					// ignore
				} else {
					System.err.println( "Error message: " + strMessage );
					e.printStackTrace();
				}
			} finally {
				iConnectionsOpen--;

				try {
					exchange.close();
				} catch ( final Exception e ) {
					Log.log( "Exception while closing HttpExchange", e );
				}
			}
		}
	}
	
	private static final int HTTP_OK_STATUS = 200;
	

  static class GetHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {

      // add the required response header for a PDF file
      Headers h = t.getResponseHeaders();
      h.add( "Content-Type", "application/pdf" );

      // a PDF (you provide your own!)
      File file = new File("c:/temp/doc.pdf");
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


  static class TestHandler implements HttpHandler {
    public void handle( final HttpExchange exchange ) throws IOException {
    	if ( null==exchange ) return;
    	
    	try ( final OutputStream os = exchange.getResponseBody() ) {
    	
			final String response = "Test/Response";
			exchange.sendResponseHeaders( 200, response.getBytes().length );
			os.write( response.getBytes() );
			os.close();
			
    	} finally {
    		try {
				exchange.close();
			} catch ( final Exception e ) {
				Log.log( "Exception while closing HttpExchange", e );
			}
    	}
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
  


  public static void main(String[] args) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/test", new TestHandler());
    server.createContext("/get", new GetHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  
  
}
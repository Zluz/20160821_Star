package jmr.home.comm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import jmr.home.apps.AtomTree;
import jmr.home.engine.Relay;
import jmr.home.model.Atom;

public class InputStreamAtomProducer {


	private final InputStream input;
	private AtomTree tree;
	private final String strPort;
	
	private final Thread thread;
	
	private boolean bOpen;

	private int iGoodBytes = 0;
	private int iBadBytes = 0;
	
	private int iCount = 0;
	
	private final StreamBuffer sb = new StreamBuffer();

	public InputStreamAtomProducer(	final InputStream input,
									final String strPort,
									final AtomTree tree ) {
		this.input = input;
		this.tree = tree;
		this.strPort = strPort;
		
		this.thread = createThread();
	}


	public int getBadBytes() {
		return iBadBytes;
	}
	
	public int getGoodBytes() {
		return iGoodBytes;
	}

	
	public void open() {
		this.bOpen = true;
		this.thread.start();
	}
	
	public void close() {
		this.bOpen = false;
	}

	public static boolean isPureAscii(String v) {
		byte bytearray[] = v.getBytes();
		CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
		try {
			CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
			r.toString();
		} catch (CharacterCodingException e) {
//			System.out.print( "[BAD]" );
			return false;
		}
//		System.out.print( "[GOOD]" );
		return true;
	}


	public Thread createThread() {
		final Thread thread = new Thread( InputStreamAtomProducer.class.getSimpleName() ) {
			public void run() {
				try {
					do {
						Thread.sleep(10);
						
						byte[] buffer = new byte[10240];
						int len = -1;

						while ((len = input.read(buffer)) > -1) {
							final String text = new String( buffer, 0, len );
							
//							for ( final byte b : text.getBytes() ) {
//								final char c = (char)b;
//								countByte( c );
//							}
							if ( !text.isEmpty() ) {
								if ( isPureAscii( text ) ) {
									iGoodBytes = iGoodBytes + text.length();
								} else {
									iBadBytes = iBadBytes + 1;
//									System.err.print( "ERROR++" );
								}
//								synchronized( strbuf ) {
//									strbuf.append( text );
//									checkBuffer();
//								}
								if ( bOpen ) {
									sb.append( text );
									checkBuffer();
								}
							}
							
							if ( bOpen ) {
//								System.out.print( text );
							} else {
								return;
							}
						}

					} while ( bOpen );
				} catch ( final InterruptedException | IOException e ) {
					bOpen = false;
					// just exit
				}
			};
		};
		return thread;
	}
	
	
	private void checkBuffer() {
		String strPopped;
		do {
			strPopped = sb.pop( '{' );
			if ( null!=strPopped && strPopped.contains( "{" ) ) {
				process( strPopped );
			}
		} while ( null!=strPopped );
	}
	

	private void process( final String string ) {
		if ( null==string ) return;
		if ( string.trim().isEmpty() ) return;
		
//		System.out.println( string );
		
		final Atom atom = new Atom( Atom.Type.EVENT, this.strPort, this.strPort );
		for ( final String strLine : string.split( "\n" ) ) {
			atom.load( strLine );
		}
		
		iCount = iCount + 1;
		this.tree.setAtom( atom, Integer.toString( iCount ) );

		Relay.get().consume( atom );
	}


	public void accept( final AtomTree atomtree ) {
		this.tree = atomtree;
	}

}

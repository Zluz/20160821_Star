package jmr.home.comm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import jmr.home.apps.AtomTree;
import jmr.home.model.Atom;

public class AtomGenerator {

	private final InputStream input;
	private AtomTree tree;
	private final String strPort;
	
	private final Thread thread;
	
	private boolean bOpen;

	private int iGoodBytes = 0;
	private int iBadBytes = 0;
	
	private int iCount = 0;
	
//	public final StringBuffer strbuf = new StringBuffer();
	private final StreamBuffer sb = new StreamBuffer();

	public AtomGenerator(	final InputStream input,
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
		final Thread thread = new Thread( AtomGenerator.class.getSimpleName() ) {
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
	
	
//	private String popBuffer() {
//		final String strText = strbuf.toString();
//		
//		System.out.println( ">>> " + formatted( strText ) );
//
//		int iPosEnd = strText.indexOf( "}" );
//		
//		if ( iPosEnd<0 ) {
//			return null;
//		} else {
//			final String strPopped = strText.substring( 0, iPosEnd+1 );
//			final String strRemaining = strText.substring( iPosEnd+1 );
//
//			strbuf.setLength( 0 );
//			strbuf.append( strRemaining );
//			System.out.println( "  < " + formatted( strPopped ) );
//			return strPopped;
//		}
//	}
	

//	private String popBuffer_() {
//		String strText = strbuf.toString();
//		
//		System.out.println( ">>> " + formatted( strText ) );
//		
//		int iPosStart = strText.indexOf( "{" );
//		if ( iPosStart<0 ) {
//			return null; // nothing started yet..
//		}
//		
//		int iPosEnd = strText.indexOf( "}", iPosStart );
//		
////		if ( iPosEnd < iPosStart ) {
////			// discard incomplete
////			strText = strText.substring( iPosStart );
////			strbuf.setLength( 0 );
////			strbuf.append( strText );
////			
////			iPosEnd = strText.indexOf( "}" );
////		}
//
//		if ( iPosEnd<0 ) {
//			return null; // nothing ended yet..
//		}
//
//		iPosEnd++;
//		final String strPopped = strText.substring( iPosStart, iPosEnd );
//		final String strRemaining = strText.substring( iPosEnd );
//		
//		strbuf.setLength( 0 );
//		strbuf.append( strRemaining );
//		
//		System.out.println( "  < " + formatted( strPopped ) );
//		return strPopped;
//	}
	
	private void checkBuffer() {
		String strPopped;
		do {
//			strPopped = popBuffer();
			strPopped = sb.pop( '{' );
			if ( null!=strPopped && strPopped.contains( "{" ) ) {
				process( strPopped );
			}
		} while ( null!=strPopped );
	}
	
//	private void _checkBuffer() {
//		String strText = strbuf.toString();
//		
//		int iPosStart = strText.indexOf( "{" );
//		if ( iPosStart<0 ) {
//			return; // nothing started yet..
//		}
//		
//		int iPosEnd = strText.indexOf( "}" );
//		
//		if ( iPosEnd < iPosStart ) {
//			// discard incomplete
//			strText = strText.substring( iPosStart );
//			strbuf.setLength( 0 );
//			strbuf.append( strText );
//			
//			iPosEnd = strText.indexOf( "}" );
//		}
//		
//		if ( iPosEnd<0 ) {
//			return; // nothing ended yet..
//		}
//		
//		do {
//			final String strSegment = strText.substring( iPosStart, iPosEnd+1 );
//			
//			process( strSegment );
//			
//			iPosStart = strText.indexOf( "{", iPosEnd );
//			iPosEnd = strText.indexOf( "}", iPosStart );
//			
////			strText = strText.substring( iPosEnd+1 );
//
//		} while ( iPosStart>=0 && iPosEnd>=0 );
//		
//		strbuf.setLength( 0 );
//		if ( iPosStart<0 ) {
//			strbuf.append( strText );
//		} else {
//			strbuf.append( strText.substring( iPosStart ) );
//		}
//	}


	private void process( final String string ) {
		if ( null==string ) return;
		if ( string.trim().isEmpty() ) return;
		
//		System.out.println( string );
		
		final Atom atom = new Atom( Atom.Type.EVENT, this.strPort );
		for ( final String strLine : string.split( "\n" ) ) {
			atom.load( strLine );
		}
		
		iCount = iCount + 1;
		this.tree.setAtom( atom, Integer.toString( iCount ) );
	}


	public void accept( final AtomTree atomtree ) {
		this.tree = atomtree;
	}

	
}

package jmr.home.comm;

public class StreamBuffer {

	private final StringBuffer strbuf = new StringBuffer();
	
//	String strbuf = "";
	
	public synchronized void append( final String text ) {
		strbuf.append( text );
//		strbuf = strbuf + text;
		
//		System.out.println( Integer.toHexString(hashCode()) + "+++ " + formatted( strbuf.toString() ) );
	}
	
	
	public synchronized String pop( final char c ) {
		final String strText = strbuf.toString();
		
//		System.out.println( Integer.toHexString(hashCode()) + ">>> " + formatted( strText ) );

		int iPosEnd = strText.indexOf( "}" );
		
		if ( iPosEnd<0 ) {
			return null;
		} else {
			final String strPopped = strText.substring( 0, iPosEnd+1 );
			final String strRemaining = strText.substring( iPosEnd+1 );

//			strbuf = strRemaining;
			strbuf.setLength( 0 );
			strbuf.append( strRemaining );
			
//			System.out.println( Integer.toHexString(hashCode()) + ">>> " + formatted( strText ) );
//			System.out.println( Integer.toHexString(hashCode()) + "  < " + formatted( strPopped ) );
			return strPopped;
		}
	}

	
	

	@SuppressWarnings("unused")
	private String formatted( final String strSource ) {
		if ( null==strSource ) return "";
		
//		String strTarget = strSource;
		char[] chars = strSource.toCharArray();
		try {
			for ( int i=0; i<strSource.length(); i++ ) {
				if ( "\n\t".indexOf( chars[i] )>-1 ) {
					chars[i] = ' ';
				} else if ( "\r".indexOf( chars[i] )>-1 ) {
//					System.arraycopy( chars, i+1, chars, i, chars.length-i-1 );
					chars[i] = ' ';
				}
//				if ( !Atom.isValidChar( chars[i] ) ) {
//					chars[i] = '_';
//				}
			}
//			strTarget = strTarget.replaceAll( "\\n", "\\" );
//			strTarget = strTarget.replaceAll( "\\r", "\\" );
		} catch ( final Exception e ) {
			// ignore
		}
		return "[" + new String( chars ) + "]";
	}

	
	
}

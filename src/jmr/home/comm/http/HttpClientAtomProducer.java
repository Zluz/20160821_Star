package jmr.home.comm.http;

public class HttpClientAtomProducer {

	
	final String strHost;
	final long lInterval;
	
	public HttpClientAtomProducer(	final String strHost,
									final long lInterval ) {
		this.strHost = strHost;
		this.lInterval = lInterval;
	}

	
	private void init() {
		// set all to digital pins read
		for ( int i=2; i<14; i++ ) {
			final String strURL = 
					"http://" + strHost + "/arduino/mode/" + i + "/input";
			final URLReader reader = new URLReader( strURL );
			@SuppressWarnings("unused")
			final String strValue = reader.getContent();
			// ex: "Pin D4 configured as INPUT!"
//			System.out.println( strURL + "\t" + strValue );
		}
	}
	
	private void scan() {
		
		for ( int iPin=2; iPin<14; iPin++ ) {

			final String strURL = 
					"http://" + strHost + "/arduino/digital/" + iPin;
			final URLReader reader = new URLReader( strURL );
			final String strValue = reader.getContent();

//			System.out.println( strURL + "\t" + strValue );
			
			if ( null!=strValue && !strValue.isEmpty() ) {
				try {
					final String strTrimmed = strValue.trim();
					final int iLastSpace = strTrimmed.lastIndexOf( ' ' );
					final String strNumber;
					if ( iLastSpace>0 ) {
						strNumber = strTrimmed.substring( iLastSpace+1 );
					} else {
						strNumber = strTrimmed;
					}
					final int iValue = Integer.parseInt( strNumber );
					
					System.out.println( "Pin D" + iPin + " is " + iValue );
				} catch ( final NumberFormatException e ) {
					// ignore
					System.err.println( "Bad number result from " + strURL );
				}
			}
		}
		

		for ( int iPin=0; iPin<6; iPin++ ) {

			final String strURL = 
					"http://" + strHost + "/arduino/analog/" + iPin;
			final URLReader reader = new URLReader( strURL );
			final String strValue = reader.getContent();

//			System.out.println( strURL + "\t" + strValue );
			
			if ( null!=strValue && !strValue.isEmpty() ) {
				try {
					final String strTrimmed = strValue.trim();
					final int iLastSpace = strTrimmed.lastIndexOf( ' ' );
					final String strNumber;
					if ( iLastSpace>0 ) {
						strNumber = strTrimmed.substring( iLastSpace+1 );
					} else {
						strNumber = strTrimmed;
					}
					final int iValue = Integer.parseInt( strNumber );
					
					System.out.println( "Pin A" + iPin + " is " + iValue );
				} catch ( final NumberFormatException e ) {
					// ignore
					System.err.println( "Bad number result from " + strURL );
				}
			}
		}
	}
	
	
	public static void main( final String[] args ) {
		final String strURLBase = "192.168.1.9";
		final HttpClientAtomProducer 
				hcap = new HttpClientAtomProducer( strURLBase, 1 );
		hcap.init();
		hcap.scan();
	}
	
}

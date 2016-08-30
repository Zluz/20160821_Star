package jmr.home.comm;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.RXTXVersion;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import jmr.home.apps.AtomTree;
import jmr.home.engine.Relay;
import jmr.home.model.Atom;
import jmr.home.model.IAtomMetadata;

//       1         2         3         4         5         6         7         8
//345678901234567890123456789012345678901234567890123456789012345678901234567890


public class SerialConnector implements IAtomMetadata {


	public enum ComBaud {	_9600,	// default, fall-back
							_115200, // fast 
							_19200, 
							_38400, 
							_57600, 
							UNKNOWN;
	
		private final int iBaud;
		
		private ComBaud() {
			int i = 0;
			try {
				i = Integer.parseInt( this.name().substring( 1 ) );
			} catch ( final Exception e ) {
				i = 0;
			}
			this.iBaud = i;
		}
		
		public int getRate() {
			return this.iBaud;
		}
		
		@Override
		public String toString() {
			return Integer.toString( this.iBaud );
		}
							
	};


	public final static int SERIAL_DATA_BITS = SerialPort.DATABITS_8;
	public final static int SERIAL_STOP_BITS = SerialPort.STOPBITS_1;
	public final static int SERIAL_PARITY = SerialPort.PARITY_NONE;
	
	
	public final CommPortIdentifier port;
	
	public ComBaud baud;
	
	public AtomTree atomtree;
	
	public final StringBuffer strbuf;
	
	public OutputStream streamOutput;
//	public InputStream streamInput;
	
	private SerialPort serialPort;
	
	private boolean bOpen;

	private Thread threadReader;

	private Thread threadWriter;
	private InputStreamAtomProducer ag;

	
	public SerialConnector( final CommPortIdentifier port ) {
		this( port, ComBaud._9600 );
	}

	public SerialConnector(	final CommPortIdentifier port,
							final ComBaud baud ) {
		assert( null!=port );
		this.port = port;
		this.baud = baud;
		strbuf = new StringBuffer();
		
		Relay.get().consume( new Atom( Atom.Type.SYSTEM, 
					"SerialConnector ctor", this.port.getName() ) );
	}


	
	public boolean attachToPort() throws Exception {

		Relay.get().consume( new Atom( Atom.Type.SYSTEM, 
				"SerialConnector attachToPort()", this.port.getName() ) );

		if ( port.isCurrentlyOwned() ) {
			return false;
		} else {
			final CommPort commPort = 
					port.open( this.getClass().getName(), 2000 );

			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;

				this.bOpen = true;
				
			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
		return true;
	}
	
	public InputStreamAtomProducer getAtomGenerator() {
		return this.ag;
	}
	
	
	public boolean adjustBaudRate( final ComBaud baud ) throws IOException {
		if ( null==serialPort ) return false;
		if ( null==baud ) return false;
		if ( baud.getRate()<=0 ) return false;

		Relay.get().consume( new Atom( Atom.Type.SYSTEM, 
				"SerialConnector adjustBaudRate()", this.port.getName() ) );

		this.ag = null;
		
		try {
			serialPort.setSerialPortParams(	baud.getRate(),
											SERIAL_DATA_BITS, 
											SERIAL_STOP_BITS,
											SERIAL_PARITY );
//			this.iBadBytes = 0;
//			this.iGoodBytes = 0;

			final InputStream input = serialPort.getInputStream();
			this.ag = new InputStreamAtomProducer( input, port.getName(), this.atomtree );
			this.ag.open();

			if ( null==streamOutput ) {
				streamOutput = serialPort.getOutputStream();
				threadWriter = new Thread( new SerialWriter(streamOutput) );
				threadWriter.start();
			}
			
			return true;
		} catch ( final UnsupportedCommOperationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}
	
	
	public void close() {
		this.serialPort = null;
		
		this.ag.close();
		
		this.bOpen = false;

		if ( null!=threadWriter ) {
			try {
				threadWriter.join( 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if ( null!=threadReader ) {
			try {
				threadReader.join( 1000 );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	
	public boolean send( final String text ) {
		try {
			this.streamOutput.write( (text + "\n").getBytes() );
			return true;
		} catch ( final IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public StringBuffer popReadBuffer() {
		synchronized( this.strbuf ) {
			final StringBuffer strbufPopped = new StringBuffer( this.strbuf );
			this.strbuf.setLength( 0 );
			return strbufPopped;
		}
	}
	
	
	/**
	 * Input from standard in, send to serial port. 
	 */
	private class SerialWriter implements Runnable {
		final OutputStream out;

		public SerialWriter( final OutputStream out ) {
			this.out = out;
		}

		public void run() {
			try {
				int c = 0;
				while ((c = System.in.read()) > -1) {
					this.out.write(c);
					if ( !bOpen ) return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void attachAtomTree( final AtomTree atomtree ) {
		this.atomtree = atomtree;
	}


	public static List<CommPortIdentifier> getAvailablePorts() {
		final List<CommPortIdentifier> list = new LinkedList<CommPortIdentifier>();
		final Enumeration<?> enumeration = CommPortIdentifier.getPortIdentifiers();
		for ( ; enumeration.hasMoreElements(); ) {
			final Object objPort = enumeration.nextElement();
			if ( objPort instanceof CommPortIdentifier ) {
				CommPortIdentifier cpiPort = (CommPortIdentifier)objPort;
//				System.out.println( "\t" + cpiPort.getName() );
				list.add( cpiPort );
			} else {
				System.out.println( "\tUnknown port type: " + objPort );
			}
		}
		return list;
	}
	

	public static Atom getVersionInfo() {
		final String strClassName = SerialConnector.class.getSimpleName();
		final Atom atom = new Atom( Atom.Type.SYSTEM, strClassName, null );
		atom.put( "RXTX.Version", RXTXVersion.getVersion() );
		
		final Enumeration<?> enumeration = CommPortIdentifier.getPortIdentifiers();
		System.out.println( "Ports identified:" );
		String strAvailablePorts = "";
		for ( ; enumeration.hasMoreElements(); ) {
			final Object objPort = enumeration.nextElement();
			if ( objPort instanceof CommPortIdentifier ) {
				CommPortIdentifier cpiPort = (CommPortIdentifier)objPort;
				
				final String strName = cpiPort.getName();

				System.out.println( "\t" + strName );
				if ( strAvailablePorts.isEmpty() ) {
					strAvailablePorts = strName;
				} else if ( !strAvailablePorts.contains( strName) ) {
					strAvailablePorts = strAvailablePorts
							+ Atom.VAL_FIELD_DELIM + strName;
				}
				// port type 2 is LPT ?
			} else {
				System.out.println( "\tUnknown port type: " + objPort );
			}
		}
		atom.put( "RXTX.Ports", strAvailablePorts );
		
		return atom;
	}
	
	@Override
	public Atom getMetadata() {
		return getVersionInfo();
	}
	
	public static void initialize() {
		System.out.println( "Initializing Serial Library.." );
		System.out.println( "Version: " + RXTXVersion.getVersion() );
		
		final Enumeration<?> enumeration = CommPortIdentifier.getPortIdentifiers();
		System.out.println( "Ports identified:" );
		for ( ; enumeration.hasMoreElements(); ) {
			final Object objPort = enumeration.nextElement();
			if ( objPort instanceof CommPortIdentifier ) {
				CommPortIdentifier cpiPort = (CommPortIdentifier)objPort;
				System.out.println( "\t" + cpiPort.getName() );
				// port type 2 is LPT ?
			} else {
				System.out.println( "\tUnknown port type: " + objPort );
			}
		}
	}
	
	@SuppressWarnings("unused")
	public static void main( String[] args ) {
		
		initialize();
		
		if ( true ) return;
		
		try {
			CommPortIdentifier port = getAvailablePorts().get(0);
			final SerialConnector connector = 
					new SerialConnector( port, ComBaud._115200 );
	//		connector.connect();
			
			final Thread threadMessage = new Thread() {
				@Override
				public void run() {
					do {
						final String text = new Date().toString() + "\n";
						connector.send( text );
						try {
							Thread.sleep( 2000 );
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} while ( true );
				}
			};
			threadMessage.start();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

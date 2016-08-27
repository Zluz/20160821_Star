package jmr.integrate.usbdeview;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jmr.home.engine.Relay;
import jmr.home.model.Atom;
import jmr.home.model.IAtomValues;
import jmr.util.Util;

public class USBDeview {

	public final static String STR_EXEC_PATH = 
			"C:\\Development\\Git\\Star__20160821\\files\\bin\\USBDeview.exe";
	
	public final static String REPORT_NAME_ATOM_NAME = "Instance ID";
	public final static String REPORT_NAME_DEVICE_PORT = "Drive Letter";
	
	public final static String REPORT_NAME_DEVICE_ID = "Driver Description";
	public final static String REPORT_VALUE_DEVICE_REGEX = ".*Arduino.*";
	
	public final static String ATOM_PREFIX = "USBDeview";
	
	private static File fileTemp;
	
	private static USBDeview instance;
	
	private final int iInterval;
	private boolean working;

	private final TreeSet<String> setSerialPorts = new TreeSet<String>(); 
	
	private final TreeSet<String> setSerialPortsLast = new TreeSet<String>();

	private USBDeview(	final int iInterval ) {
		this.iInterval = iInterval;
		try {
			fileTemp = File.createTempFile( "jmr_usbdeview_", ".tmp" );
			fileTemp.deleteOnExit();
			this.working = true;
		} catch (IOException e) {
			this.working = false;
		}
	}

	
	public static USBDeview get() {
		return instance;
	}
	

	public boolean call() {
		if ( !working ) return false;
		
		final String strCommand = 
				STR_EXEC_PATH + " /stext \"" + fileTemp.getAbsolutePath() + "\"";
		try {
			fileTemp.delete();
			final Process process = Runtime.getRuntime().exec( strCommand );
			process.waitFor();

			final String strOutput = Util.getTextFromFile( fileTemp );
			fileTemp.delete();
			int iDeviceIndex = 1;
			if ( null!=strOutput ) {
				setSerialPorts.clear();
				for ( final String strBlock : strOutput.split( 
						"==================================================" ) ) {
					boolean bArduino = false;
					String strPort = null;
					final Atom atom = new Atom(	Atom.Type.EVENT, 
												"USB Device " + iDeviceIndex, 
												"USBDeview" );
					for ( final String strLine : strBlock.split( "\n" ) ) {
						if ( strLine.contains( ":" ) ) {
							final String[] strParts = strLine.split( ":" );
							final String strName = strParts[0].trim();
							final String strValue = strParts[1].trim();
							atom.put( strName, strValue );
							if ( REPORT_NAME_ATOM_NAME.equals( strName ) ) {
								atom.setName( ATOM_PREFIX + "." + strValue );
							} else if ( isDevice( strName, strValue ) ) {
								bArduino = true;
							} else if ( REPORT_NAME_DEVICE_PORT.equals( strName ) ) {
								strPort = strValue;
							}
						}
					}
					if ( bArduino && null!=strPort ) {
						setSerialPorts.add( strPort );
					}
					if ( atom.size() > 3 ) {
						Relay.get().consume( atom );
						iDeviceIndex = iDeviceIndex + 1;
					}
				}
				
				System.out.println( "Ports found in USBDeview scan: " + setSerialPorts );
				
				if ( !setSerialPorts.equals( setSerialPortsLast ) ) {
					System.out.println( "Ports found changed." );
					
					final Atom atomPortChange = new Atom( 
							Atom.Type.INVOKE, IAtomValues.VAL_INVOKE_RESCAN_PORTS, "" );
					Relay.get().consume( atomPortChange );
				}
				setSerialPortsLast.clear();
				setSerialPortsLast.addAll( setSerialPorts );
				
			} else {
				return false;
			}
			
			return true;
		} catch ( final IOException e ) {
			return false;
		} catch ( final InterruptedException e ) {
			return false;
		}
	}

	
	private static boolean isDevice(	final String strName,
										final String strValue ) {
		if ( null==strValue ) return false;
		if ( !REPORT_NAME_DEVICE_ID.equals( strName ) ) return false;
		
		if ( Pattern.matches( REPORT_VALUE_DEVICE_REGEX, strValue ) ) {
			return true;
		} else {
			return false;
		}
	}
	

	public static boolean initialize( final int iInterval ) {
		if ( null!=instance ) return false;
		instance = new USBDeview( iInterval );
		if ( instance.working ) {
			
			final Thread thread = new Thread( "USBDeview monitor" ) {
				@Override
				public void run() {
					try {
						do {
							instance.call();
							Thread.sleep( instance.iInterval * 1000 );
						} while (true);
					} catch ( final InterruptedException e ) {
						// just exit
					}
				}
			};
			thread.start();
			
			return true;
		} else {
			instance = null;
			return false;
		}
	}
	
	
	
	public static void main( final String[] args ) {
		USBDeview.initialize( 1 );
		USBDeview.instance.call();
	}

}

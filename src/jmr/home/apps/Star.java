package jmr.home.apps;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import gnu.io.CommPortIdentifier;
import jmr.home.comm.HttpServerAtomProducer;
import jmr.home.comm.InputStreamAtomProducer;
import jmr.home.comm.SerialConnector;
import jmr.home.comm.SerialConnector.ComBaud;
import jmr.home.engine.Processor;
import jmr.home.engine.Relay;
import jmr.home.model.SystemAtoms;
import jmr.util.Util;

public class Star {
	
	private static final String TIMESTAMP = "HH:mm:ss.SSS";

	public static final boolean ENABLE_COM_PORT_SCANNING = false;
	
	
	public final static Display display = new Display();
	
	private final Shell shell;

	private PortTree porttree;
	private AtomTree atomtree;
	
	public static Star instance;

	private StyledText txtLog;

	public Star() {

		SerialConnector.initialize();
		
		this.shell = this.buildUI();
		
		this.shell.addShellListener( new ShellAdapter() {
			@Override
			public void shellClosed( final ShellEvent e ) {
				log( "Event: Shell.shellClosed()" );
				shell.dispose();
				log( "Event: Shell.shellClosed() - shell disposed." );
				porttree.closeConnectors();
			}
		});
		
		instance = this;
		log("");

		log( "Star initialized." );
		log( "Shell size: " + this.shell.getSize() );
		
		if ( ENABLE_COM_PORT_SCANNING ) {
			startPortMonitor();
		}
		
		this.atomtree.setAtom( SystemAtoms.generateJavaProperties(), "(system)" );
		this.atomtree.setAtom( SystemAtoms.generateEnvironmentVariables(), "(system)" );
		this.atomtree.setAtom( SerialConnector.getVersionInfo(), "(system)" );
		
		Relay.get();
		
		HttpServerAtomProducer.get().open();
		
		// disable for now.
//		USBDeview.initialize( 1 );
//		USBDeview.get().call();
	}
	
	
	public void startPortMonitor() {
		final Thread threadPortMonitor = new Thread() {
			@Override
			public void run() {
				try {
					do {
						Star.this.scanPlanets();
						Thread.sleep( 10000 );
					} while ( true );
				} catch ( final InterruptedException e ) {
					// exit
				}
			}
		};
		threadPortMonitor.start();
	}
	
	
	public void open() {
		this.shell.open();
	}
	
	public Shell getShell() {
		return this.shell;
	}
	
	public static String getVersion() {
		return "0.01/development";
	}
	
	private Shell buildUI() {
		final Shell shell = new Shell( display, SWT.SHELL_TRIM );
		
		shell.setLayout( new GridLayout( 8, true ) );
		
		// [ports-planets] [planet/system event detail] [planet/system log] 
		
		final Composite compLeft = new Composite( shell, SWT.NONE );
		final GridData gcLeft = new GridData( GridData.FILL, GridData.FILL, true, true );
		gcLeft.horizontalSpan = 2;
		gcLeft.heightHint = 300;
		compLeft.setLayoutData( gcLeft );
		compLeft.setLayout( new FillLayout() );
		
		final Composite compCenter = new Composite( shell, SWT.NONE );
		final GridData gcCenter = new GridData( GridData.FILL, GridData.FILL, true, true );
		gcCenter.horizontalSpan = 3;
		gcCenter.widthHint = 200;
		compCenter.setLayoutData( gcCenter );
		compCenter.setLayout( new FillLayout() );

		final Composite compRight = new Composite( shell, SWT.NONE );
		final GridData gcRight = new GridData( GridData.FILL, GridData.FILL, true, true );
		gcRight.horizontalSpan = 3;
		gcRight.widthHint = 100;
		compRight.setLayoutData( gcRight );
		compRight.setLayout( new FillLayout() );
		
		porttree = new PortTree( compLeft );
		Relay.get().registerConsumer( porttree );
		atomtree = new AtomTree( compRight );
		Relay.get().registerConsumer( atomtree );
		
		new Processor( porttree, atomtree );
		Relay.get().registerConsumer( Processor.getProcessor() );

		txtLog = new StyledText( compCenter, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY );
	    
	    shell.pack();
	    
		final String strTitle = Star.class.getSimpleName() + " - " + Util.getHostIP();
		shell.setText( strTitle );

	    
		return shell;
	}


	public static void log(	final String strMessage,
							final boolean bNewLine ) {
		
		final Date date = new Date();
		final SimpleDateFormat format = new SimpleDateFormat( TIMESTAMP, Locale.ENGLISH);
		final String strTimestamp = format.format( date );
		
		System.out.println( strTimestamp + "  " + strMessage );
		
		if ( null!=instance ) {
			display.asyncExec( new Runnable() {
				@Override
				public void run() {
					final StyledText txt = instance.txtLog;
					final StringBuffer strbuf = new StringBuffer( txt.getText() );
					final boolean bEndsInNewLine = 
								strbuf.toString().endsWith( Text.DELIMITER );
					if ( bEndsInNewLine ) {
						strbuf.append( strTimestamp + "  " ); 
					}
					strbuf.append( strMessage );
					if ( bNewLine ) {
						strbuf.append( Text.DELIMITER );
					}
					// now crop
					String strNewText = strbuf.toString();
					int iPos = strNewText.length() - 10000;
					if ( iPos>0 ) {
						iPos = strNewText.indexOf( Text.DELIMITER, iPos );
						strNewText = strNewText.substring( iPos );
					}
					// apply the text
					txt.setText( strNewText );
					txt.setSelection( strNewText.length() );
				}				
			});
		}
	}
	
	public static void log( final String strMessage ) {
		log( strMessage, true );
	}
	
	private void scanPlanets() {
//		log( "Scanning for planets." );
		
		final List<CommPortIdentifier> arrPorts = SerialConnector.getAvailablePorts();
		final List<CommPortIdentifier> listPorts = new LinkedList<>( arrPorts );

		final Set<String> setPortsInUse = porttree.mapConnectors.keySet();

		for ( final CommPortIdentifier port : listPorts ) {
//			log( "Checking port: " + port.getName() );
			
			final SerialConnector connector = new SerialConnector( port );
			try {
				if ( !setPortsInUse.contains( port.getName() ) 
						&& connector.attachToPort() ) {
					
					log( "Checking port: " + port.getName() );

					connector.attachAtomTree( atomtree );

					boolean bNewPortAdded = false;
					porttree.setStatus( port, "Checking...", connector );
					
					boolean bTimedout = false;
					
					ComBaud[] rates = SerialConnector.ComBaud.values();
					for ( int i=0; (	i<rates.length 
										&& !bNewPortAdded 
										&& !bTimedout		); i++ ) {
						final ComBaud rate = rates[i];
						
						porttree.setStatus( port, "Checking: " + rate.toString() );

						boolean bOk = false;
						
						try {
							bOk = connector.adjustBaudRate( rate );
						} catch (IOException e) {
							bOk = false;
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		
						try {
							if ( bOk ) {

								final String strMessage = "Scanning port " 
										+ port.getName() + " at baud " + rate + "...";
								log( strMessage, false );


								final InputStreamAtomProducer ag = connector.getAtomGenerator();
								
								final long lTimeTimeout = 
										System.currentTimeMillis() + 5000;
								long lTimeSend = 
										System.currentTimeMillis() + 500;
								
								boolean bAcceptGood = false;
								@SuppressWarnings("unused")
								boolean bAcceptBad = false;
								boolean bWaiting = true;
								do {
									Thread.sleep( 1 );
									if ( null!=ag ) {
										if ( ag.getBadBytes() > 1 ) {
											bAcceptBad = true;
											bWaiting = false;
										} else if ( ag.getGoodBytes() > 10 ) {
											bAcceptGood = true;
											bWaiting = false;
										}
									}
									if ( System.currentTimeMillis() > lTimeSend ) {
										final boolean bSent = connector.send( "REQ_STATUS" );
										if ( !bSent ) {
											bAcceptBad = true;
										}
										lTimeSend = lTimeTimeout + 100; // turn off
									}
									bTimedout = System.currentTimeMillis() > lTimeTimeout; 
								} while ( bWaiting && !bTimedout );
			
								if ( bAcceptGood ) {

									System.out.println();
									log( "ESTABLISHED." );
									
									ag.accept( this.atomtree );
									
									bNewPortAdded = true;
									porttree.setStatus( 
											port, 
											"Established " + rate.toString() );

								} else if ( bTimedout ) { 
									ag.close();
									log( "Timed out." );
								} else {
									ag.close();
									log( "Not accepted." );
								}
							} else {
//								log( "Unavailable." );
							}
							
						} catch ( final Exception e ) {
							// probably means port is unavailable. skip.
						}
					}
					
					if (!bNewPortAdded) {
						porttree.setStatus( port, null );
					}
				}
			} catch ( final Throwable t ) {
//				t.printStackTrace();
			}
		}
	}
	
	
	public static void main( final String[] args ) {
		
		

//		final Date date = new Date();
//		final SimpleDateFormat format = new SimpleDateFormat( TIMESTAMP, Locale.ENGLISH);
//		final String strTimestamp = format.format( date );
//		System.out.println( "Time: " + strTimestamp );
//		if (1==1) return;
		
		
		final Star star = new Star();
		star.open();
		
		while ( !star.getShell().isDisposed() ) {
	    	if ( !display.readAndDispatch() ) {
	    		display.sleep();
	    	}
		}
		Star.log( "Program exiting." );
//		if ( !display.isDisposed() ) {
//			display.dispose();
//		}
		System.exit( 0 );
	}
	
}

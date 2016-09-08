package jmr.home.engine;

import jmr.home.apps.AtomTree;
import jmr.home.apps.PortTree;
import jmr.home.model.Atom;
import jmr.home.model.Atom.Type;
import jmr.home.model.IAtomConsumer;
import jmr.home.model.IAtomValues;
import jmr.integrate.arduino.SketchDefines;

public class Processor implements IAtomConsumer, IAtomValues {

	private final PortTree porttree;
	private final AtomTree atomtree;
	
	
	private static Processor instance;
	
	public Processor(	final PortTree porttree,
						final AtomTree atomtree ) {
		this.porttree = porttree;
		this.atomtree = atomtree;
		Processor.instance = this;
	}
	
	public static Processor getProcessor() {
//		if ( null==instance ) {
//			instance = new Processor();
//		}
		return instance;
	}
	
	
	public static final int REFRESH_INTERVAL = 2000;
	
	
	@Override
	public void consume( final Atom atom ) {
		if ( null==atom ) return;
		
		final int iSendCode = atom.getAsInt( VAR_SEND_CODE );
		
		final int iNodeInit = SketchDefines.get( "SEND_CODE_NODE_INIT" );
		
		if ( iSendCode == iNodeInit ) {

			final Thread threadInitPlanet = new Thread() {
				@Override
				public void run() {
					try {
						String strCommand; 
						final String strSerNo = atom.get( VAR_SERIAL_NUMBER );
	
						final Atom atomSetTime = 
								new Atom( Type.INVOKE, "Set Time", null );
						atomSetTime.put( VAR_DEST_SERNO, strSerNo );
						strCommand = "/set/time=" + System.currentTimeMillis();
						atomSetTime.put( VAR_COMMAND, strCommand );
						Relay.get().consume( atomSetTime );
						
						Thread.sleep( 1000 );
	
						final Atom atomSetSchedule = 
								new Atom( Type.INVOKE, "Set Interval", null );
						atomSetSchedule.put( VAR_DEST_SERNO, strSerNo );
						strCommand = "/set/interval=" + REFRESH_INTERVAL;
						atomSetSchedule.put( VAR_COMMAND, strCommand );
						Relay.get().consume( atomSetSchedule );

					} catch ( final InterruptedException e ) {
						// just ignore
					}
				}
			};
			threadInitPlanet.start();
			
		}
		
//		final String strPort = atom.get( Atom.VAR_ORIG_PORT );
//		if ( null!=strPort ) {
//			final String strSerialNumber = atom.get( Atom.VAR_SERIAL_NUMBER );
//			if ( null!=strSerialNumber ) {
//				porttree.setSerialNumber( strPort, strSerialNumber );
//			}
////			porttree.mapConnectors.
//		}
	
	}
	
	public PortTree getPortTree() {
		return this.porttree;
	}
	
	public AtomTree getAtomTree() {
		return this.atomtree;
	}
	
}

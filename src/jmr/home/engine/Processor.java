package jmr.home.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jmr.home.Log;
import jmr.home.apps.AtomTree;
import jmr.home.apps.PortTree;
import jmr.home.database.AtomTable;
import jmr.home.model.Atom;
import jmr.home.model.Atom.Type;
import jmr.home.model.IAtomConsumer;
import jmr.home.model.IAtomValues;
import jmr.integrate.arduino.SketchDefines;

public class Processor implements IAtomConsumer, IAtomValues {

	private final PortTree porttree;
	private final AtomTree atomtree;
	
	private final AtomTable tableAtom = new AtomTable();
	
	
	private static Processor instance;
	
	private final Thread threadRunRules;
	
	public Processor(	final PortTree porttree,
						final AtomTree atomtree ) {
		this.porttree = porttree;
		this.atomtree = atomtree;
		Processor.instance = this;
		threadRunRules = createRuleThread();
		threadRunRules.start();
	}

	public static Processor getProcessor() {
//		if ( null==instance ) {
//			instance = new Processor();
//		}
		return instance;
	}
	
	
	public static final int REFRESH_INTERVAL = 6000;
	public static final int ALLOWABLE_DELAY = 2000;
	public static final int RULE_LOOP_INTERVAL = 200;
	
	private static final Map<String,PlanetInfo> mapPlanetInfo = new HashMap<>();
	
	
	private static class PlanetInfo {
		public long lLastContact;
		private long lGracePeriod;
		
		public int iInitAttempts;
		public boolean bAWOL = false;
		
		public void applyGracePeriod( int iTime ) {
			final long lNow = System.currentTimeMillis();
			lGracePeriod = Math.max( lGracePeriod, lNow + iTime );
		}
		
		public boolean inGracePeriod() {
			final long lNow = System.currentTimeMillis();
			return ( lNow<lGracePeriod );
		}
	}
	
	
	private PlanetInfo getPlanetInfo( final String strSerNo ) {
		final PlanetInfo piGet = mapPlanetInfo.get( strSerNo );
		if ( null!=piGet ) {
			return piGet;
		} else {
			final PlanetInfo piNew = new PlanetInfo();
			mapPlanetInfo.put( strSerNo, piNew );
			return piNew;
		}
	}

	
	
	private void registerContact( final String strSerNo ) {
		if ( null==strSerNo ) return;
		if ( strSerNo.isEmpty() ) return;
		
		final PlanetInfo pi = getPlanetInfo( strSerNo );
		pi.lLastContact = System.currentTimeMillis();
		pi.iInitAttempts = 0;
		pi.bAWOL = false;
	}
	
	
	private Thread createRuleThread() {
		final String strName = Processor.class.getSimpleName() + " - RuleLoop";
		final Thread threadCreateRules = new Thread( strName ) {
			@Override
			public void run() {
				boolean bLoop = true;
				while ( bLoop ) {
					try {
						
						final long lNow = System.currentTimeMillis();
//						final long lLate = lNow - REFRESH_INTERVAL - ALLOWABLE_DELAY;

						for ( final Entry<String, PlanetInfo> 
										entry : mapPlanetInfo.entrySet() ) {
							final PlanetInfo pi = entry.getValue();
							final Long lLast = pi.lLastContact;
							
//							final long lTimePast = lLast - lLate;
							
							final boolean bLate = 
									(lLast + REFRESH_INTERVAL + ALLOWABLE_DELAY) < lNow;

							if ( !pi.bAWOL && bLate ) {
								System.out.print( "L" );
//								if ( lNow<pi.lGracePeriod ) {
								if ( pi.inGracePeriod() ) {
									// in grace period
									System.out.print( "G" );
								} else {
									System.out.print( "I" );
									final String strSerNo = entry.getKey();
									initializeContact( strSerNo );
								}
							}
						}
						
						Thread.sleep( RULE_LOOP_INTERVAL );
					} catch ( final InterruptedException e ) {
						bLoop = false;
					}
				}
			}
		};
		return threadCreateRules;
	}

	
	private void initializeContact( final String strSerNo ) {
		if ( null==strSerNo ) return;
		if ( strSerNo.isEmpty() ) return;
		
//		final long lNow = System.currentTimeMillis();
		
		final PlanetInfo pi = getPlanetInfo( strSerNo );
//		pi.lGracePeriod = Math.max( pi.lGracePeriod,
//				lNow + 5000 );
		pi.applyGracePeriod( 5000 );
		pi.iInitAttempts = pi.iInitAttempts + 1;
		
		if ( pi.iInitAttempts>100 ) {
			pi.bAWOL = true;
			return;
		} else if ( pi.iInitAttempts>3 ) {
			// has not initialized after 3 attempts. 
			// slow attempts to every minute.
//			pi.lGracePeriod = Math.max( pi.lGracePeriod,
//					lNow + (1000*60) );
			pi.applyGracePeriod( 1000*60 );
		} 
		
		final Thread threadInitPlanet = new Thread() {
			@Override
			public void run() {
				try {
					String strCommand; 

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
	
	
	
	@Override
	public void consume( final Atom atom ) {
		if ( null==atom ) return;

		tableAtom.write( atom );

		final String strSerNo = atom.get( VAR_SERIAL_NUMBER );
		registerContact( strSerNo );

		final Integer iSendCode = atom.getAsInt( VAR_SEND_CODE );
		
		final int iNodeInit = SketchDefines.get( "SEND_CODE_NODE_INIT" );

		
		if ( null!=iSendCode && iSendCode.intValue() == iNodeInit ) {
			initializeContact( strSerNo );
			
			Log.log( "Initialize contact to planet.", atom );
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

package jmr.home.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jmr.home.apps.AtomTree;
import jmr.home.apps.PortTree;
import jmr.home.database.AtomTable;
import jmr.home.database.ConfigTable;
import jmr.home.logging.EventType;
import jmr.home.logging.Log;
import jmr.home.model.Atom;
import jmr.home.model.Atom.Type;
import jmr.home.model.IAtomConsumer;
import jmr.home.model.IAtomValues;
import jmr.util.Util;

public class Processor implements IAtomConsumer, IAtomValues {

	private final PortTree porttree;
	private final AtomTree atomtree;
	
	private final AtomTable tableAtom = new AtomTable();
	
	
	private static Processor instance;
	
	private final Thread threadRunRules;

	public static final String KEY_HOST_IP = "host_ip";

	private Boolean bPrimaryStarHere = null;
	
	
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
	
	
	public static final int REFRESH_INTERVAL = 4000;
	public static final int ALLOWABLE_DELAY = 2000;
	public static final int RULE_LOOP_INTERVAL = 200;
	
	private static final Map<String,PlanetInfo> 
					mapPlanetInfo = new HashMap<>();
	
	
	private static class PlanetInfo {
		public long lLastContact;
		private long lGracePeriod;
		
		public int iInitAttempts;
		public boolean bAWOL = false;

		public final String strSerNo;
		
		public PlanetInfo( final String strSerNo ) {
			this.strSerNo = strSerNo;
		}
		
		final Map<String,String> mapAllData = new HashMap<>();
		
		
		public void applyGracePeriod( int iTime ) {
			final long lNow = System.currentTimeMillis();
			lGracePeriod = Math.max( lGracePeriod, lNow + iTime );
		}
		
		public boolean inGracePeriod() {
			final long lNow = System.currentTimeMillis();
			return ( lNow<lGracePeriod );
		}
		
		
		public void applyInputs( final Atom atom ) {
			
			for ( Entry<String, String> entry : atom.entrySet() ) {
				final String strName = entry.getKey();

				final boolean bIgnorable = VAR_TIME.equals( strName );
				
				if ( !bIgnorable ) {
					final String strValueNew = entry.getValue();
					final String strValueOld = mapAllData.get( strName );
					
					if ( ( null==strValueOld ) 
							|| ( !strValueOld.equals( strValueNew ) ) ) {

						// data changed. check triggers, record.

//						if ( strName.matches( "^D.$" ) ) {
						if ( strName.matches( "D." ) ) {
							
							// fire digital triggers
							
							System.out.println( "Value changed: " 
										+ strSerNo + "." + strName 
										+ " was \"" + strValueOld + "\", "
										+ "now \"" + strValueNew + "\"" );
						}
						
						this.mapAllData.put( strName, strValueNew );
					}
				}
			}
		}
		
	}
	
	
	private PlanetInfo getPlanetInfo( final String strSerNo ) {
		if ( null==strSerNo ) return null;
		if ( strSerNo.isEmpty() ) return null;
		
		final PlanetInfo piGet = mapPlanetInfo.get( strSerNo );
		if ( null!=piGet ) {
			return piGet;
		} else {
			final PlanetInfo piNew = new PlanetInfo( strSerNo );
			mapPlanetInfo.put( strSerNo, piNew );
			return piNew;
		}
	}
	
	
	public boolean isPrimaryStarHere() {
		if ( null==bPrimaryStarHere ) {
			final String strConfigHostIP = ConfigTable.get().get( KEY_HOST_IP );
			final String strThisHostIP = Util.getHostIP();
			bPrimaryStarHere = strThisHostIP.equals( strConfigHostIP );
		}
		return bPrimaryStarHere.booleanValue();
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

									Log.log( EventType.MISSING_SCHEDULED_COMM_FROM_PLANET, 
																	null );

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

	
	private void sendRemoteAtomCommand(	final String strSerNo,
										final String strName,
										final String strCommand ) {
		final Atom atom = new Atom( Type.INVOKE, strName, null );
		atom.put( VAR_DEST_SERNO, strSerNo );
		atom.put( VAR_COMMAND, strCommand );
		Relay.get().consume( atom );
	}

	private void sendLocalAtom( final String strName,
								final String strValue ) {
		final Atom atom = new Atom( Type.STATUS, strName, null );
		atom.put( strName, strValue );
		Relay.get().consume( atom );
	}


	private void initializeContact( final String strSerNo ) {
		if ( null==strSerNo ) return;
		if ( strSerNo.isEmpty() ) return;
		
		Log.log( EventType.INIT_COMM_TO_PLANET, 
					"Initialize contact to planet." );

//		final long lNow = System.currentTimeMillis();
		
		final PlanetInfo pi = getPlanetInfo( strSerNo );
		
		if ( null==pi ) return;
		
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

					if ( isPrimaryStarHere() ) {
	
//						final long lTime = System.currentTimeMillis();
						final long lTime = Util.getMillisecondsPastMidnight();
						strCommand = "/set/time=" + lTime;
						sendRemoteAtomCommand( strSerNo, "Set Time", strCommand );
						
						Thread.sleep( 1000 );
	
						strCommand = "/set/interval=" + REFRESH_INTERVAL;
						sendRemoteAtomCommand( strSerNo, "Set Time", strCommand );

						
						// custom planet settings
						
						if ( "102008".equals( strSerNo ) ) {

							Thread.sleep( 1000 );

							strCommand = "/mode?12=1";
							sendRemoteAtomCommand( strSerNo, "Set Pin Mode", strCommand );
							
						} else if ( "102009".equals( strSerNo ) ) {

							Thread.sleep( 1000 );

							strCommand = "/mode?12=1";
							sendRemoteAtomCommand( strSerNo, "Set Pin Mode", strCommand );
						}
						
						
						
						
					} else {

						final Atom atomRedirect = 
								new Atom( Type.INVOKE, "Redirect Host", null );
						atomRedirect.put( VAR_DEST_SERNO, strSerNo );
						strCommand = "/set/host_ip=" 
									+ ConfigTable.get().get( KEY_HOST_IP );
						atomRedirect.put( VAR_COMMAND, strCommand );
						Relay.get().consume( atomRedirect );

					}

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
		if ( null!=iSendCode ) {
			final EventType type = EventType.getEventType(iSendCode);
			if ( null!=type ) {
				Log.log( type, null, atom );
			}
		}
		
//		final int iNodeInit = SketchDefines.get( "SEND_CODE_NODE_INIT" );
		final long iNodeInit = EventType.SEND_CODE_NODE_INIT.getSeq();

		
		if ( null!=iSendCode && iSendCode.intValue() == iNodeInit ) {
			initializeContact( strSerNo );
		}
		
		
		final PlanetInfo pi = getPlanetInfo( strSerNo );
		if ( null!=pi ) {
			
			pi.applyInputs( atom );

			final Atom atomUI = new Atom( Type.TO_UI, "Update UI", null );


			
			final Integer iHumid02 = atom.getAsInt( "Humid02" );
			if ( null!=iHumid02 ) {
				final String strField = strSerNo + ".Humid_02";
				final String strValue = atom.get( "Humid02" );
				atomUI.put( strField, strValue );
			}
			final Integer iHumid03 = atom.getAsInt( "Humid03" );
			if ( null!=iHumid03 ) {
				final String strField = strSerNo + ".Humid_03";
				final String strValue = atom.get( "Humid03" );
				atomUI.put( strField, strValue );
			}
			final Integer iTemp02 = atom.getAsInt( "Temp02" );
			if ( null!=iTemp02 ) {
				final String strField = strSerNo + ".Temp_02";
				final String strValue = atom.get( "Temp02" );
				atomUI.put( strField, strValue );
			}

			
			
			if ( "102009".equals( strSerNo ) ) {
				
				// ..
			} else if ( "102008".equals( strSerNo ) ) {
				
				final Integer iRawValue = atom.getAsInt( "A5" );
				if ( null!=iRawValue ) {
					
					final float fAdjusted = (float)iRawValue / 4;
					
					sendLocalAtom( "Gas", Float.toString( fAdjusted ) );
					
					final String strField = "Combustable Gas";
					final String strValue = Integer.toString( (int)fAdjusted );
					atomUI.put( strField, strValue );
				}
				
			}
			
			if ( null!=atomUI ) {
				Relay.get().consume( atomUI );
			}
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

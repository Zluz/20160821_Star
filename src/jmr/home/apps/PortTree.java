package jmr.home.apps;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import gnu.io.CommPortIdentifier;

public class PortTree {

	private final Tree tree;
	private final TreeColumn tcName;
	private final TreeColumn tcPlanet;
	private final TreeColumn tcStatus;

	private final Display display;

//	public final EnumMap<SerialConnector.ComPort,ConnectorData> 
//			mapConnectors = new EnumMap<>( SerialConnector.ComPort.class );

	public final Map<CommPortIdentifier,ConnectorData> 
			mapConnectors = new HashMap<>();
	
	public static class ConnectorData {
		
		Planet planet;
		
		final TreeItem item;
		
		public ConnectorData( final TreeItem item ) {
			this.item = item;
		}

//		public ConnectorData( final Planet planet ) {
//			this.planet = planet;
//		}
		
		public void setPlanet( final Planet planet ) {
			this.planet = planet;
		}
	}
	
	
	public PortTree( final Composite comp ) {
		this.tree = new Tree( comp, 
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL 
				| SWT.SINGLE | SWT.FULL_SELECTION );
		this.display = comp.getDisplay();
		
		tree.setHeaderVisible( true );
		
		tcName = new TreeColumn( tree, SWT.LEFT );
		tcName.setText( "Port" );
		tcName.setWidth( 80 );
		
		tcPlanet = new TreeColumn( tree, SWT.LEFT );
		tcPlanet.setText( "Planet" );
		tcPlanet.setWidth( 80 );
		
		tcStatus = new TreeColumn( tree, SWT.LEFT );
		tcStatus.setText( "Status" );
		tcStatus.setWidth( 120 );
	}
	
	public Tree getTree() {
		return this.tree;
	}

	public void closeConnectors() {
//		for ( final ComPort port : mapConnectors.keySet() ) {
		for ( final CommPortIdentifier port : mapConnectors.keySet() ) {
			final ConnectorData cd = mapConnectors.get( port );
			try {
				if ( null!=cd && cd.planet!=null ) {
					cd.planet.close();
				}
			} catch ( final Throwable t ) {
//				log( "ERROR: " + t );
				System.err.println( "ERROR: " + t );
			}
		}
	}
	
	
	public void setStatus(	final CommPortIdentifier port,
							final String strStatus ) {
		if ( null==port ) return;

//		final ConnectorData[] cd = new ConnectorData[]{ null };
		display.asyncExec( new Runnable() {

			@Override
			public void run() {
				
				final TreeItem item;
				final ConnectorData cdExisting = mapConnectors.get( port );
				if ( null!=cdExisting ) {
					item = cdExisting.item;
					if ( null!=strStatus ) {
						item.setText( 2, strStatus );
					} else {
						item.dispose();
						mapConnectors.remove( port );
					}
				} else {
					if ( null!=strStatus ) {
						item = new TreeItem( tree, SWT.NONE );
						
						final String[] row = new String[] { 
								port.getName(),
								"unknown",
								strStatus };
						item.setText( row );
						
						final ConnectorData 
								cdNew = new PortTree.ConnectorData( item );
						mapConnectors.put( port, cdNew );

					} else {
						item = null;
						mapConnectors.remove( port );
					}
				}
				
//				if ( null!=item ) {
//					cd[0] = new PortTree.ConnectorData( item );
////					cd[0].item = item;
//				}
//				if ( null!=cd ) {
//					mapConnectors.put( port, cd );
//				}
			}
			
		});
//		mapConnectors.put( port, cd[0] );
//		return cd[0];
	}
	
//	public void setLineStatus(	final SerialConnector.ComPort port,
//								final String strStatus ) {
//		if ( null==port ) return;
//		
//		final ConnectorData cd = mapConnectors.get( port );
//		if ( null==cd ) return;
//		
//	}
	
	
	
}

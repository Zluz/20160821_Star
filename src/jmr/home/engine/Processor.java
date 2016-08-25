package jmr.home.engine;

import jmr.home.apps.AtomTree;
import jmr.home.apps.PortTree;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;

public class Processor implements IAtomConsumer {

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
	
	
	
	@Override
	public void consume( final Atom atom ) {

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

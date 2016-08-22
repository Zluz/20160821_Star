package jmr.home.engine;

import jmr.home.apps.AtomTree;
import jmr.home.apps.PortTree;
import jmr.home.model.Atom;

public class Processor {

	private final PortTree porttree;
	private final AtomTree atomtree;
	
	public Processor(	final PortTree porttree,
						final AtomTree atomtree ) {
		this.porttree = porttree;
		this.atomtree = atomtree;
	}
	
	public void consumeAtom( final Atom atom ) {
		//TODO implement..
	}
	
	public PortTree getPortTree() {
		return this.porttree;
	}
	
	public AtomTree getAtomTree() {
		return this.atomtree;
	}
	
}

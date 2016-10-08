package jmr.home.comm.file;

import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;

public class FileOutAtomConsumer implements IAtomConsumer {

	final String strOutDir = System.getProperty( "tmp.dir" );
	
	@Override
	public void consume( final Atom atom ) {
		
	}

}

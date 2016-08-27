package jmr.home.engine;

import java.util.HashSet;
import java.util.Set;

import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;
import jmr.home.model.IAtomProducer;

public class Relay implements IAtomProducer, IAtomConsumer {


	final Set<IAtomConsumer> setConsumers = new HashSet<>();

	final private static Relay instance = new Relay();
	
	public static Relay get() {
		return Relay.instance;
	}
	
	
	@Override
	public void consume( final Atom atom ) {
		if ( null==atom ) return;
		
		System.out.println( "Relay.consume(), atom: " + atom );
		
		for ( final IAtomConsumer consumer : setConsumers ) {
			consumer.consume( atom );
		}
	}

	@Override
	public void registerConsumer( final IAtomConsumer consumer ) {
		setConsumers.add( consumer );
	}

}

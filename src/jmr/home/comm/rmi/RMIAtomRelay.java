package jmr.home.comm.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import jmr.home.api.comm.rmi.ISimpleMessageConsumer;
import jmr.home.api.comm.rmi.ISimpleMessageConsumer.Type;
import jmr.home.model.Atom;
import jmr.home.model.IAtomConsumer;

public class RMIAtomRelay implements IAtomConsumer {

	final public static String RMI_URL = "rmi://127.0.0.1/" 
				+ ISimpleMessageConsumer.class.getName();
	
	public RMIAtomRelay() {
//		if ( null==System.getSecurityManager() ) {
//			System.setSecurityManager( new RMISecurityManager() );
//		}
	}
	
	
	@Override
	public void consume( final Atom atom ) {
		try {
			
			final ISimpleMessageConsumer server = 
					(ISimpleMessageConsumer)Naming.lookup( RMI_URL );
			
			final String strMessage = atom.report();
			
			server.sendMessage( Type.MESSAGE, strMessage );
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package jmr.home.model;

public interface IAtomValues {

	public final static String VAL_FIELD_DELIM = "\n";

	public final static String VAR_TIME = "Atom.Time";

	public final static String VAR_ORIG_HOST = "Atom.Orig.Host";
	public final static String VAR_ORIG_PORT = "Atom.Orig.Port";

	public final static String VAR_DEST_SERNO = "Atom.Dest.SerNo";
//	public final static String VAR_DEST_HOST = "Atom.Dest.Host";

	public final static String VAR_PLANET_IP = "Planet.IP";
	public final static String VAR_STAR_IP = "Star.IP";
	
	public final static String VAR_COMMAND = "Command";

	public final static String VAR_SEND_CODE = "SendCode";

//	public final static String VAR_SERIAL_NUMBER = "Device.SerialNumber";
//	public final static String VAR_SERIAL_NUMBER = "Arduino.Serial";
	public final static String VAR_SERIAL_NUMBER = "SerNo";
	
	public final static String VAR_INVOKE = "VAR_INVOKE";
	
	public final static String VAL_INVOKE_RESCAN_PORTS = "RESCAN_PORTS";
	
	public final static String VAR_UI_FIELD = "UIField";
	public final static String VAR_UI_VALUE = "UIValue";
}

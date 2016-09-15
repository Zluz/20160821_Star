package jmr.home.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Map.Entry;

import jmr.home.model.Atom;
import jmr.home.model.Star;
import jmr.util.Util;

public class AtomTable extends BaseTable implements ITable<Atom> {

	@Override
	public Long write( final Atom atom ) {
		if ( null==atom ) return null;
		
		if ( null!=atom.getSeq() ) {
			return atom.getSeq(); // TODO update database with values
		}
		
		try ( final Statement stmt = ConnectionProvider.createStatement() ) {
			if ( null!=stmt ) {
				
//				INSERT INTO table_name (column1,column2,column3,...)
//				VALUES (value1,value2,value3,...);
				
				final String strStar = Long.toString( Star.get().getSeq() );
				
				final String strPlanet;
				if ( null!=atom.getPlanet() 
								&& null!=atom.getPlanet().getSeq() ) {
					strPlanet = Long.toString( atom.getPlanet().getSeq() );
				} else {
					strPlanet = "null";
				}
				
				final Date date = new Date( atom.getTime() );
				final String strInsertAtom = 
						"INSERT INTO galaxy.Atom ( seq_Star, seq_Planet, Created ) "
						+ "VALUES ( "
								+ strStar + ", " 
								+ strPlanet + ", " 
								+ format( date ) + " )";
				stmt.execute( strInsertAtom, Statement.RETURN_GENERATED_KEYS );
				final ResultSet keys = stmt.getGeneratedKeys();
				if ( !keys.next() ) {
					return null;
				}
				final long lSeqAtom = keys.getLong(1);
				
				String strInsertLines = 
						"INSERT INTO galaxy.Line "
								+ "( seq_Atom, Name, ValueText, Value, ValueInt ) "
								+ " VALUES ";
				boolean bFirst = true;
				for ( final Entry<String, String> entry : atom.entrySet() ) {
					final String strName = entry.getKey();
					final String strValue = entry.getValue();
					
					String strNameTrunc = Util.truncate( strName, 60 );
					String strValueTrunc = Util.truncate( strValue, 45 );
					final Integer iIntValue = atom.getAsInt( strName );
					final String strIntValue; 
					if ( null!=iIntValue ) {
						strIntValue = Integer.toString( iIntValue );
					} else {
						strIntValue = "null";
					}
					
					if ( bFirst ) {
						bFirst = false;
					} else {
						strInsertLines = strInsertLines + ", ";
					}
					strInsertLines = strInsertLines
							+ "( " + lSeqAtom + ", "
							+ "\"" + strNameTrunc + "\", "
							+ "\"" + strValue + "\", "
							+ "\"" + strValueTrunc + "\", "
							+ strIntValue + " ) ";  
				}

				stmt.execute( strInsertLines );
			}
			
		} catch ( final SQLException e ) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Atom read( final long seq ) {
		// TODO Auto-generated method stub
		return null;
	}

}

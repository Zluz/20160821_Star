package jmr.home.database;

public interface ITable<T> {

	Long write( T element );
	
	T read( long seq );
	
}

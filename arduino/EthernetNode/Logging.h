

void log( const String strText ) {
#if defined( DEFINE_DEVICE_MEGA )
  File fileLog = SD.open( FILE_LOG, FILE_WRITE );
  if ( fileLog ) {
    
    // 86400000
    // 12345678
//    sprintf( cTimestamp, "%08u", millis() );

    long lTime = millis();
    long lBig = lTime / 1000;
    long lSmall = lTime % 1000;
    sprintf( cStringBuffer, "%05u", lBig );
    fileLog.print( cStringBuffer );
    fileLog.print( F(".") );
    sprintf( cStringBuffer, "%03u", lSmall );
    fileLog.print( cStringBuffer );
 
//    fileLog.print( cTimestamp );
//    fileLog.print( formatTimeValue( millis() ) );
    
    fileLog.print( F(" ") );
    fileLog.println( strText );
    fileLog.close();
  }
#endif
  Serial.print( F( "log> " ) );
  Serial.println( strText );
}






int freeRam() {
  extern int __heap_start, *__brkval; 
  int v; 
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval); 
}


void reboot() {
  log( F("reboot requested") );
  delay( 1000 );
  asm volatile( "jmp 0" );
}












String pop( String& strSource,
            String strToken ) {
  if ( 0==strSource.length() ) return "";
  if ( 0==strToken.length() ) return "";
  int iPos = strSource.indexOf( strToken );
  if ( -1==iPos ) return strSource;
  
  String strSub = strSource.substring( 0, iPos );
  strSource = strSource.substring( iPos + strToken.length() );
  return strSub;
}



String formatIP( IPAddress address ) {
  String strResult = F("( ");
  for ( int i=0; i<4; i++ ) {
    if ( i>0 ) {
      strResult = strResult + F(".");
    }
    strResult = strResult + String( address[i] );
  }
  strResult = strResult + F(" )");
  return strResult;
}



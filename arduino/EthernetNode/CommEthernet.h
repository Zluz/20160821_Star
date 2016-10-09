


void printNameValue(  EthernetClient client,
                      const String strName,
                      const String strValue ) {
  client.print( F( "<tr><td colspan='2'>" ) );
  client.print( strName );
  client.print( F( "</td><td><tt>" ) );
  client.print( strValue );
  client.println( F( "</tt></td></tr>" ) );
}

void printNameValue(  EthernetClient client,
                      const String strName,
                      const String strKeyword,
                      const String strValue ) {
  client.print( F( "<tr><td>" ) );
  client.print( strName );
  client.print( F( "</td><td><tt>" ) );
  client.print( strKeyword );
  client.print( F( "</td><td><tt>" ) );
  client.print( strValue );
  client.println( F( "</tt></td></tr>" ) );
}


void printSection( EthernetClient client,
                   const String strTitle ) {
  client.print( F( "<tr><th colspan='3'>" ) );
  client.print( strTitle );
  client.print( F( "</th></tr>" ) );
}



void formatHexDigit( EthernetClient client,
                     int num ) {
  client.print( F("0x") );
  if ( num < 11 ) {
    client.print( F("0") );
  }
  String strHexValue = String( num, HEX );
  strHexValue.toUpperCase();
  client.print( strHexValue );
}


void printStarIP( EthernetClient client ) {
  client.print( F("( ") );
  for ( int i=0; i<4; i++ ) {
    if ( i>0 ) {
      client.print( F(".") );
    }
    client.print( String( arrStarIP[i] ) );
  }
  client.print( F(" )") );
}



void printMACAddress( EthernetClient client ) {
  client.print( F("[ ") );
  for ( int i=0; i<6; i++ ) {
    formatHexDigit( client, macPlanet[i] );
    client.print( F(" ") );
  }
  client.print( F("]") );
}







void sendHTTPHTMLHeader( EthernetClient client ) {
  client.println( F("Content-Type: text/html" ) );
  client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
  client.println();
  client.println( F("<!DOCTYPE HTML>" ) );
  client.println( F("<html>" ) );
}


void sendHTTPTextHeader( EthernetClient client ) {
  client.println( F("Content-Type: text/plain" ) );
  client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
  client.println();
}


void sendDataJSONResponse( EthernetClient client ) {

    // light up activity LED
    digitalWrite( byteActivityLED, HIGH );

//    Serial.println( "Sending response back to client.." );

    // send a standard http response header
    client.println( F("HTTP/1.1 200 OK" ) );
    client.println( F("Content-Type: application/json" ) );
    client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//    client.println("Refresh: 1");  // refresh the page automatically every 5 sec
    client.println();

    // output the value of each analog input pin
    client.println( F( "{" ) );    
    for ( int iA = 0; iA < 6; iA++ ) {
      int iValue = analogRead( iA );
      client.print( F( " \"A" ) );
      client.print( String( iA ) );
      client.print( F( "\": " ) );
      client.print( String( iValue ) );
      client.println( F( "," ) );
    }
    for ( int iD = 2; iD < 14; iD++ ) {
      int iValue = digitalRead( iD );
      client.print( F( " \"D" ) );
      client.print( String( iD ) );
      client.print( F( "\": " ) );
      client.print( String( iValue ) );
      client.println( F( "," ) );
    }
    
    client.println( F("</table></font>" ) );
    
  client.println( F("</html>" ) );
//  Serial.println( "Response sent completely." );

}

//void sendCompleteHTMLResponse() {}





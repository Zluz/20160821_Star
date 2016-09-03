
#include <SPI.h>
#include <Ethernet.h>

byte macPlanet[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress ipPlanet( 192,168,1,3 );


EthernetServer server(80);



void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);

  Ethernet.begin( macPlanet, ipPlanet );
  server.begin();
  Serial.print("server is ");
  Serial.println(Ethernet.localIP());
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


void sendInfo( EthernetClient client ) {
  if ( !client.connected() ) return;

    String strRequest;
    boolean bRead = true;
    while ( bRead ) {

      if ( client.available() ) {
        char c = client.read();
        strRequest = strRequest + c;
      }
      
      if ( strRequest.indexOf( "HTTP" )>-1 ) {
        bRead = false;
      }
    }
    
    String strOriginal = strRequest;
    
    pop( strRequest, " " );
    strRequest = pop( strRequest, "HTTP" );
    strRequest.trim();
    
    String strPath = strRequest;
    strPath = pop( strPath, "?" );
    
    String strParams = strRequest;
    pop( strParams, "?" );
    
    String strName = strParams;
    strName = pop( strName, "=" );
    
    String strValue = strParams;
    pop( strValue, "=" );
    

    // send a standard http response header
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: text/html");
    client.println("Connection: close");  // the connection will be closed after completion of the response
//    client.println("Refresh: 1");  // refresh the page automatically every 5 sec
    client.println();
    client.println("<!DOCTYPE HTML>");
    client.println("<html>");

    client.println( "strOriginal: " + strOriginal + "</p>" );
    client.println( "strRequest: " + strRequest + "</p>" );
    
    client.println( "strPath: " + strPath + "</p>" );
    client.println( "strParams: " + strParams + "</p>" );
    
    client.println( "strName: " + strName + "</p>" );
    client.println( "strValue: " + strValue + "</p>" );
    
    client.println( "  <br />" );
    
    // output the value of each analog input pin
    for ( int iA = 0; iA < 6; iA++ ) {
      int iValue = analogRead( iA );
      client.print( "  analog input " );
      client.print( iA );
      client.print( " is " );
      client.print( iValue );
      client.println( "<br />" );
    }
  client.println( "</html>" );
}



void loop() {
  // put your main code here, to run repeatedly:

  EthernetClient client = server.available();
  if (client) {
    Serial.println("new client");
    
    sendInfo( client );
    delay(1);
    client.stop();
  }


}
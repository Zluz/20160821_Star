/****************************************
  EthernetNode_002
 ****************************************/


/* Included libraries */
#include <SPI.h>
#include <Ethernet.h>
#include <EEPROM.h>
#include <avr/pgmspace.h>

#define DEBUG // set to DEBUG to apply

/* Constants */

//const String strVersion = "20160903_001";
const static String strVersion = "20160903_001";
//const static String strVersion PROGMEM = "20160903_001";
//const static String strVersion PROGMEM = "20160903_001";
//const static char strVersion[] PROGMEM = "20160903_001";
//const static char strVersion[] = F("20160903_001");

byte macPlanet[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
IPAddress ipPlanet( 192,168,1,3 );
//IPAddress ipStar( 192,168,1,4 );
IPAddress ipStar( 0,0,0,0 );
byte arrStarIP[] = { 0,0,0,0 };

/* Globals */

char sIDRead[7];

String strStarHost;
int iInterval;
long lNextSend;

EthernetServer server(80);

unsigned long lTime;




/* Functions */


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
//  client.print( F( "(printStarIP() disabled)" ) ); return;
  
  client.print( F("( ") );
  for ( int i=0; i<4; i++ ) {
    if ( i>0 ) {
      client.print( F(".") );
    }
    client.print( String( arrStarIP[i] ) );
  }
  client.print( F(" )") );
}




long getSystemTime() {
  if ( lTime>0 ) {
    return lTime + millis();
  } else {
    return 0;
  }
}


void resolveMACAddress() {
  const String strSerNo = getSerialNumber();
  if ( strSerNo.equals( F("0105X5") ) ) {
    const byte value[] PROGMEM = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else {
//    macPlanet = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
  }
}
  

void printMACAddress( EthernetClient client ) {
//  client.print( F("[getMACAddress() is disabled]") ); return;
  
  client.print( F("[ ") );
  for ( int i=0; i<6; i++ ) {
    formatHexDigit( client, macPlanet[i] );
    client.print( F(" ") );
  }
  client.print( F("]") );
}


String getSerialNumber() {
  for (int i=0; i<6; i++) {
    sIDRead[i] = EEPROM.read(i);
  }
  String strSerialNumber = String( sIDRead );
  return strSerialNumber;
}


String getVersion() {
  return strVersion;
}


void printNameValue(  EthernetClient client,
                      String strName,
                      String strValue ) {
//  Serial.println( "--- printNameValue(), \"" + strName + "\" = \"" + strValue + "\"" );
  client.print( F( "<tr><td>" ) );
  client.print( strName );
  client.print( F( "</td><td><tt>" ) );
  client.print( strValue );
  client.println( F( "</tt></td></tr>" ) );
}


void printSection( EthernetClient client,
                   String strTitle ) {
  client.print( F( "<tr><th colspan='2'>" ) );
  client.print( strTitle );
  client.print( F( "</th></tr>" ) );
}


int freeRam() {
  extern int __heap_start, *__brkval; 
  int v; 
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval); 
}





String sendAtom() {
//  if ( 0==strStarHost.length() ) return "strStarHost not set";
//  if ( 0==arrStarIP[0] ) return "Star host IP not set";
  
  EthernetClient client;
//  int iResult = client.connect( strStarHost, 80 );
//  int iResult = client.connect( arrStarIP, 80 );

  int iResult = client.connect( ipStar, 80 );
  
  if ( iResult < 0 ) {
    String strResult = "Failed to connect, connect() response: " + String( iResult );
    return strResult;
  }
  
  if ( !client ) {
    return F( "Client is false." );
  }
  
  client.print( F("GET /atom?") );
  for ( int iA = 0; iA < 6; iA++ ) {
    int iValue = analogRead( iA );
    client.print( F( "A" ) );
    client.print( String( iA ) );
    client.print( F( "=" ) );
    client.print( String( iValue ) );
    client.print( F( "&" ) );
  }
  for ( int iD = 2; iD < 14; iD++ ) {
    int iValue = digitalRead( iD );
    client.print( F( "D" ) );
    client.print( String( iD ) );
    client.print( F( "=" ) );
    client.print( String( iValue ) );
    client.print( F( "&" ) );
  }
  client.print( F( "SerNo=" ) );
  client.print( getSerialNumber() );
  client.print( F( "&Ver=" ) );
  client.print( strVersion );
  client.print( F( "&Mem=" ) );
  client.print( String( freeRam() ) );
  
  
  client.println( F(" HTTP/1.1") );
//  client.println( "Host: 192.168.1.4" );
  client.println( F("Connection: close") );
  client.println();

  String strResponse = "[begin]";
  boolean bRead = client.available();
//  boolean bRead = true;
  while ( bRead ) {
    const int i = client.read();
    if ( i>0 ) {
      const char c = (char) i;
      strResponse = strResponse + String( c );
    } else {
      bRead = false;
    }
  }
  strResponse = strResponse + "[end]";
  
  client.stop();
  
  String strResult = "Atom sent, response: " + strResponse;
  return strResult;
}






void processRequest( EthernetClient client ) {
  if ( !client.connected() ) return;

    /* pull the request from the stream */

    String strRequest;
    boolean bRead = true;
    while ( bRead ) {

      if ( client.available() ) {
        const int i = client.read();
        if ( -1==i ) {
          bRead = false;
        } else {
          char c = (char) i;
          strRequest = strRequest + c;
        }
      }
      
      if ( strRequest.indexOf( F("HTTP") )>-1 ) {
        bRead = false;
      }
    }
    
    /* extract info the request */
    
    const String strOriginal = strRequest;
    
    String strTemp;
    
    pop( strRequest, " " );
    strRequest = pop( strRequest, "HTTP" );
    strRequest.trim();
    
    String strCommand = strRequest + "/";
    const int iPosQMark = strCommand.indexOf( F("?") );
    const int iPosSlash = strCommand.indexOf( F("/"), 2 );
    if ( iPosQMark<0 || iPosQMark>iPosSlash ) {
      strCommand = strCommand.substring( 0, iPosSlash );
    } else {
      strTemp = strCommand;
      strCommand = pop( strTemp, "?" );
    }
    strCommand.toLowerCase();
    
    String strParams = strRequest;
    strParams = strParams.substring( strCommand.length() + 1 );

    String strName = strParams;
    strTemp = strName;
    strName = pop( strTemp, F("=") );
    strName.toLowerCase();
    
    String strValue = strParams;
    if ( strValue.indexOf( F("=") )>-1 ) {
      pop( strValue, "=" );
    } else {
      strValue = "";
    }
    
    #if defined( DEBUG )
      Serial.print( F("strRequest = ") );
      Serial.println( strRequest );
      Serial.print( F("strCommand = ") );
      Serial.println( strCommand );
      Serial.print( F("strParams = ") );
      Serial.println( strParams );
      Serial.print( F("strName = ") );
      Serial.println( strName );
      Serial.print( F("strValue = ") );
      Serial.println( strValue );
    #endif
   
    /* process the changes requested */ 
    
    String strMessage = F("(no message)");
    
   
    if ( strCommand.equals( F("/set") ) ) {
      
//      Serial.println( "(command is to set)" );

      if ( strName.equals( F("hostname") ) ) {
        
        strStarHost = strValue;
        strMessage = "host set to \"" + strStarHost + "\"";

      } else if ( strName.equals( F("host_ip") ) ) {

//        Serial.println( "(config value is host_ip)" );
        
        String strIP = strValue + ".";
        
//        Serial.println( "strIP = " + strIP );
        Serial.println( F("strIP = ") );
        Serial.println( strIP );

        String strOct1 = pop( strIP, "." );
        strOct1.trim();
        Serial.print( F("strOct1 = ") );
        Serial.println( strOct1 );
        const int iOct1 = strOct1.toInt();
        
        String strOct2 = pop( strIP, "." );
        strOct2.trim();
        Serial.print( F("strOct2 = ") );
        Serial.println( strOct2 );
        const int iOct2 = strOct2.toInt();
        
        String strOct3 = pop( strIP, "." );
        strOct3.trim();
        Serial.print( F("strOct3 = ") );
        Serial.println( strOct3 );
        const int iOct3 = strOct3.toInt();
        
        String strOct4 = pop( strIP, "." );
        strOct4.trim();
        Serial.print( F("strOct4 = ") );
        Serial.println( strOct4 );
        const int iOct4 = strOct4.toInt();
        
        arrStarIP[0] = iOct1;
        arrStarIP[1] = iOct2;
        arrStarIP[2] = iOct3;
        arrStarIP[3] = iOct4;

        ipStar = IPAddress( arrStarIP[0], arrStarIP[1], arrStarIP[2], arrStarIP[3] );

        strMessage = "Host IP set to " + strValue;
        
      } else if ( strName.equals( F("interval") ) ) {
        
        String strInterval = strValue;
        strInterval.trim();
        int iValue = strInterval.toInt();
        
        if ( strInterval.equals("0") ) {
          
          iInterval = 0;
          scheduleSend( 0 );
          strMessage = "Schedule send disabled.";
          
        } else if ( iValue>0 ) {
  
          iInterval = iValue;
          scheduleSend( getSystemTime() );
          strMessage = "interval set to " + String( iValue ) + ".";

        } else {
          
          strMessage = "Invalid interval value: \"" + strValue + "\".";
          
        }
        
      } else if ( strName.equals( F("time") ) ) {
        
        String strTime = strValue;
        strTime.trim();
        long lValue = strTime.toInt();
        if ( lValue>0 ) {
          
          const long lRunningTime = millis();
          lTime = lValue - lRunningTime;
          strMessage = "Time offset set to " + String( lTime ) + ".";

          scheduleSend( getSystemTime() );
          
        } else {
          
          strMessage = "Invalid time value: \"" + strValue + "\".";
          
        }
        
      } else {
          strMessage = "Invalid variable: \"" + strName + "\".";
      }
      
    } else if ( strCommand.equals( F("/send") ) ) {

      Serial.println( F("(request to send atom)") );
      
      String strResult = sendAtom();

      strMessage = "Request to send atom, result: " + strResult;
      
    } else if ( strCommand.equals( F("/read") ) ) {

      Serial.println( F("(command is to read)") );

      strMessage = F("Read request recognized.");
      
    } else {

      Serial.println( F("(command is unknown)") );
      
      strMessage = "Unknown command: \"" + strCommand + "\", available commands: \"/set\".";
      
    }
    
    
    
    /* write the response back to the client */

//    Serial.println( "Sending response back to client.." );

    // send a standard http response header
    client.println( F("HTTP/1.1 200 OK" ) );
    client.println( F("Content-Type: text/html" ) );
    client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//    client.println("Refresh: 1");  // refresh the page automatically every 5 sec
    client.println();
    client.println( F("<!DOCTYPE HTML>" ) );
    client.println( F("<html>" ) );
    
    client.println( F("<font face='verdana'>" ) );
    client.println( F("<table border='1' cellpadding='4'>" ) );
    
    printSection( client, F("Configuration") );
    printNameValue( client, F("Serial Number"), getSerialNumber() );
    
//    printNameValue( client, F("MAC Address"), getMACAddress() );
    // print mac address
    client.print( F( "<tr><td>" ) );
    client.print( F("MAC Address") );
    client.print( F( "</td><td><tt>" ) );
    printMACAddress( client );
    client.print( F( "</tt></td></tr>" ) );    
    
//    client.println( printNameValue( "MAC Address", macPlanet ) );
    printNameValue( client, F("Sketch Version"), getVersion() );
    printNameValue( client, F("Running time"), String( millis() ) );
    printNameValue( client, F("System time"), String( getSystemTime() ) );
    printNameValue( client, F("Scheduled send"), String( lNextSend ) );
    printNameValue( client, F("strStarHost"), strStarHost );
    // print star host IP
    client.print( F( "<tr><td>" ) );
    client.print( F("Star Host IP") );
    client.print( F( "</td><td>" ) );
    printStarIP( client );
    client.print( F( "</td></tr>" ) );    

    printNameValue( client, F("iInterval"), String( iInterval ) );
//    client.println( printNameValue( "memory", String( free_ram() ) ) );
    printNameValue( client, F("memory"), String( freeRam() ) );

    
    printSection( client, F("Request Details") );
    printNameValue( client, F("strOriginal"), strOriginal );
    printNameValue( client, F("strRequest"), strRequest );
    printNameValue( client, F("strCommand"), strCommand );
    printNameValue( client, F("strParams"), strParams );
    printNameValue( client, F("strName"), strName );
    printNameValue( client, F("strValue"), strValue );
    
    printSection( client, F("Results") );
//    printNameValue( client, F("strMessage"), strMessage );
    client.print( F( "<tr><td>" ) );
    client.print( F("strMessage") );
    client.println( F( "</td><td><pre>" ) );
    client.println( strMessage );
    client.println( F( "</pre>\n</td></tr>" ) );    
    
//    client.println( printSection( "Data Points" ) );
    printSection( client, F("Data Points") );
    // output the value of each analog input pin
    for ( int iA = 0; iA < 6; iA++ ) {
      int iValue = analogRead( iA );
      client.print( F( "<tr><td>" ) );
      client.print( F( "Analog Input " ) );
      client.print( String( iA ) );
      client.print( F( "</td><td><tt>" ) );
      client.print( String( iValue ) );
      client.println( F( "</tt></td></tr>" ) );
    }
    for ( int iD = 2; iD < 14; iD++ ) {
      int iValue = digitalRead( iD );
      client.print( F( "<tr><td>" ) );
      client.print( F( "Digital Input " ) );
      client.print( String( iD ) );
      client.print( F( "</td><td><tt>" ) );
      client.print( String( iValue ) );
      client.println( F( "</tt></td></tr>" ) );
    }

    
    client.println( F("</table></font>" ) );
    
  client.println( F("</html>" ) );
//  Serial.println( "Response sent completely." );
}


void scheduleSend( long lTimeReference ) {
  lNextSend = lTimeReference + iInterval;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);

  resolveMACAddress();
  Ethernet.begin( macPlanet, ipPlanet );
  server.begin();
  Serial.print( F("server is ") );
  Serial.println( Ethernet.localIP() );
}


void loop() {
  // put your main code here, to run repeatedly:

  EthernetClient client = server.available();
  if (client) {
    Serial.println( F("new client") );
    
    processRequest( client );
    delay(1);
    client.stop();
  }


  const long lTimeNow = getSystemTime();
  if ( (lTimeNow>0) && (lNextSend>0) && (lTimeNow>lNextSend) ) {
    sendAtom();
    scheduleSend( lTimeNow );
  }

}

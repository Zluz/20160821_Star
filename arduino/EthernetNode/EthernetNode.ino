/****************************************
  EthernetNode_002
 ****************************************/


/* Included libraries */
#include <SPI.h>
#include <Ethernet.h>
#include <EEPROM.h>

/* Constants */

const String strVersion = "20160903_001";

byte macPlanet[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
IPAddress ipPlanet( 192,168,1,3 );

/* Globals */

char sIDRead[7];

String strStarHost;
int iInterval;

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


/* figure this out later */
String printHex(  int num, 
                  int precision ) {
  char tmp[16];
  char format[128];
  
//  sprintf(format, "0x%%.%dX", precision);
//  sprintf(format, "0x%%dX", precision);
  
  sprintf(tmp, format, num);
//     Serial.print(tmp);
  String strValue = String( tmp );
  return strValue;
}

String formatHexDigit( int num ) {
  String strHexValue = String( num, HEX );
  if ( num < 11 ) {
    strHexValue = "0" + strHexValue;
  }
  strHexValue.toUpperCase();
  strHexValue = "0x" + strHexValue;
  return strHexValue;
}

long getSystemTime() {
  if ( lTime>0 ) {
    return lTime + millis();
  } else {
    return 0;
  }
}

String getMACAddress() {
  const String strSerNo = getSerialNumber();
  if ( strSerNo.equals( "0105X5" ) ) {
    byte value[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else {
//    macPlanet = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
  }
  
  String strMAC = "[ ";
  for ( int i=0; i<6; i++ ) {
    strMAC = strMAC + formatHexDigit( macPlanet[i] ) + " ";
  }
  strMAC = strMAC + "]";
//  String strMAC = String( macPlanet );
  return strMAC;
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


String printNameValue(  String strName,
                        String strValue ) {
  String strHTML = "<tr><td>"
          + strName + "</td><td><tt>"
          + strValue + "</tt></td></tr>";
  return strHTML;
}

String printSection( String strTitle ) {
  String strHTML = "<tr><th colspan='2'>"
          + strTitle + "</th></tr>";
  return strHTML;
}


void processRequest( EthernetClient client ) {
  if ( !client.connected() ) return;

    /* pull the request from the stream */

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
    
    /* extract info the request */
    
    String strOriginal = strRequest;
    
    pop( strRequest, " " );
    strRequest = pop( strRequest, "HTTP" );
    strRequest.trim();
    
    String strPath = strRequest;
    strPath = pop( strPath, "?" );
    strPath.toLowerCase();
    
    String strParams = strRequest;
    pop( strParams, "?" );
    
    String strName = strParams;
    strName = pop( strName, "=" );
    strName.toLowerCase();
    
    String strValue = strParams;
    pop( strValue, "=" );
    
   
    /* process the changes requested */ 
    
    String strMessage = "(no message)";
    
   
    if ( strPath.equals( "/set" ) ) {
      
      if ( strName.equals( "host" ) ) {
        
        strStarHost = strValue;
        strMessage = "host set to \"" + strStarHost + "\"";
        
      } else if ( strName.equals( "interval" ) ) {
        
        String strInterval = strValue;
        strInterval.trim();
        int iValue = strInterval.toInt();
        if ( iValue>0 ) {
          
          iInterval = iValue;
          strMessage = "interval set to " + String( iValue ) + ".";
          
        } else {
          
          strMessage = "Invalid interval value: \"" + strValue + "\".";
          
        }
        
      } else if ( strName.equals( "time" ) ) {
        
        String strTime = strValue;
        strTime.trim();
        long lValue = strTime.toInt();
        if ( lValue>0 ) {
          
          lTime = lValue;
          strMessage = "time set to " + String( lValue ) + ".";
          
        } else {
          
          strMessage = "Invalid time value: \"" + strValue + "\".";
          
        }
        
      }
      
    } else if ( strPath.equals( "/read" ) ) {

      strMessage = "Read request recognized.";
      
    } else {
      
      strMessage = "Unknown command: \"" + strPath + "\", available commands: \"/set\".";
      
    }
    
    
    
    
    /* write the response back to the client */

    // send a standard http response header
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: text/html");
    client.println("Connection: close");  // the connection will be closed after completion of the response
//    client.println("Refresh: 1");  // refresh the page automatically every 5 sec
    client.println();
    client.println("<!DOCTYPE HTML>");
    client.println("<html>");
    
    client.println( "<font face='verdana'>" );
    client.println( "<table border='1' cellpadding='4'>" );
    
    client.println( printSection( "Configuration" ) );
    client.println( printNameValue( "Serial Number", getSerialNumber() ) );
    client.println( printNameValue( "MAC Address", getMACAddress() ) );
//    client.println( printNameValue( "MAC Address", macPlanet ) );
    client.println( printNameValue( "Sketch Version", getVersion() ) );
    client.println( printNameValue( "Time since started", String( millis() ) ) );
    client.println( printNameValue( "Internal time", String( getSystemTime() ) ) );
    client.println( printNameValue( "strStarHost", strStarHost ) );
    client.println( printNameValue( "iInterval", String( iInterval ) ) );
    
    client.println( printSection( "Request Details" ) );
    client.println( printNameValue( "strOriginal", strOriginal ) );
    client.println( printNameValue( "strRequest", strRequest ) );
    client.println( printNameValue( "strPath", strPath ) );
    client.println( printNameValue( "strParams", strParams ) );
    client.println( printNameValue( "strName", strName ) );
    client.println( printNameValue( "strValue", strValue ) );
    
    client.println( printSection( "Results" ) );
    client.println( printNameValue( "strMessage", strMessage ) );
    
    
    client.println( printSection( "Data Points" ) );
    // output the value of each analog input pin
    for ( int iA = 0; iA < 6; iA++ ) {
      int iValue = analogRead( iA );
      const String strName = "Analog Input " + String( iA );
      const String strLine = printNameValue( strName, String( iValue ) );
      client.println( strLine );
    }
    for ( int iD = 2; iD < 14; iD++ ) {
      int iValue = digitalRead( iD );
      const String strName = "Digital Input " + String( iD );
      const String strLine = printNameValue( strName, String( iValue ) );
      client.println( strLine );
    }

    
    client.println( "</table></font>" );
    
  client.println( "</html>" );
}



void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);

  getMACAddress();
  Ethernet.begin( macPlanet, ipPlanet );
  server.begin();
  Serial.print("server is ");
  Serial.println( Ethernet.localIP() );
}


void loop() {
  // put your main code here, to run repeatedly:

  EthernetClient client = server.available();
  if (client) {
    Serial.println("new client");
    
    processRequest( client );
    delay(1);
    client.stop();
  }


}
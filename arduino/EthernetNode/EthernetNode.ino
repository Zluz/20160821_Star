/****************************************
  EthernetNode

Compiled for UNOs
Sketch uses 30,328 bytes (94%) of program storage space. Maximum is 32,256 bytes.
Global variables use 611 bytes (29%) of dynamic memory, leaving 1,437 bytes for local variables. Maximum is 2,048 bytes.

 ****************************************/


/* Included libraries */
#include "DHT.h"
#include <SPI.h>
#include <Ethernet.h>
#include <EEPROM.h>
#include <avr/pgmspace.h>
#include "Arduino.h"
#include "DefineMessages.h"


#define DEBUG // set to DEBUG to apply


// DHT options
#define DHTPIN 2     // what digital pin we're connected to
// Uncomment whatever type you're using!
#define DHTTYPE DHT11   // DHT 11
//#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
//#define DHTTYPE DHT21   // DHT 21 (AM2301)
DHT dht(DHTPIN, DHTTYPE);



/* Constants */

const static String VERSION        = "20160907_001";
//const static String COMMA          PROGMEM = ", ";
//const static String OP_SET         PROGMEM = "/set";
//const static String OP_SEND        PROGMEM = "/send";
//const static String OP_MODE        PROGMEM = "/mode";
//const static String OP_READ        PROGMEM = "/read";
//const static String OP_WRITE       PROGMEM = "/write";

//const static String FIELD_HOSTNAME PROGMEM = "hostname";
//const static String FIELD_HOST_IP  PROGMEM = "host_ip";
//const static String FIELD_INTERVAL PROGMEM = "interval";
//const static String FIELD_TIME     PROGMEM = "time";

//const static char VERSION[] PROGMEM = "20160906_001";
//const static char COMMA[] PROGMEM = {", "};
//const static char OP_SET[] PROGMEM = "/set";
//const static char OP_READ[] PROGMEM = {"/read"};
//const static char OP_SEND[] PROGMEM = {"/send"};
//const static char FIELD_HOSTNAME[] PROGMEM = {"hostname"};
//const static char FIELD_HOST_IP[] PROGMEM = {"host_ip"};
//const static char FIELD_INTERVAL[] PROGMEM = {"interval"};
//const static char FIELD_TIME[] PROGMEM = {"time"};


const static byte byteActivityLED = 13;




/* Globals */

static byte macPlanet[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
IPAddress ipPlanet( 192,168,1,3 );
IPAddress ipStar( 192,168,1,210 );
static byte arrStarIP[] = { 192,168,1,210 };
byte byteFailedAttempts = 0;
unsigned long lSuccessAttempts = 0;
unsigned long lLastSendTime = 0;
String strLastFailedMessage = "";

const static long MAX_PIN = 13; // pins 0 and 1 are reserved. 2 to 13 are usable [on Uno].
// output pin modes   0=undefined, 1=read mode, 2=write digital, 3=write analog
//                           x  x  2  3  4  5  6  7  8  9 10 11 12 13
static byte arrPinMode[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
static byte arrPinLast[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

//static char arrFloatToStr[16];

char sIDRead[7];

String strStarHost;
unsigned int iInterval;
unsigned long lNextSend;

EthernetServer server(80);

unsigned long lTime;




/* Functions */


//String printTimeValue( EthernetClient client,
//                       const long lTime ) {
//  /* 14 is mininum width, 3 is precision; float value is copied onto str_temp*/
//  float fTime = ((float)lTime) / 1000;
////  dtostrf( fTime, 11, 3, &arrFloatToStr[0] );
//  dtostrf( fTime, 11, 3, arrFloatToStr );
////  sprintf( arrFloatToStr, "%s F", arrFloatToStr );
//  client.print( arrFloatToStr );
//  client.print( F(" s") );
//}


String printTimeValue( EthernetClient client,
                       const unsigned long lTime ) {
  client.print( ((float)lTime) / 1000 );
  client.print( F(" s") );

//  client.print( lTime );
//  client.print( F(" ms") );
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
  } else if ( strSerNo.equals( F("0101X1") ) ) {
    const byte value[] PROGMEM = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else {
//    macPlanet = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
  }
}
  

void printMACAddress( EthernetClient client ) {
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
  return VERSION;
}


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


int freeRam() {
  extern int __heap_start, *__brkval; 
  int v; 
  return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval); 
}





int sendAtom( int iSendCode ) {
  if ( 0==arrStarIP[0] ) {
    lNextSend = 0;
    iInterval = 0;

//    return F("Star host IP not set");
    return MSG_SEND_FAILED_NO_STAR_HOST;
  }
  
  Serial.println( F("--> sendAtom()") );
  
  // light up activity LED
  digitalWrite( byteActivityLED, HIGH );

  // HTTP client to star host
  EthernetClient client;

  Serial.println( F("    sendAtom() - connect()") );

  int iResult = client.connect( ipStar, 80 );
  
  if ( iResult < 0 ) {
    String strResult = "Failed to connect, connect() response: " + String( iResult );
    Serial.println( F("<-- sendAtom(); iResult < 0") );
    
    strLastFailedMessage = F("Failed to connect");

    // turn off activity LED
    digitalWrite( byteActivityLED, LOW );
    // return
//    return strResult;
    return MSG_SEND_FAILED_TO_CONNECT;
  }
  
  Serial.println( F("    sendAtom() - testing client") );

  if ( !client ) {
    byteFailedAttempts = byteFailedAttempts + 1;
    
    if ( byteFailedAttempts > 2 ) {
      lNextSend = 0;
      iInterval = 0;
      strLastFailedMessage = F("Client is false. Disabling schedule.");
    } else {
      strLastFailedMessage = F("Client is false. Not yet disabling schedule.");
    }

    Serial.print( F("<-- sendAtom(); !client, byteFailedAttempts = ") );
    Serial.println( byteFailedAttempts );
    
    // turn off activity LED
    digitalWrite( byteActivityLED, LOW );
    // return
//    return F( "Client is false." );
    return MSG_SEND_FAILED_NO_CLIENT;
  }

  Serial.println( F("    sendAtom() - print()") );
  
  lLastSendTime = getSystemTime();
  
  client.print( F("GET /atom?") );
  
  client.print( F( "SendCode=" ) );
  client.print( String( iSendCode ) );
  client.print( F( "&SerNo=" ) );
  client.print( getSerialNumber() );
  client.print( F( "&Ver=" ) );
  client.print( getVersion() );
  client.print( F( "&Mem=" ) );
  client.print( String( freeRam() ) );
  
  for ( int iA = 0; iA < 6; iA++ ) {
    int iValue = analogRead( iA );
    client.print( F( "&A" ) );
    client.print( String( iA ) );
    client.print( F( "=" ) );
    client.print( String( iValue ) );
  }
  for ( int iD = 2; iD < 14; iD++ ) {
    int iValue = digitalRead( iD );
    client.print( F( "&D" ) );
    client.print( String( iD ) );
    client.print( F( "=" ) );
    client.print( String( iValue ) );
  }


  
  // DHT11: temperature and humidity

  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
  float fHumidity = dht.readHumidity();
//  // Read temperature as Celsius (the default)
//  float fTemperature = dht.readTemperature();
  // Read temperature as Fahrenheit (isFahrenheit = true)
  float fTemperature = dht.readTemperature(true);

  // Check if any reads failed and exit early (to try again).
  if ( isnan(fHumidity) || isnan(fTemperature) ) {
    client.print( F( "&Temp=NA&Humid=NA" ) );
  } else {
    client.print( F( "&Temp=" ) );
    client.print( String( fTemperature ) );
    client.print( F( "&Humid=" ) );
    client.print( String( fHumidity ) );
  }
  
  
 
  
  
  
  
  
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
  
  byteFailedAttempts = 0;
  lSuccessAttempts = lSuccessAttempts + 1;
  
  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );

  // return
  String strResult = "Atom sent, response: " + strResponse;
  Serial.println( F("<-- sendAtom(), normal") );
//  Serial.print( F("    sendAtom(), response = ") );
//  Serial.println( strResponse );
//  return strResult;
  return MSG_SEND_SUCCESS;
}






void processRequest( EthernetClient client ) {
  if ( !client.connected() ) return;

    /* pull the request from the stream */
    
    // light up activity LED
    digitalWrite( byteActivityLED, HIGH );

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

    // turn off activity LED
    digitalWrite( byteActivityLED, LOW );
    
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
    
//    String strMessage = F("(no message)");
    String strMsgText = "";
    int iMsgCode = 0;
    boolean bSendFullResponse = true;
   
    if ( strCommand.equals( F("/set") ) ) {
//    if ( strCommand.equals( OP_SET ) ) {
      
//      Serial.println( "(command is to set)" );

      if ( strName.equals( F("hostname") ) ) {
//      if ( strName.equals( FIELD_HOSTNAME ) ) {
        
        strStarHost = strValue;
//        strMessage = "host set to \"" + strStarHost + "\"";
        iMsgCode = MSG_STAR_HOSTNAME_SET;
        strMsgText = strStarHost;

      } else if ( strName.equals( F("host_ip") ) ) {
//      } else if ( strName.equals( FIELD_HOST_IP ) ) {

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

//        strMessage = "Host IP set to " + strValue;
        iMsgCode = MSG_STAR_IP_SET;
        strMsgText = strValue;
        
        sendAtom( SEND_CODE_NODE_INIT );
        
      } else if ( strName.equals( F("interval") ) ) {
//      } else if ( strName.equals( FIELD_INTERVAL ) ) {
        
        String strInterval = strValue;
        strInterval.trim();
        int iValue = strInterval.toInt();
        
        if ( strInterval.equals( F("0") ) ) {
          
          iInterval = 0;
          scheduleSend( 0 );
//          strMessage = "Schedule send disabled.";
          iMsgCode = MSG_SCHEDULE_DISABLED;
          
        } else if ( iValue>0 ) {
  
          iInterval = iValue;
          scheduleSend( getSystemTime() );
//          strMessage = "interval set to " + String( iValue ) + ".";
          iMsgCode = MSG_INTERVAL_SET;

        } else {
          
//          strMessage = "Invalid interval value: \"" + strValue + "\".";
          iMsgCode = MSG_INTERVAL_INVALID_VALUE;
          strMsgText = strValue;
          
        }
        
      } else if ( strName.equals( F("time") ) ) {
        
        String strTime = strValue;
        strTime.trim();
        long lValue = strTime.toInt();
        if ( lValue>0 ) {
          
          const long lRunningTime = millis();
          lTime = lValue - lRunningTime;
//          strMessage = "Time offset set to " + String( lTime ) + ".";
          iMsgCode = MSG_TIME_OFFSET_SET;
          strMsgText = strTime;

          scheduleSend( getSystemTime() );
          
        } else {
//          strMessage = "Invalid time value: \"" + strValue + "\".";
          iMsgCode = MSG_TIME_OFFSET_INVALID_VALUE;
          strMsgText = strValue;
        }
        
      } else {
//          strMessage = "Invalid variable: \"" + strName + "\".";
        iMsgCode = MSG_INVALID_VARIABLE;
        strMsgText = strName;
      }
      
    } else if ( strCommand.equals( F("/send") ) ) {
//    } else if ( strCommand.equals( OP_SEND ) ) {

      Serial.println( F("(request to send atom)") );
      
      iMsgCode = sendAtom( SEND_CODE_REQUESTED );
      //strMsgText = "";

      if ( strName.equals( "fast" ) ) {
        bSendFullResponse = false;
      }

//      strMessage = "Request to send atom, result: " + strResult;
//      strMsgText = strResult;

    } else if ( strCommand.equals( F("/mode") ) ) {

      const long lPin = strName.toInt();
      if ( lPin>1 && lPin<=MAX_PIN ) {
        const long lValue = strValue.toInt();
        if ( lValue>0 && lValue<4 ) {
          arrPinMode[lPin] = lValue;
          
          if ( 1==lValue ) {
            pinMode( lPin, INPUT );
          } else {
            pinMode( lPin, OUTPUT );
          }
//          strMessage = "Pin " + strName + " set to mode " + strValue;
          iMsgCode = MSG_SET_PIN_MODE_SUCCESS;
          strMsgText = strName;
        } else {
//          strMessage = "Failed to set pin mode. Invalid mode: " + strValue;
//          strMessage = "Invalid mode: " + strValue;
          iMsgCode = MSG_SET_PIN_MODE_INVALID_MODE;
          strMsgText = strValue;
        }
      } else {
//        strMessage = "Failed to set pin mode. Invalid pin: " + strName;
//        strMessage = "Invalid pin: " + strName;
        iMsgCode = MSG_SET_PIN_MODE_INVALID_PIN;
        strMsgText = strName;
      }
            
    } else if ( strCommand.equals( F("/write") ) ) {
//    } else if ( strCommand.equals( OP_WRITE ) ) {

      const long lPin = strName.toInt();
      if ( lPin>1 && lPin<=MAX_PIN ) {
        
        const long lValue = strValue.toInt();
        const byte byteMode = arrPinMode[lPin];
        
        if ( 2==byteMode ) { // write digital
          if ( lValue<2 ) {
            digitalWrite( lPin, lValue );
            iMsgCode = MSG_WRITE_DIGITAL_SUCCESS;
            strMsgText = strName;
          } else {
//            strMessage = "Attempt to write failed: value is invalid for digital pin: " + strValue;
//            strMessage = "Value is invalid for digital pin: " + strValue;
            iMsgCode = MSG_WRITE_DIGITAL_INVALID_VALUE;
            strMsgText = strValue;
          }
        } else if ( 3==byteMode ) { // write PWM
          if ( lValue<1024 ) {
            analogWrite( lPin, lValue );
            iMsgCode = MSG_WRITE_PWM_SUCCESS;
            strMsgText = strName;
          } else {
//            strMessage = "Attempt to write failed: value is invalid for PWM pin: " + strValue;
//            strMessage = "Value is invalid for PWM pin: " + strValue;
            iMsgCode = MSG_WRITE_PWM_INVALID_VALUE;
            strMsgText = strValue;
          }
        } else {
//          strMessage = "Attempt to write failed: Pin is invalid mode: " + byteMode;
//          strMessage = "Pin is in invalid mode: " + byteMode;
            iMsgCode = MSG_WRITE_PIN_INVALID_MODE;
            strMsgText = String( byteMode );
        }

      } else {
//        strMessage = "Failed to write to pin. Invalid pin: " + strName;
//        strMessage = "Invalid pin: " + strName;
        iMsgCode = MSG_WRITE_PIN_INVALID_PIN;
        strMsgText = strName;
      }
                  
    } else if ( strCommand.equals( F("/read") ) ) {
//    } else if ( strCommand.equals( String( OP_READ ) ) ) {

      Serial.println( F("(command is to read)") );

//      strMessage = F("Read request recognized.");
      iMsgCode = MSG_OP_READ_SUCCESS;
//      strMsgText = ""; // nothing to say
      
    } else {

      Serial.println( F("(command is unknown)") );
      
//      strMessage = "Unknown command: \"" + strCommand + "\".";
//      strMessage = "Unknown command: \"" + strCommand + "\", available commands:\n";
//          + OP_SET + " (" + FIELD_HOSTNAME + COMMA
//              + FIELD_HOST_IP + COMMA + FIELD_INTERVAL + COMMA + FIELD_TIME + F(")") + COMMA
//          + OP_READ + COMMA 
//          + OP_SEND;
      iMsgCode = MSG_OP_UNKNOWN;
      strMsgText = strCommand;
    }
    
    
    
    /* write the response back to the client */
    
    
    
    // light up activity LED
    digitalWrite( byteActivityLED, HIGH );

//    Serial.println( "Sending response back to client.." );

    // send a standard http response header
    client.println( F("HTTP/1.1 200 OK" ) );
//    client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//    client.println("Refresh: 1");  // refresh the page automatically every 5 sec
//    client.println();
    
    if ( bSendFullResponse ) {

      sendHTMLHeader( client );

//      client.println( F("Content-Type: text/html" ) );
//
//      client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//      client.println();
//
//      client.println( F("<!DOCTYPE HTML>" ) );
//      client.println( F("<html>" ) );
      
      client.println( F("<font face='verdana'>" ) );
      client.println( F("<table border='1' cellpadding='4'>" ) );
      
      printSection( client, F("Configuration") );
      printNameValue( client, F("Serial Number"), getSerialNumber() );
      
  //    printNameValue( client, F("MAC Address"), getMACAddress() );
      // print mac address
      client.print( F( "<tr><td colspan='2'>" ) );
      client.print( F("MAC Address") );
      client.print( F( "</td><td><tt>" ) );
      printMACAddress( client );
      client.print( F( "</tt></td></tr>" ) );    
  
  //    client.println( printNameValue( "MAC Address", macPlanet ) );
      printNameValue( client, F("Sketch Version"), getVersion() );
  
      printNameValue( client, F("SRAM"), String( freeRam() ) + F(" bytes") );
      
  
      client.print( F( "<tr><td colspan='3' align='center'> Star Host </td></tr>" ) );    
  
      printNameValue( client, F("Hostname"), F("hostname"), strStarHost );
      // print star host IP
      client.print( F( "<tr><td>Host IP</td><td><tt>host_ip</tt></td><td><tt>" ) );
      printStarIP( client );
      client.print( F( "</tt></td></tr>" ) );    
  
      printNameValue( client, F("Send, total success"), String( lSuccessAttempts ) );
      printNameValue( client, F("Send, recent failed"), String( byteFailedAttempts ) );
      printNameValue( client, F("Send, last failed message"), strLastFailedMessage );
      
      printNameValue( client, F("Last success time"), String( lLastSendTime ) );
  //    client.print( F( "<tr><td colspan='2'> Last success time </td><td><tt>" ) );
  //    printTimeValue( client, lLastSendTime );
  //    client.println( F( "</tt>\n</td></tr>" ) );    
  
      client.print( F( "<tr><td colspan='3' align='center'>Time (in ms)</td></tr>" ) );
      
      printNameValue( client, F("Running time"), String( millis() ) );
  //    client.print( F( "<tr><td> Running time </td><td><tt> time </tt></td><td><tt>" ) );
  //    printTimeValue( client, millis() );
  //    client.println( F( "</tt>\n</td></tr>" ) );
      
      printNameValue( client, F("System time"), F("time"), String( getSystemTime() ) );
  //    client.print( F( "<tr><td colspan='2'> System time </td><td><tt>" ) );
  //    printTimeValue( client, getSystemTime() );
  //    client.println( F( "</tt>\n</td></tr>" ) );    
          
      printNameValue( client, F("Send interval"), F("interval"), String( iInterval ) );
      printNameValue( client, F("Scheduled send"), String( lNextSend ) );
  
      
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
      client.print( F("Message") );
      client.println( F( "</td><td>" ) );
      client.println( String( iMsgCode ) );
      client.println( F( "</td><td>" ) );
      client.println( strMsgText );
      client.println( F( "</td></tr>" ) );    
      
  //    client.println( printSection( "Data Points" ) );
      printSection( client, F("Data Points") );
      // output the value of each analog input pin
      client.print( F( "<tr><td colspan='3' align='center'>Analog Inputs</td></tr>" ) );    
      for ( int iA = 0; iA < 6; iA++ ) {
        int iValue = analogRead( iA );
        client.print( F( "<tr><td colspan='2'>" ) );
        client.print( F( "Analog Input " ) );
        client.print( String( iA ) );
        client.print( F( "</td><td><tt>" ) );
        client.print( String( iValue ) );
        client.println( F( "</tt></td></tr>" ) );
      }
      client.print( F( "<tr><td colspan='3' align='center'>Digital Inputs</td></tr>" ) );    
      for ( int iD = 2; iD <= MAX_PIN; iD++ ) {
        int iValue = digitalRead( iD );
        client.print( F( "<tr><td>Digital Input" ) );
        client.print( String( iD ) );
        client.print( F( "</td><td>" ) );
        const byte byteMode = arrPinMode[iD];
        if ( 0==byteMode ) {
          client.print( F("0:undef") );
        } else if ( 1==byteMode ) {
          client.print( F("1:input") );
        } else if ( 2==byteMode ) {
          client.print( F("2:out digital") );
        } else if ( 3==byteMode ) {
          client.print( F("3:out PWM") );
        } else {
          client.print( String( byteMode ) );
          client.print( F(":INVALID") );
        }
        client.print( F( "</td><td><tt>" ) );
        client.print( String( iValue ) );
        client.println( F( "</tt></td></tr>" ) );
      }
  
      
      client.println( F("</table></font>" ) );
    
      client.println( F("</html>" ) );

    } else {
      
//      client.println( F("Content-Type: text/plain" ) );
//      client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//      client.println();
//      client.print( F("iMsgCode: " ) );
//      client.println( String( iMsgCode ) );
      
      sendHTMLHeader( client );
      client.print( F("<tt>iMsgCode: " ) );
      client.println( String( iMsgCode ) );
      client.print( F("</tt></html>" ) );
    }
    
//  Serial.println( "Response sent completely." );

    
    
  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );
}


void sendHTMLHeader( EthernetClient client ) {
  client.println( F("Content-Type: text/html" ) );
  client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
  client.println();
  client.println( F("<!DOCTYPE HTML>" ) );
  client.println( F("<html>" ) );
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
      client.print( F( "  \"A" ) );
      client.print( String( iA ) );
      client.print( F( "\": " ) );
      client.print( String( iValue ) );
      client.println( F( "," ) );
    }
    for ( int iD = 2; iD < 14; iD++ ) {
      int iValue = digitalRead( iD );
      client.print( F( "  \"D" ) );
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


void scheduleSend( long lTimeReference ) {
  if ( iInterval>0 && lTimeReference>0 ) {
    lNextSend = lTimeReference + iInterval;
  } else {
    lNextSend = 0;
  }
}


boolean hasDataChanged() {
  
  boolean bResult = false;
  
  for ( int iD = 2; iD <= MAX_PIN; iD++ ) {
    const byte byteMode = arrPinMode[iD];
    if ( 1==byteMode ) { // digital input
    
      const byte byteValue = digitalRead( iD );
      const byte byteLast = arrPinLast[iD];

      if ( byteValue != byteLast ) {
        arrPinLast[iD] = byteValue;
        bResult = true;
      }    
    }
  }
  return bResult;
}



void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  
  pinMode( byteActivityLED, OUTPUT );

  dht.begin();

  resolveMACAddress();
  Ethernet.begin( macPlanet, ipPlanet );
  server.begin();
  Serial.print( F("server is ") );
  Serial.println( Ethernet.localIP() );
  
  sendAtom( SEND_CODE_NODE_INIT );
}




void loop() {
  // put your main code here, to run repeatedly:


  /* check for HTTP requests */
  EthernetClient client = server.available();
  if (client) {
    Serial.println( F("new client") );
    
    processRequest( client );
    delay(1);
    client.stop();
  }


  /* check conditions to Send to Star */
  {
    boolean bUpdateTime = false;
  
    if ( hasDataChanged() ) {
      sendAtom( SEND_CODE_DIGITAL_CHANGE );
      bUpdateTime = true;
    }
  
    /* check to see if there is a Send scheduled */
    const long lTimeNow = getSystemTime();
    if ( (lTimeNow>0) && (lNextSend>0) && (lTimeNow>lNextSend) ) {
      sendAtom( SEND_CODE_SCHEDULED );
      bUpdateTime = true;
    }
    
    if ( bUpdateTime ) {
      scheduleSend( lTimeNow );
    }
  }

  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );
}

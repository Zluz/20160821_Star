/****************************************
  EthernetNode
  
  Arduino Mega 2560
Sketch uses 46,130 bytes (18%) of program storage space. Maximum is 253,952 bytes.
Global variables use 1,658 bytes (20%) of dynamic memory, leaving 6,534 bytes for local variables. Maximum is 8,192 bytes.

  Arduino UNO - OUTDATED
Sketch uses 29,602 bytes (91%) of program storage space. Maximum is 32,256 bytes.
Global variables use 621 bytes (30%) of dynamic memory, leaving 1,427 bytes for local variables. Maximum is 2,048 bytes.

 ****************************************/

/* Select device. UNO program must fit in space. MEGA adds SD card support. */

//#define DEFINE_DEVICE_UNO
#define DEFINE_DEVICE_MEGA


/* Included libraries */
#include "DHT.h"
#include <SPI.h>
#include <Ethernet.h>
#include <EEPROM.h>
#include <avr/pgmspace.h>
#include "Arduino.h"


const static String VERSION  = "20160929_001";


#if defined( DEFINE_DEVICE_MEGA )
#include <SD.h>
#endif



// DHT options
//#define PIN_DHT 2     // what digital pin we're connected to
#define DHT_TYPE DHT11   // DHT 11
DHT dht02( 02, DHT_TYPE );
DHT dht03( 03, DHT_TYPE );
//DHT dht04( 04, DHT_TYPE );

// SD card options
#define PIN_SD 4

const static byte byteActivityLED = 13;

//long FILE_LOG_VIEW_SIZE = 262144; // 256 K
long FILE_LOG_VIEW_SIZE = 4096; // 4 K

//const static long MANDATORY_UPTIME_REBOOT = 86400000; // milliseconds in a day
const static long MANDATORY_UPTIME_REBOOT = 3600000; // hour, 3600s/hr




/* Globals */

static byte macPlanet[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
//IPAddress ipPlanet( 192,168,1,3 );
//IPAddress ipPlanet( 192,168,1,202 );
IPAddress ipPlanet( 127,0,0,1 );
IPAddress ipStar( 192,168,1,200 );
static byte arrStarIP[] = { 192,168,1,200 };
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

////char* cTimestamp = malloc( 9 );
//char cTimestamp[20];
////char* cFilename = "1234567890123456789012345678901234567890";
//char cFilename[80];

const static long BUFFER_SIZE = 80;
static char cStringBuffer[ BUFFER_SIZE ];

//File fileLog;



#include "DefineMessages.h"
#include "Logging.h"
#include "Utilities.h"
#include "CommEthernet.h"
#include "CommSerial.h"



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


//String printTimeValue( EthernetClient client,
//                       const unsigned long lTime ) {
//  client.print( ((float)lTime) / 1000 );
//  client.print( F(" s") );
//
////  client.print( lTime );
////  client.print( F(" ms") );
//}


String formatTimeValue( const unsigned long lTime ) {
  String strText;
  long lBig = lTime / 1000;
  long lSmall = lTime % 1000;
  sprintf( cStringBuffer, "%07u", lBig );
  strText = String( cStringBuffer ) + F(".");
  sprintf( cStringBuffer, "%03u", lSmall );
  strText = strText + String( cStringBuffer );

  return strText;
}

void saveConfig( String strName,
                 String strValue ) {
#if defined( DEFINE_DEVICE_MEGA )
  log( "Saving config: \"" + strName + "\" = \"" + strValue + "\"" );
  
  strName.toUpperCase();
  
//  String strFilename = "ARDUINO/CONFIG/" + strName + ".CFG";
  String strFilename = "ARDUINO/" + strName + ".CFG";
//  String strFilename = strName + ".CFG";

  char cFilename[strFilename.length()+1];
  strFilename.toCharArray( cFilename, sizeof(cFilename) );

//  log( "Config filename: \"" + String( cFilename ) + "\"" );
  
  if ( SD.exists( cFilename ) ) {
//    log( F("Config file already exists. Deleting.") );
    SD.remove( cFilename );
//    delay( 100 );
  }
  File fileConfig = SD.open( cFilename, FILE_WRITE );
//  File fileConfig = SD.open( strFilename, O_WRITE | O_CREAT | O_TRUNC );
  if ( fileConfig ) {
//    log( F("Created, writing data") );
    fileConfig.println( strValue );
    fileConfig.close();
//    log( F("File complete") );
//  } else {
//    log( F("Failed to save configuration") );
  }
  
//  if ( SD.exists( cFilename ) ) {
//    log( F("Config file created") );
//  } else {
//    log( F("Config file NOT created") );
//  }

#endif  
}

String readConfig( String strName,
                   String strDefault ) {
#if defined( DEFINE_DEVICE_MEGA )

  log( "Reading config: \"" + strName + "\"  (default:\"" + strDefault + "\")" );

  strName.toUpperCase();

//  String strFilename = "ARDUINO/CONFIG/" + strName + ".CFG";
  String strFilename = "ARDUINO/" + strName + ".CFG";
//  String strFilename = strName + ".CFG";
  char cFilename[strFilename.length()+1];
//  strFilename.toCharArray( cStringBuffer, sizeof(cStringBuffer) );
  strFilename.toCharArray( cFilename, sizeof(cFilename) );
  
  if ( SD.exists( cFilename ) ) {
    File fileConfig = SD.open( cFilename, FILE_READ );
    String strValue = "";
    if ( fileConfig ) {
  
      boolean bScan = fileConfig.available();
      char c;
      while ( bScan ) {
        c = fileConfig.read();
        if ( '\n'==c ) {
          bScan = false;
        } else {
          strValue = strValue + c;
          bScan = fileConfig.available();
        }
      }
      fileConfig.close();
  
      log( "Loaded config: \"" + strName + "\" = \"" + strValue + "\"" );
      
      return strValue;
    } else {
      log( "Using default, unable to open file: " + String( cFilename ) );
      return strDefault;
    }
  } else {
    log( "Using default, file not found: " + String( cFilename ) );
    return strDefault;
  }
#else
  return strDefault;
#endif  
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
  if ( strSerNo.equals( F("0101X1") ) ) {
//    const byte value[] PROGMEM = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
    const byte value[] PROGMEM = { 0xBE, 0x01, 0x01, 0x01, 0x01, 0x01 };
    ipPlanet = IPAddress( 192,168,1,201 );
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else if ( strSerNo.equals( F("0102X2") ) ) {
    ipPlanet = IPAddress( 192,168,1,202 );
    const byte mac[] PROGMEM = { 0xBE, 0xEF, 0x8F, 0xB4, 0x6B, 0x11 };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = mac[i];
    }
  } else if ( strSerNo.equals( F("0103X3") ) ) {
    ipPlanet = IPAddress( 192,168,1,203 );
    const byte mac[] PROGMEM = { 0xBE, 0x01, 0x01, 0x01, 0x01, 0x03 };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = mac[i];
    }
  } else if ( strSerNo.equals( F("0105X5") ) ) {
    const byte value[] PROGMEM = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else if ( strSerNo.equals( F("102008") ) ) {
    ipPlanet = IPAddress( 192,168,1,208 );
    const byte value[] PROGMEM = { 0xBE, 0x01, 0x01, 0x01, 0x01, 0x08 };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else if ( strSerNo.equals( F("102009") ) ) {
    ipPlanet = IPAddress( 192,168,1,209 );
    const byte value[] PROGMEM = { 0xBE, 0x01, 0x01, 0x01, 0x01, 0x09 };
    for ( int i=0; i<6; i++ ) {
      macPlanet[i] = value[i];
    }
  } else {
//    macPlanet = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01 };
  }
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



int sendAtom( int iSendCode ) {
  int iResult;
  String strMessage = buildSendMessage( iSendCode );
//  if ( Serial ) {
    iResult = sendAtomSerial( iSendCode, strMessage );
//  } else {
    iResult = sendAtomHTTP( iSendCode, strMessage );
//  }
  return iResult;
}


int sendAtomSerial( int iSendCode,
                    String strMessage ) {

//  String strMessage = buildSendMessage( iSendCode );
  Serial.println( strMessage );

  return SEND_CODE_NOT_SUPPORTED;
}



String buildSendMessage( int iSendCode ) {
  
  String str = "";
  str += "/atom?";
  str += "SendCode=";
  str += String( iSendCode );
  
  str += "&SerNo=";
  str += getSerialNumber();
  str += "&Ver=";
  str += getVersion();
  str += "&Mem=";
  str += String( freeRam() );
  
  for ( int iA = 0; iA < 6; iA++ ) {
    int iValue = analogRead( iA );
    str += "&A";
    str += String( iA );
    str += "=";
    str += String( iValue );
  }
  for ( int iD = 2; iD < 14; iD++ ) {
    int iValue = digitalRead( iD );
    str += "&D";
    str += String( iD );
    str += "=";
    str += String( iValue );
  }

  str += buildSendMessageDHT11( dht02, "02" );
  str += buildSendMessageDHT11( dht03, "03" );
//  str += buildSendMessageDHT11( dht04, "04" );
  
  return str;
}

String buildSendMessageDHT11( DHT dht, String strSuffix ) {

  String str = "";
  // DHT11: temperature and humidity

  // Reading temperature or humidity takes about 250 milliseconds!
  // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
//  // Read temperature as Celsius (the default)
  // Read temperature as Fahrenheit (isFahrenheit = true)
  float fTemperature = dht.readTemperature(true);
//  float fTemperature = dht.readTemperature();
  float fHumidity = dht.readHumidity();

  // Check if any reads failed and exit early (to try again).
  if ( isnan(fHumidity) || isnan(fTemperature) ) {
//    str += "&Temp=NA&Humid=NA";
  } else {
    str += "&Temp" + strSuffix + "=";
    str += String( fTemperature );
    str += "&Humid" + strSuffix + "=";
    str += String( fHumidity );
  }

  return str;
}





int sendAtomHTTP( int iSendCode,
                  String strMessage ) {
  log( F("--> sendAtom()") );
  
  if ( 0==arrStarIP[0] ) {
    lNextSend = 0;
    iInterval = 0;

//    return F("Star host IP not set");
    log( F("<-- sendAtom(), no star host") );
    return MSG_SEND_FAILED_NO_STAR_HOST;
  }
  
//  Serial.println( F("--> sendAtom()") );
  
  // light up activity LED
  digitalWrite( byteActivityLED, HIGH );

  // HTTP client to star host
  EthernetClient client;

//  Serial.println( F("    sendAtom() - connect()") );

  int iResult = client.connect( ipStar, 80 );
  
  if ( iResult < 0 ) {
    String strResult = "Failed to connect, connect() response: " + String( iResult );
//    Serial.println( F("<-- sendAtom(); iResult < 0") );
    
    strLastFailedMessage = F("Failed to connect");

    // turn off activity LED
    digitalWrite( byteActivityLED, LOW );
    // return
//    return strResult;
    log( F("<-- sendAtom(), failed to connect") );
    return MSG_SEND_FAILED_TO_CONNECT;
  }
  
//  Serial.println( F("    sendAtom() - testing client") );

  if ( !client ) {
    byteFailedAttempts = byteFailedAttempts + 1;
    
    if ( byteFailedAttempts > 2 ) {
      lNextSend = 0;
      iInterval = 0;
      strLastFailedMessage = F("Client is false. Disabling schedule.");
    } else {
      strLastFailedMessage = F("Client is false. Not yet disabling schedule.");
    }

//    Serial.print( F("<-- sendAtom(); !client, byteFailedAttempts = ") );
//    Serial.println( byteFailedAttempts );
    
    // turn off activity LED
    digitalWrite( byteActivityLED, LOW );
    // return
//    return F( "Client is false." );
    log( F("<-- sendAtom(), no client") );
    return MSG_SEND_FAILED_NO_CLIENT;
  }

//  Serial.println( F("    sendAtom() - print()") );
  
  lLastSendTime = getSystemTime();
  
  client.print( F("GET ") );
  
//  String strMessage = buildSendMessage( iSendCode );
  client.print( strMessage );
  
  
  
  client.println( F(" HTTP/1.1") );
//  client.println( "Host: 192.168.1.4" );
  client.println( F("Connection: close") );
  client.println();

//  String strResponse = "[begin]";
  boolean bRead = client.available();
//  boolean bRead = true;
  while ( bRead ) {
    const int i = client.read();
    if ( i>0 ) {
      const char c = (char) i;
//      strResponse = strResponse + String( c );
    } else {
      bRead = false;
    }
  }
//  strResponse = strResponse + "[end]";
  
  client.stop();
  
  byteFailedAttempts = 0;
  lSuccessAttempts = lSuccessAttempts + 1;
  
  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );

  // return
//  String strResult = "Atom sent, response: " + strResponse;
//  Serial.println( F("<-- sendAtom(), normal") );
//  Serial.print( F("    sendAtom(), response = ") );
//  Serial.println( strResponse );
//  return strResult;
  log( F("<-- sendAtom(), success") );
  return MSG_SEND_SUCCESS;
}





int processRequestSet( String strName,
                       String strValue,
                       boolean bAllowSave ) {

  log( "Request to set: " + strName + " to " + strValue );                         
                         


//    String strMessage = F("(no message)");
    String strMsgText = "";
    int iMsgCode = 0;
//    boolean bSendFullResponse = true;
//    int iResponseMessageFormat = 2; // 1 = brief, 2 = full, 3 = showlog
   
  
  
  
//      Serial.println( "(command is to set)" );

      if ( strName.equals( F("hostname") ) ) {

        if ( bAllowSave ) {
          saveConfig( "hostname", strValue );
        }

        strStarHost = strValue;
//        strMessage = "host set to \"" + strStarHost + "\"";
        iMsgCode = MSG_STAR_HOSTNAME_SET;
        strMsgText = strStarHost;
        
        /*iMsgCode =*/ sendAtom( SEND_CODE_NODE_INIT ); //TODO add a new code

      } else if ( strName.equals( F("host_ip") ) ) {
        
        if ( bAllowSave ) {
          saveConfig( "host_ip", strValue );
        }

//        Serial.println( "(config value is host_ip)" );
        
        String strIP = strValue + ".";
        
//        Serial.println( "strIP = " + strIP );
//        Serial.println( F("strIP = ") );
//        Serial.println( strIP );

        String strOct1 = pop( strIP, "." );
        strOct1.trim();
//        Serial.print( F("strOct1 = ") );
//        Serial.println( strOct1 );
        const int iOct1 = strOct1.toInt();
        
        String strOct2 = pop( strIP, "." );
        strOct2.trim();
//        Serial.print( F("strOct2 = ") );
//        Serial.println( strOct2 );
        const int iOct2 = strOct2.toInt();
        
        String strOct3 = pop( strIP, "." );
        strOct3.trim();
//        Serial.print( F("strOct3 = ") );
//        Serial.println( strOct3 );
        const int iOct3 = strOct3.toInt();
        
        String strOct4 = pop( strIP, "." );
        strOct4.trim();
//        Serial.print( F("strOct4 = ") );
//        Serial.println( strOct4 );
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

        if ( bAllowSave ) {
          saveConfig( "interval", strValue );
        }

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

log( "Setting time: strValue = " + strValue );
        
        String strTime = strValue;
//log( "Setting time: strTime (1) = \"" + strTime + "\"" );
        strTime.trim();
//log( "Setting time: strTime (2) = \"" + strTime + "\"" );
        long lValue = strTime.toInt();

//log( "Setting time: strTime.toInt().." );

        String strTest = String( lValue );

//log( "Setting time: strTest = " + strTest );
        
//log( "Setting time: lValue = " + lValue );
        if ( lValue>0 ) {
//log( "Setting time (lValue>0)" );
          
          const long lRunningTime = millis();
          lTime = lValue - lRunningTime;
//          strMessage = "Time offset set to " + String( lTime ) + ".";
          iMsgCode = MSG_TIME_OFFSET_SET;
          strMsgText = strTime;

          scheduleSend( getSystemTime() );
          
        } else {
log( "Setting time (invalid time?)" );
//          strMessage = "Invalid time value: \"" + strValue + "\".";
          iMsgCode = MSG_TIME_OFFSET_INVALID_VALUE;
          strMsgText = strValue;
        }
        
      } else {
//          strMessage = "Invalid variable: \"" + strName + "\".";
        iMsgCode = MSG_INVALID_VARIABLE;
        strMsgText = strName;
      }


  return iMsgCode;
}



int processRequest( EthernetClient client ) {
  if ( !client.connected() ) return ERR_CLIENT_NOT_CONNECTED;

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

    log( "Incoming request: " + strRequest );
    
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
    
//    #if defined( DEFINE_DEBUG )
//      Serial.print( F("strRequest = ") );
//      Serial.println( strRequest );
//      Serial.print( F("strCommand = ") );
//      Serial.println( strCommand );
//      Serial.print( F("strParams = ") );
//      Serial.println( strParams );
//      Serial.print( F("strName = ") );
//      Serial.println( strName );
//      Serial.print( F("strValue = ") );
//      Serial.println( strValue );
//    #endif

   
    /* process the changes requested */ 
    
//    String strMessage = F("(no message)");
    String strMsgText = "";
    int iMsgCode = 0;
//    boolean bSendFullResponse = true;
    int iResponseMessageFormat = 2; // 1 = brief, 2 = full, 3 = showlog
   
    if ( strCommand.equals( F("/set") ) ) {
//    if ( strCommand.equals( OP_SET ) ) {

  
      log( F("Command: set") );
      
      
      iMsgCode = processRequestSet( strName, strValue, true );
      
      
      
      
    } else if ( strCommand.equals( F("/send") ) ) {
//    } else if ( strCommand.equals( OP_SEND ) ) {

      log( F("Command: send") );
  
//      Serial.println( F("(request to send atom)") );
      
      iMsgCode = sendAtom( SEND_CODE_REQUESTED );
      //strMsgText = "";

      if ( strName.equals( "fast" ) ) {
        iResponseMessageFormat = 1;
      }

//      strMessage = "Request to send atom, result: " + strResult;
//      strMsgText = strResult;

    } else if ( strCommand.equals( F("/mode") ) ) {

      log( F("Command: mode") );

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

      log( F("Command: write") );

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

      log( F("Command: read") );

//      Serial.println( F("(command is to read)") );

//      strMessage = F("Read request recognized.");
      iMsgCode = MSG_OP_READ_SUCCESS;
//      strMsgText = ""; // nothing to say
      
    } else if ( strCommand.equals( F("/showlog") ) ) {

      log( F("Command: showlog") );

#if defined( DEFINE_DEVICE_MEGA )
      iResponseMessageFormat = 3;
      iMsgCode = MSG_OP_SHOW_LOG_SUCCESS;
#else
      iResponseMessageFormat = 2;
      iMsgCode = MSG_OP_SHOW_LOG_ERR_NO_SD;
#endif
      
    } else if ( strCommand.equals( F("/reboot") ) ) {

      log( F("Command: reboot") );

      Serial.println( F("Command: reboot") );

//      strMessage = F("Read request recognized.");
      iMsgCode = MSG_OP_REBOOT_QUEUED;
//      strMsgText = ""; // nothing to say
      
    } else {

      log( F("Unknown command") );
      
//      Serial.println( F("(command is unknown)") );
      
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
    
    if ( 1==iResponseMessageFormat ) {

      log( F("Sending brief message response") );
      
//      client.println( F("Content-Type: text/plain" ) );
//      client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//      client.println();
//      client.print( F("iMsgCode: " ) );
//      client.println( String( iMsgCode ) );
      
//      sendHTTPHTMLHeader( client );
//      client.print( F("<tt>iMsgCode: " ) );
//      client.println( String( iMsgCode ) );
//      client.print( F("</tt></html>" ) );

      sendHTTPTextHeader( client );
      client.print( F("iMsgCode: " ) );
      client.println( String( iMsgCode ) );
      
    } else if ( 3==iResponseMessageFormat ) {

      log( F("Sending file log contents as response") );
      
      sendHTTPTextHeader( client );

#if defined( DEFINE_DEVICE_MEGA )
//      File fileLogRead = SD.open( FILE_LOG, FILE_READ );
//      if ( fileLogRead ) {
//        while ( fileLogRead.available() ) {
//          const char c = fileLogRead.read();
//          client.print( c );
//        }
//        fileLogRead.close();
//      }


      File fh = SD.open( FILE_LOG );
      if (fh) {
       
        long lSize = fh.size();
        if ( lSize > FILE_LOG_VIEW_SIZE ) {
          long lPos = lSize - FILE_LOG_VIEW_SIZE;
          fh.seek( lPos );

          boolean bScan = fh.available();
          char c;
          while ( bScan ) {
            c = fh.read();
            bScan = ( c!='\n' && fh.available() );
          }
        }
        
        byte clientBuf[64];
        int clientCount = 0;
 
        while ( fh.available() ) {
          clientBuf[clientCount] = fh.read();
          clientCount++;
 
          if(clientCount > 63) {
//            Serial.println("Packet");
            client.write(clientBuf,64);
            clientCount = 0;
          }
        }
     
        if (clientCount > 0) {
          client.write( clientBuf, clientCount );
        }

        fh.close();
      } else {
//        Serial.println("file open failed");
          client.print( F("Failed to open file.") );
      }
      
#else
      client.println( F( "No log file available." ) );
#endif

    } else /*if ( 2==iResponseMessageFormat )*/ {

      log( F("Sending complete response") );

      sendHTTPHTMLHeader( client );

//      client.println( F("Content-Type: text/html" ) );
//
//      client.println( F("Connection: close" ) );  // the connection will be closed after completion of the response
//      client.println();
//
//      client.println( F("<!DOCTYPE HTML>" ) );
//      client.println( F("<html>" ) );
      
      client.println( F("<font face='verdana'><table border='1' cellpadding='4'>" ) );
      
      printSection( client, F("Configuration") );
      printNameValue( client, F("Serial Number"), getSerialNumber() );
      
  //    printNameValue( client, F("MAC Address"), getMACAddress() );
      // print mac address
      client.print( F( "<tr><td colspan='2'>MAC Address</td><td><tt>" ) );
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
      
//      printNameValue( client, F("Last success time"), String( lLastSendTime ) );
      printNameValue( client, F("Last success time"), formatTimeValue( lLastSendTime ) );
  //    client.print( F( "<tr><td colspan='2'> Last success time </td><td><tt>" ) );
  //    printTimeValue( client, lLastSendTime );
  //    client.println( F( "</tt>\n</td></tr>" ) );    
  
      client.print( F( "<tr><td colspan='3' align='center'>Time (in ms)</td></tr>" ) );
      
//      printNameValue( client, F("Running time"), String( millis() ) );
      printNameValue( client, F("Running time"), formatTimeValue( millis() ) );
  //    client.print( F( "<tr><td> Running time </td><td><tt> time </tt></td><td><tt>" ) );
  //    printTimeValue( client, millis() );
  //    client.println( F( "</tt>\n</td></tr>" ) );
      
//      printNameValue( client, F("System time"), F("time"), String( getSystemTime() ) );
      printNameValue( client, F("System time"), F("time"), formatTimeValue( getSystemTime() ) );
  //    client.print( F( "<tr><td colspan='2'> System time </td><td><tt>" ) );
  //    printTimeValue( client, getSystemTime() );
  //    client.println( F( "</tt>\n</td></tr>" ) );    
          
//      printNameValue( client, F("Send interval"), F("interval"), String( iInterval ) );
      printNameValue( client, F("Send interval"), F("interval"), formatTimeValue( iInterval ) );
//      printNameValue( client, F("Scheduled send"), String( lNextSend ) );
      printNameValue( client, F("Scheduled send"), formatTimeValue( lNextSend ) );


#if defined( DEFINE_DEVICE_MEGA )
      printSection( client, F("Storage") );
      
      String strStatus;

//      Sd2Card card;
//      SdVolume volume;
//      
//      if ( card.init( SPI_HALF_SPEED, PIN_SD ) ) {
//        switch (card.type()) {
//          case SD_CARD_TYPE_SD1:
//            strStatus = F("SD1");
//            break;
//          case SD_CARD_TYPE_SD2:
//            strStatus = F("SD2");
//            break;
//          case SD_CARD_TYPE_SDHC:
//            strStatus = F("SDHC");
//            break;
//          default:
//            strStatus = F("Unknown");
//        }        
//        printNameValue( client, F("SD card type"), strStatus );
//        
//        if ( volume.init( card ) ) {
//          
//          uint32_t volsize;
//          volsize = volume.blocksPerCluster() * volume.clusterCount() * 512;
//          printNameValue( client, F("Volume size"), String( volsize ) + " bytes" );
//          volsize = volsize / 1024 / 1024;
//          printNameValue( client, F("Volume size"), String( volsize ) + " MB" );
//          
//        } else {
//          printNameValue( client, F("Volume"), F("Init failed") );
//        }
//
//      } else {
//        printNameValue( client, F("SD card"), F("Init failed") );
//      }

      
      File fileLog = SD.open( FILE_LOG );
      if (fileLog) {
        long lSize = fileLog.size();
        lSize = lSize / 1024;
        strStatus = String( lSize ) + " KB";
        fileLog.close();
      } else {
          strStatus = F("File could not be opened");
      }
      printNameValue( client, F("Log file size"), strStatus );

#endif  
      
      printSection( client, F("Request Details") );
      printNameValue( client, F("strOriginal"), strOriginal );
      printNameValue( client, F("strRequest"), strRequest );
      printNameValue( client, F("strCommand"), strCommand );
      printNameValue( client, F("strParams"), strParams );
      printNameValue( client, F("strName"), strName );
      printNameValue( client, F("strValue"), strValue );
      
      printSection( client, F("Results") );
  //    printNameValue( client, F("strMessage"), strMessage );
      client.print( F( "<tr><td>Message</td><td>" ) );
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
    }
    
//  Serial.println( "Response sent completely." );

    
    
  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );
  return iMsgCode;
}



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
//  Serial.begin(9600);
  Serial.begin(115200);

  pinMode( byteActivityLED, OUTPUT );

#if defined( DEFINE_DEVICE_MEGA )
  if ( !SD.begin( PIN_SD ) ) {
    Serial.print( F( "Failed to initialize SD card." ) );
  } else {
    if ( !SD.exists( SD_DIRS ) ) {
      SD.mkdir( SD_DIRS );
    }
  }
//#else
//  fileLog = 0;
#endif  
  
  log( F("-----------------------------------------------------------") );
  log( F("--> setup()") );

  //TODO remove  
  Serial.println( formatTimeValue( millis() ) );

  dht02.begin();
  dht03.begin();
//  dht04.begin();

  resolveMACAddress();
//  Ethernet.maintain(); // do NOT use DHCP: not enough room for program
  Ethernet.begin( macPlanet, ipPlanet );
//  Ethernet.begin( macPlanet );
//  Ethernet.maintain(); // do NOT use DHCP: not enough room for program
  
  server.begin();
  log( F("Device IP is ") );
  log( formatIP( Ethernet.localIP() ) );
//  log( String( F("Device IP is ") + formatIP( Ethernet.localIP() ) ) );
  
  
  String strHostIP = readConfig( "host_ip", "" );
  if ( strHostIP.length() > 0 ) {
    processRequestSet( "host_ip", strHostIP, false );
  } else if ( Serial ) {
    ipStar = IPAddress( 127,0,0,1 );
  } else {
    sendAtom( SEND_CODE_NODE_INIT );
  }


  log( F("<-- setup()") );
}



long lCounter = 0;

void loop() {
  // put your main code here, to run repeatedly:
  lCounter = lCounter + 1;

  /* check for HTTP requests */
  EthernetClient client = server.available();
  if (client) {
//    Serial.println( F("new client") );
    
    const int iMsgCode = processRequest( client );
    delay(1);
    client.stop();
    
    if ( iMsgCode==MSG_OP_REBOOT_QUEUED ) {
      reboot();
    }
  }
  
  if ( 0==(lCounter % 1000) ) {
//    Ethernet.maintain(); // do NOT use DHCP: not enough room for program
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
  
  
  /* check for mandatory reboot */
  if ( millis() >= MANDATORY_UPTIME_REBOOT ) {
    reboot();
  }
  
  

  // turn off activity LED
  digitalWrite( byteActivityLED, LOW );
}


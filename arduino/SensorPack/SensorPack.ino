/*
 Original code derived from
 http://www.arduino.cc/en/Tutorial/SerialEvent
 */

// see http://forum.arduino.cc/index.php?topic=45104.0
// reads the Serial of the Arduino from the
// first 6 bytes of the EEPROM

/* Set up globals */

#include <EEPROM.h>
char sID[7];
String strVersion = "20160827_1053";

String inputString = "";         // a string to hold incoming data
boolean stringComplete = false;  // whether the string is complete

int iRemainingDelay;
int iIdleDelay = 500;


/*
 * Main setup() method
 */
void setup() {
  // initialize serial:
//  Serial.begin(9600);
  Serial.begin( 115200 );
  // reserve 200 bytes for the inputString:
  inputString.reserve(200);
  
  for (int i=0; i<6; i++) {
    sID[i] = EEPROM.read(i);
  }

  pinMode( 1, INPUT );
  pinMode( 2, INPUT );
  
  Serial.println( F("{ LOAD=SensorPack_01 }") );
}

/*
 * Main loop() method
 */
void loop() {
  
  if ( iRemainingDelay <= 0 ) {
    
    Serial.println( "{" );
  
    // Show the temperature in degrees Celsius.
    float fInternalTempC = GetTemp();
    Serial.print( " Temp_Internal(C)=" );
    Serial.println( fInternalTempC,1 );
    float fInternalTempF = ( fInternalTempC * 9.0 / 5.0 ) + 32;
    Serial.print( " Temp_Internal(F)=" );
    Serial.println( fInternalTempF,1 );

    if ( true ) { // show all analog input values    
      int iA0 = analogRead( A0 );
      Serial.print( " A0=" );
      Serial.println( iA0,1 );
      int iA1 = analogRead( A1 );
      Serial.print( " A1=" );
      Serial.println( iA1,1 );
      int iA2 = analogRead( A2 );
      Serial.print( " A2=" );
      Serial.println( iA2,1 );
      int iA3 = analogRead( A3 );
      Serial.print( " A3=" );
      Serial.println( iA3,1 );
      
      int iD2 = digitalRead( 2 );
      Serial.print( " D2=" );
      Serial.println( iD2,1 );
      int iD3 = digitalRead( 3 );
      Serial.print( " D3=" );
      Serial.println( iD3,1 );
    }
    
    
    if ( '1'==sID[3] ) {
      // Show the sensor temperature
      int iSensorVal = analogRead( A0 );
      float fSensorVolts = ( iSensorVal / 1024.0 ) * 5;
      float fSensorTempC = ( fSensorVolts - 0.5) * 100;
      Serial.print( " Temp_Sensor(C)=" );
      Serial.println( fSensorTempC,1 );
      float fSensorTempF = ( fSensorTempC * 9.0 / 5.0 ) + 32;
      Serial.print( " Temp_Sensor(F)=" );
      Serial.println( fSensorTempF,1 );
    }
  
    Serial.print( " Arduino.Serial=" );
    Serial.println( sID );
    Serial.print( " Version=" );
    Serial.println( strVersion );
  
    Serial.println( "}" );
    
    iRemainingDelay = iIdleDelay;
  } else {
    iRemainingDelay = iRemainingDelay - 1;
  }
  
  
  // print the string when a newline arrives:
  if (stringComplete) {
    int length = inputString.length();
    Serial.print( "Input String (" );
    Serial.print( length );
    Serial.print( " chars):" );
//    Serial.print("Input String (" + length + " chars):" + inputString);
    Serial.println( inputString );
    // clear the string:
    inputString = "";
    stringComplete = false;
  }
  
  delay( 10 );
}

/*
  SerialEvent occurs whenever a new data comes in the
 hardware serial RX.  This routine is run between each
 time loop() runs, so using delay inside loop can delay
 response.  Multiple bytes of data may be available.
 */
void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      stringComplete = true;
      iRemainingDelay = 0;
    }
  }
}


/*
//source:http://playground.arduino.cc/Main/InternalTemperatureSensor
*/
double GetTemp(void)
{
  unsigned int wADC;
  double t;

  // The internal temperature has to be used
  // with the internal reference of 1.1V.
  // Channel 8 can not be selected with
  // the analogRead function yet.

  // Set the internal reference and mux.
  ADMUX = (_BV(REFS1) | _BV(REFS0) | _BV(MUX3));
  ADCSRA |= _BV(ADEN);  // enable the ADC

  delay(20);            // wait for voltages to become stable.

  ADCSRA |= _BV(ADSC);  // Start the ADC

  // Detect end-of-conversion
  while (bit_is_set(ADCSRA,ADSC));

  // Reading register "ADCW" takes care of how to read ADCL and ADCH.
  wADC = ADCW;

  // The offset of 324.31 could be wrong. It is just an indication.
  t = (wADC - 324.31 ) / 1.22;

  // The returned temperature is in degrees Celsius.
  return (t);
}


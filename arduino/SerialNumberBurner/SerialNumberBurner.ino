// WRITES ARDUINO SERIAL TO EEPROM
//
// do this only once on an Arduino,
// write the Serial of the Arduino in the
// first 6 bytes of the EEPROM

#include <EEPROM.h>
char sIDRead[7];
char sIDWrite[7] = "0103X3";
String strMessage;

//             VVYYNN
// Version (01=arduino uno, this version scheme)
// Device number acquired
// Role (Gx)


void setup()
{
  Serial.begin(9600);
  for (int i=0; i<6; i++) {
    sIDRead[i] = EEPROM.read(i);
  }
  
  if ( '0'==sIDRead[0] && '1'==sIDRead[1] ) {
    strMessage = "Device already has a Serial Number.";
  } else {
    strMessage = "New Serial Number written.";
//    for (int i=0; i<6; i++) {
//      EEPROM.write(i,sIDWrite[i]);
//    }
  }

}

void loop() {
  Serial.println( strMessage );
  Serial.print( "Existing value: " );
  Serial.println( sIDRead );
  Serial.print( "Target value: " );
  Serial.println( sIDWrite );
  delay(2000);
}


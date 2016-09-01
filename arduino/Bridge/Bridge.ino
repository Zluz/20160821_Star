/*
  Arduino Yún Bridge example

 This example for the Arduino Yún shows how to use the
 Bridge library to access the digital and analog pins
 on the board through REST calls. It demonstrates how
 you can create your own API when using REST style
 calls through the browser.

 Possible commands created in this shetch:

 * "/arduino/digital/13"     -> digitalRead(13)
 * "/arduino/digital/13/1"   -> digitalWrite(13, HIGH)
 * "/arduino/analog/2/123"   -> analogWrite(2, 123)
 * "/arduino/analog/2"       -> analogRead(2)
 * "/arduino/mode/13/input"  -> pinMode(13, INPUT)
 * "/arduino/mode/13/output" -> pinMode(13, OUTPUT)

 This example code is part of the public domain

 http://arduino.cc/en/Tutorial/Bridge

 */

#include <Bridge.h>
#include <YunServer.h>
#include <YunClient.h>

#include <Process.h>


//#include <SPI.h>
//#include <Ethernet.h>
#include <HttpClient.h>



// Listen to the default port 5555, the Yún webserver
// will forward there all the HTTP requests you send
YunServer server;










//byte mac[] = { 0xA8, 0x40, 0x41, 0x15, 0xCC, 0x33 };
//IPAddress ipStar( 192,168,1,4 );  // (remote server)
//IPAddress ipPlanet( 192,168,1,9 ); // (this client)
//EthernetClient ec;











void setup() {
  // Bridge startup
  pinMode(13, OUTPUT);
  digitalWrite(13, LOW);
  Bridge.begin();
  digitalWrite(13, HIGH);

  // Listen for incoming connection only from localhost
  // (no one from the external network could connect)
  server.listenOnLocalhost();
  server.begin();
  
  
  
  
  
    Process p;
//  p.runShellCommand("/usr/bin/pretty-wifi-info.lua | grep Signal");
//  p.runShellCommand( "/usr/bin/pretty-wifi-info.lua > /tmp/pretty-wifi-info.out" );
  p.runShellCommand( "ps > /tmp/ps.out" );

  // do nothing until the process finishes, so you get the whole output:
  while (p.running());





//  delay(100);
//    Ethernet.begin( mac, ipPlanet );
//  delay(100);
//  ec.connect( ipStar, 80 );
//  delay(100);
//    ec.println("GET /atom HTTP/1.1");
//  delay(10);
//    ec.println("Host: 192.168.1.4");
//  delay(10);
//    ec.println("Connection: close");
//  delay(10);
//    ec.println();

}

void loop() {
  // Get clients coming from server
  YunClient client = server.accept();

  // There is a new client?
  if (client) {
    // Process request
    process(client);

    // Close connection and free resources.
    client.stop();
  }





//  if (ec.available()) {
//    char c = ec.read();
////    Serial.print(c);
//  }
//
//  // if the server's disconnected, stop the client:
//  if (!ec.connected()) {
//    ec.stop();
//  }




  delay(50); // Poll every 50ms
}

void process(YunClient client) {
  // read the command
  String command = client.readStringUntil('/');

  // is "digital" command?
  if (command == "digital") {
    digitalCommand(client);
  }

  // is "analog" command?
  if (command == "analog") {
    analogCommand(client);
  }

  // is "read" command?
  if (command == "read") {
    readCommand(client);
  }

  // is "mode" command?
  if (command == "mode") {
    modeCommand(client);
  }
}

void digitalCommand(YunClient client) {
  int pin, value;

  // Read pin number
  pin = client.parseInt();

  // If the next character is a '/' it means we have an URL
  // with a value like: "/digital/13/1"
  if (client.read() == '/') {
    value = client.parseInt();
    digitalWrite(pin, value);
  }
  else {
    value = digitalRead(pin);
  }

  // Send feedback to client
  client.print(F("Pin D"));
  client.print(pin);
  client.print(F(" set to "));
  client.println(value);

  // Update datastore key with the current pin value
  String key = "D";
  key += pin;
  Bridge.put(key, String(value));
}

void readCommand(YunClient client) {
  for ( int pin=0; pin<6; pin++ ) {
    // Read analog pin
    int value = analogRead(pin);

    // Send feedback to client
    client.print(F("Pin A"));
    client.print(pin);
    client.print(F(" reads analog "));
    client.print(value);
//    client.print(F(", "));
    client.println();
    
    // Update datastore key with the current pin value
//    String key = "A";
//    key += pin;
//    Bridge.put(key, String(value));
  }

  for ( int pin=2; pin<15; pin++ ) {
  // If the next character is a '/' it means we have an URL
  // with a value like: "/digital/13/1"
    int value = digitalRead(pin);

    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" set to "));
    client.println(value);
  }
  
  
  
  
  // Initialize the client library
  HttpClient hc;
  String content;
  content = "<start>";

  // Make a HTTP request:
//  hc.get("http://www.arduino.cc/asciilogo.txt");
  hc.get("http://192.168.1.4/atom?name01=value01&name02=value02&name03=value03");
  

  // if there are incoming bytes available
  // from the server, read them and print them:
  while (hc.available()) {
    char c = hc.read();
//    SerialUSB.print(c);
    content = content + c;
  }
  content = content + "<end>";
//  SerialUSB.flush();
  
  
  client.println();
  client.println( "response from client request:" );
  client.println();
  client.println( content );
  client.println();
  
}

void analogCommand(YunClient client) {
  int pin, value;

  // Read pin number
  pin = client.parseInt();

  // If the next character is a '/' it means we have an URL
  // with a value like: "/analog/5/120"
  if (client.read() == '/') {
    // Read value and execute command
    value = client.parseInt();
    analogWrite(pin, value);

    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" set to analog "));
    client.println(value);

    // Update datastore key with the current pin value
    String key = "D";
    key += pin;
    Bridge.put(key, String(value));
  }
  else {
    // Read analog pin
    value = analogRead(pin);

    // Send feedback to client
    client.print(F("Pin A"));
    client.print(pin);
    client.print(F(" reads analog "));
    client.println(value);

    // Update datastore key with the current pin value
    String key = "A";
    key += pin;
    Bridge.put(key, String(value));
  }
}

void modeCommand(YunClient client) {
  int pin;

  // Read pin number
  pin = client.parseInt();

  // If the next character is not a '/' we have a malformed URL
  if (client.read() != '/') {
    client.println(F("error"));
    return;
  }

  String mode = client.readStringUntil('\r');

  if (mode == "input") {
    pinMode(pin, INPUT);
    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" configured as INPUT!"));
    return;
  }

  if (mode == "output") {
    pinMode(pin, OUTPUT);
    // Send feedback to client
    client.print(F("Pin D"));
    client.print(pin);
    client.print(F(" configured as OUTPUT!"));
    return;
  }

  client.print(F("error: invalid mode "));
  client.print(mode);
}


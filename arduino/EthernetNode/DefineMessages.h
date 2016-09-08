
// #include "Arduino.h"

// replace  strMessge
//   with   iMsgCode, strMsgText

#define MSG_SCHEDULE_DISABLED			1001

#define MSG_STAR_HOSTNAME_SET			1002
#define MSG_STAR_IP_SET					1003
#define MSG_INTERVAL_SET				1004
#define MSG_INTERVAL_INVALID_VALUE		1005
#define MSG_TIME_OFFSET_SET				1006
#define MSG_TIME_OFFSET_INVALID_VALUE	1007

#define MSG_SET_PIN_MODE_SUCCESS			1008	// text is pin
#define MSG_SET_PIN_MODE_INVALID_MODE		1009
#define MSG_SET_PIN_MODE_INVALID_PIN		1010
#define MSG_SET_PIN_MODE_SUCCESS			1011

#define MSG_WRITE_DIGITAL_SUCCESS			1012	// text is pin
#define MSG_WRITE_DIGITAL_INVALID_VALUE		1013

#define MSG_WRITE_PWM_SUCCESS				1014	// text is pin
#define MSG_WRITE_PWM_INVALID_VALUE			1015

#define MSG_WRITE_PIN_INVALID_MODE			1016
#define MSG_WRITE_PIN_INVALID_PIN			1017

#define MSG_INVALID_VARIABLE			1101
// #define MSG_OP_SEND					1102
// #define MSG_OP_SEND_FAST_SUCCESS		1104

#define MSG_SEND_FAILED_TO_CONNECT		1105
#define MSG_SEND_FAILED_NO_STAR_HOST	1108
#define MSG_SEND_FAILED_NO_CLIENT		1106
#define MSG_SEND_SUCCESS				1107

#define MSG_OP_READ_SUCCESS				1103

#define MSG_OP_UNKNOWN					1100
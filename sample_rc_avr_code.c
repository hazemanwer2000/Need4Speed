
#define F_CPU 16000000UL

#include <avr/io.h>
#include <avr/interrupt.h>
#include <string.h>
#include <stdlib.h>

#define BIT(N) (0b0000001 << N)

	/* MACRO: Motor functions and constants. */

#define LEFT_SPEED(_X)			OCR2A = _X 
#define RIGHT_SPEED(_X)			OCR2B = _X
#define SPEED(_X)				LEFT_SPEED(_X); RIGHT_SPEED(_X)
#define ADJUST_SPEED(X)			((int) ((X / 100.0f) * 255))
#define LOW						190
#define HIGH					225

#define FWD_ON					(0b10000010)
#define FWD_OFF					(0b00101000)
#define MOVE_FORWARD()			PORTA = ((PORTA | FWD_ON) & ~FWD_OFF)
#define MOVE_BACKWARD()			PORTA = ((PORTA | FWD_OFF) & ~FWD_ON)

#define ROT_L_ON				(0b10001000)
#define ROT_L_OFF				(0b00100010)
#define ROTATE_LEFT()			PORTA = ((PORTA | ROT_L_ON) & ~ROT_L_OFF)
#define ROTATE_RIGHT()			PORTA = ((PORTA | ROT_L_OFF) & ~ROT_L_ON)

	/* MACRO: Sensory readings of line-tracking IR's. */

#define RIGHT_LINE				(!(PINL & BIT(3)))
#define LEFT_LINE				(!(PINL & BIT(2)))

	/* MACRO: Sensory readings of obstacle-avoidance IR's. */

#define RIGHT_OBS				(!(PINL & BIT(5)))
#define LEFT_OBS				(!(PINL & BIT(4)))

	/* MACRO: Buffer size, for commands to be stored (received over UART). */

#define BUFFER_SIZE 16

	/* MACRO: Timer5, ultrasonic-related. */

#define MICRO_SECONDS_2				32
#define MICRO_SECONDS_10			160

	/* MACRO: Distance threshold (obstacle avoidance). */

#define DISTANCE_THRESHOLD			10.0

	/* MACRO: Trigger pin ON/OFF. */

#define TRIG_ON()				PORTL |= BIT(0)
#define TRIG_OFF()				PORTL &= ~BIT(0)

	/* MACRO: Debug LED pin ON/OFF/TOGGLE. */

#define DEBUG_SETUP()			DDRB |= BIT(7);
#define DEBUG_ON()				PORTB |= BIT(7)
#define DEBUG_OFF()				PORTB &= ~BIT(7)
#define DEBUG_TOGGLE()			PORTB ^= BIT(7)

	/* GLOBAL VAR(S): Buffer-related, for commands to be stored. */

static char buffer[BUFFER_SIZE];
static unsigned char i;

	/* GLOBAL VAR(S): Stores current mode of operation. */

static char mode = '0';

	/* GLOBAL VAR(S): Distance reading storage, and an overflow counter. */

static double distance = DISTANCE_THRESHOLD + 1.0;
static long long int ovf_count;

ISR(USART1_RX_vect) {
	buffer[i++] = UDR1;
}

ISR (TIMER5_COMPA_vect) {
	if (OCR5A == MICRO_SECONDS_2) {
		TRIG_ON();									/* Trigger pin turned on. */
		TCNT5 = 0;									/* Resetting timer. */
		OCR5A = MICRO_SECONDS_10;
														/* NOTE: Must be done early, just-after issuing trigger signal. */
		TCCR5B |= BIT(ICES5);						/* On-rising edge input-capture (must be done early, while triggering). */
		TIFR5 |= BIT(ICF5);							/* Input-capture flag reset (not always necessary). */
	} else {
		TRIG_OFF();														/* Trigger pin turned off. */
		TIMSK5 = (TIMSK5 & ~BIT(OCIE5A)) | BIT(ICIE5);					/* Disabling compare-match interrupt, enabling input-capture interrupt. */
	}
}

ISR (TIMER5_OVF_vect) {
	ovf_count++;
}

ISR (TIMER5_CAPT_vect) {
	if (TCCR5B & BIT(ICES5)) {										/* CONDITION: Rising-edge. */
		TCCR5B &= ~BIT(ICES5);											/* On-falling edge input-capture. */
		TCNT5 = 0;														/* Resetting timer. */
		ovf_count = 0;													/* Resetting overflow counter. */
		TIFR5 |= BIT(TOV5);												/* Overflow flag reset. */
		TIMSK5 |= BIT(TOIE5);											/* Enabling overflow interrupt. */
	} else {														/* CONDITION: Falling-edge. */
		distance = ((ovf_count << 16) + ICR5) / 932.945;						/* Updating distance. */
		TCNT5 = 0;																/* Resetting timer. */
		OCR5A = MICRO_SECONDS_2;
		TIFR5 |= BIT(OCF5A);													/* Output-compare flag reset. */
		TIMSK5 = (TIMSK5 & ~(BIT(ICIE5) | BIT(TOIE5))) | BIT(OCIE5A);			/* Disabling input-capture and output-compare interrupts, enabling overflow interrupt. */
	}
}

void set_speed(char *buffer) {
	int i, j;
	i = j = 6;

	while(buffer[++j] != ' ');
	buffer[j++] = '\0';

	i = atoi(buffer + i);
	j = atoi(buffer + j);

	LEFT_SPEED(ADJUST_SPEED(i));
	RIGHT_SPEED(ADJUST_SPEED(j));
}

int main() {		
	
		/* SETUP: GPIO pins, as output. */
	
	DDRA |= 0b10101010;         /* Pins A (1, 3, 5, 7), for motor direction control. */
	
		/* SETUP: PWM using Timer2, for DC motor speed control. */
	
	DDRB |= BIT(4); DDRH |= BIT(6);								/* Setting pins as output. */
	TCCR2A |= BIT(COM2A1) | BIT(COM2B1) | BIT(WGM20);			/* Fast-PWM mode, non-inverting mode. */
	TCCR2B |= BIT(CS20);										/* Clocked at CPU speed, no prescaler. */
	
		/* SETUP: UART(1) for HC-05 (Bluetooth) communication, asynchronous, no-parity, 1-stop-bit. */
	
	UCSR1B |= BIT(RXEN1) | BIT(RXCIE1);							/* Enable receive (and its interrupt). */
	UBRR1L |= 103;												/* 9600 bps (% error = 0.16155). */

		/* SETUP: Timer5, for ultrasonic readings, used to trigger (compare-match) and read distance (on echo signal, input-capture). */
	
	DDRL |= BIT(0);												/* Trigger pin, set as output. */
	OCR0A = MICRO_SECONDS_2;									/* Initially, low-trigger signal for 2 micro-seconds. */
	TIMSK5 |= BIT(OCIE5A);										/* Enabling output-compare interrupt. */
	TCCR5B |= BIT(CS50);										/* Enabling clock, no-prescaler. */
	
	sei();
	
	DEBUG_SETUP();
	DEBUG_OFF();
	
	while (1) {
		
			/* DO: Execute a command, sent over bluetooth, and received over UART. */
		
		if (buffer[i-1] == '\n') {
			buffer[i-2] = '\0';
			if (strncmp(buffer, "mode", 4) == 0) {
				mode = buffer[5];
				MOVE_FORWARD();
				if (mode == 'a' || mode == '0') {
					SPEED(0);
				} else if (mode == 'c' || mode == 'b') {
					SPEED(LOW);
				}
			} else if (mode == 'a') {
				if (strcmp(buffer, "forward") == 0) {
					MOVE_FORWARD();
				} else if (strcmp(buffer, "backward") == 0) {
					MOVE_BACKWARD();
				} else if (strcmp(buffer, "rotate left") == 0) {
					ROTATE_LEFT();
				} else if (strcmp(buffer, "rotate right") == 0) {
					ROTATE_RIGHT();
				} else if (strcmp(buffer, "slow") == 0) {
					SPEED(LOW);
				} else if (strcmp(buffer, "fast") == 0) {
					SPEED(HIGH);
				} else if (strcmp(buffer, "stop") == 0) {
					SPEED(0);
				} else if (strncmp(buffer, "speed", 5) == 0) {
					set_speed(buffer);
				}
			}
			i = 0;
		}
		
			/* DO: Perform logic related to the current mode. */
		
		if (mode == 'b') {
			if (distance < DISTANCE_THRESHOLD || LEFT_OBS) {
				ROTATE_RIGHT();
			} else if (RIGHT_OBS) {
				ROTATE_LEFT();
			} else {
				MOVE_FORWARD();
			}
		} else if (mode == 'c') {
			if (RIGHT_LINE && LEFT_LINE) {
				MOVE_FORWARD();
			} else if (LEFT_LINE) {
				ROTATE_RIGHT();
			} else {
				ROTATE_LEFT();
			}
		}
		
			/* DO: DEBUG (Ultra-sonic distance, sent over to built-in LED). */
		if (distance < DISTANCE_THRESHOLD) {
			DEBUG_ON();
		} else {
			DEBUG_OFF();
		}
	};
	
	return 0;
}


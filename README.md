# Need4Speed
**Need4Speed** is an open-source JavaFX (Java 8) solution, controlling any RC vehicle with a serial interface.

The GUI consists of three main components:
* Speed limiter (Slider): Limits the maximum speed of the vehicle.
* Lock (Button): Limits the maximum speed of the vehicle (ON/OFF).
* Gauge (From the [*Medusa*](https://github.com/HanSolo/medusa) library): Displays the current speed of the vehicle. 

***Notes:***
* The arrow keys control the direction of motion of the vehicle, and are *non-latching*.
* The vehicle constantly accelerates, or decelerates, to its target speed, as long as a key is pressed, and the speed limit has not been reached yet.
* Speed increases gradually, yet velocity (speed and direction) may change in zero-time. For example, when moving forward, then backwards.

<a href="https://www.youtube.com/watch?v=sc1wYeOnlsE" target="_blank"><img src="" alt="Click to play a demonstrative video." width="240" height="180" border="10" /></a>

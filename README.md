Virtual Thermostat Device with Device Type Hander to create a thermostat device (Works With Google Home and Alexa)

Simply allows the creation of a new device thats shows as a thermostat, using the temperature from selected temperature sensor(s) and an on/off outlet(s) for heating. 

Allows a contact sensor(s) to disable the thermostat when open (no heating when window/door is open) - optional

Allows a presence sensor(s) to detect absence and either turn off the thermostat or change the temperature - optional

Allows a motion sensor(s) to detect absence and either turn off the thermostat or change the temperature - optional

Supports minimum/frost temperature setting, will not allow the temperature to drop below this point (even if thermostat is turned off)
Supports time zones (4 for weekdays and 4 for weekend days)
Supports configurable zone names (name is given through to device handler, the zone name changes to presence/contact state)
Supports "boost" with configurable on time and amount to boost temperature

Displays a value (Device attribute) that shows either the name of the current zone (configurable), the boost mode, or the presence/motion/contact status


The basis of this code is from Eliot Stocker's Virtual Thermostat - So a big thumbs up to him :)

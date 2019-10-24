definition(
	name: "Virtual Thermostat With Device and Schedule",
	namespace: "jayuk",
	author: "JayUK",
	description: "Control a heater in conjunction with any temperature sensor, like a SmartSense Multi.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo.png",
	parent: "jayuk:Virtual Thermostat Manager",
)

preferences {
	section("Choose a temperature sensor(s)... (If multiple sensors are selected, the average value will be used)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true
	}
	section("Select the heater outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Only heat when contact isnt open (optional, leave blank to not require contact sensor)..."){
		input "motion", "capability.contactSensor", title: "Contact", required: false
	}
 	section("Only heat when person is present (optional, leave blank to not require presence sensor)..."){
		input "presence", "capability.presenceSensor", title: "Presence", required: false
	}
	section("Never go below this temperature: (optional)"){
		input "emergencySetpoint", "decimal", title: "Emergency Temp", required: false
	}
	section("Temperature Threshold (Don't allow heating to go above or bellow this amount from set temperature)") {
		input "threshold", "decimal", "title": "Temperature Threshold", required: false, defaultValue: 1.0
	}
 	section("Monday to Friday Schedule") {
		input "zone1", "time", title: "Zone 1 start time", required: true
		input "zone1Temperature", "decimal", title: "Zone 1 temperature", defaultValue: 15, required: true
        	input "zone1Name", "string", title: "Zone 1 name", defaultValue: "Zone 1", required: true
		input "zone2", "time", title: "Zone 2 start time", required: true
		input "zone2Temperature", "decimal", title: "Zone 2 temperature", defaultValue: 15, required: true
        	input "zone2Name", "string", title: "Zone 2 name", defaultValue: "Zone 2", required: true
		input "zone3", "time", title: "Zone 3 start time", required: true
		input "zone3Temperature", "decimal", title: "Zone 3 temperature", defaultValue: 15, required: true
        	input "zone3Name", "string", title: "Zone 3 name", defaultValue: "Zone 3", required: true
		input "zone4", "time", title: "Zone 4 start time", required: true
		input "zone4Temperature", "decimal", title: "Zone 4 temperature", defaultValue: 15, required: true
        	input "zone4Name", "string", title: "Zone 4 name", defaultValue: "Zone 4", required: true
	}
	section("Saturday and Sunday Schedule") {
		input "zone1Weekend", "time", title: "Zone 1 start time", required: true
		input "zone1WeekendTemperature", "decimal", title: "Zone 1 temperature", defaultValue: 15, required: true
        	input "zone1WeekendName", "string", title: "Zone 1 name", defaultValue: "Zone 1 Weekend", required: true
		input "zone2Weekend", "time", title: "Zone 2 start time", required: true
		input "zone2WeekendTemperature", "decimal", title: "Zone 2 temperature", defaultValue: 15, required: true
        	input "zone2WeekendName", "string", title: "Zone 2 name", defaultValue: "Zone 2 Weekend", required: true
		input "zone3Weekend", "time", title: "Zone 3 start time", required: true
		input "zone3WeekendTemperature", "decimal", title: "Zone 3 temperature", defaultValue: 15, required: true
        	input "zone3WeekendName", "string", title: "Zone 3 name", defaultValue: "Zone 3 Weekend", required: true
		input "zone4Weekend", "time", title: "Zone 4 start time", required: true
		input "zone4WeekendTemperature", "decimal", title: "Zone 4 temperature", defaultValue: 15, required: true
        	input "zone4WeekendName", "string", title: "Zone 4 name", defaultValue: "Zone 4 Weekend", required: true
	}
}

def installed()
{
	log.debug "Running installed"
	state.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
	state.lastTemp = null
	state.contact = true
	state.presence = true
	state.previousZoneName = null
	state.todayTime = 0
	state.yesterdayTime = 0
	state.date = new Date().format("dd-MM-yy")
	state.lastOn = 0

	/* Flags to only allow the temperature to be set once by a zone change (to allow a user to manually override the temp until next Zone) */
	state.zone1Set = false
	state.zone2Set = false
	state.zone3Set = false
	state.zone4Set = false
	state.zone1WeekendSet = false
	state.zone2WeekendSet = false
	state.zone3WeekendSet = false
	state.zone4WeekendSet = false
}

def createDevice() {
	def thermostat
	def label = app.getLabel()

	log.debug "Create device with id: pmvt$state.deviceID, named: $label"
	
	try {
		thermostat = addChildDevice("jayuk", "Virtual Thermostat Device", "pmvt" + state.deviceID, null, [label: label, name: label, completedSetup: true])
	} catch(e) {
		log.error("Caught exception", e)
	}
	
	return thermostat
}

def getThermostat() {
	def child = getChildDevices().find {
	d -> d.deviceNetworkId.startsWith("pmvt" + state.deviceID)
	}
	return child
}

def uninstalled() {
    	deleteChildDevice("pmvt" + state.deviceID)
}

def updated() {
	log.debug "Running updated: $app.label"
	unsubscribe()
	unschedule()

	def thermostat = getThermostat()

	if(thermostat == null) {
		log.debug "Creating thermostat"
		thermostat = createDevice()
	}

	state.lastTemp = null

	if(state.todayTime == null) state.todayTime = 0
	if(state.yesterdayTime == null) state.yesterdayTime = 0
	if(state.date == null) state.date = new Date().format("dd-MM-yy")
	if(state.lastOn == null) state.lastOn = 0

	subscribe(sensors, "temperature", temperatureHandler)

	if (motion) {
		log.debug "Contact sensors selected"
		subscribe(motion, "contact", motionHandler)
	} else {
		log.debug "No contact sensor selected"
	}

	if (presence) {
		log.debug "Presence sensor selected"
		subscribe(presence, "presence", presenceHandler)
	} else {
		log.debug "No presence sensor selected"
	}

	subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
	subscribe(thermostat, "thermostatMode", thermostatModeHandler)
	thermostat.clearSensorData()
	thermostat.setVirtualTemperature(getAverageTemperature())
	thermostat.setTemperatureScale(parent.getTempScale())
	runEvery1Hour(updateTimings)

	setRequiredZone()
	}

def getAverageTemperature() {
	def total = 0;
	def count = 0;

	for(sensor in sensors) {
		total += sensor.currentValue("temperature")
		thermostat.setIndividualTemperature(sensor.currentValue("temperature"), count, sensor.label)
		count++
	}
	return total / count
}

def temperatureHandler(evt) {
	def thermostat = getThermostat()
	thermostat.setVirtualTemperature(getAverageTemperature())

	if ((state.contact && state.presence) || emergencySetpoint) {
		evaluate(evt.doubleValue, thermostat.currentValue("thermostatSetpoint"))
		state.lastTemp = evt.doubleValue
	} else {
		heatingOff()
	}
}

def motionHandler(evt) {
    	def thermostat = getThermostat()
	if (evt.value == "Contact sensor closed") {
    	state.contact = true
        log.debug "Setting zone name back to the previous zone name (temporary until we check what Zone we should be in: $state.previousZoneName"
        thermostat.setZoneName(state.previousZoneName)
        
        // Check if we are still in the same zone as previously
        setRequiredZone()
        
        def thisTemp = getAverageTemperature()
	if (thisTemp != null) {
		evaluate(thisTemp, thermostat.currentValue("thermostatSetpoint"))
		state.lastTemp = thisTemp
	}
	} else if (evt.value == "open") {
        	log.debug "Contact sensor open - Turn heating off"
		state.contact = false
		state.previousZoneName = thermostat.currentvalue("zoneName")
		thermostat.setZoneName("Contact: Open")
		heatingOff()
	}
}

def presenceHandler(evt) {
    	def thermostat = getThermostat()
    
    	log.debug "Presence value: $evt.value"
    
	if (evt.value == "present") {
		state.presence = true
		log.debug "Setting zone name back to the previous zone name (temporary until we check what Zone we should be in: $state.previousZoneName"
		thermostat.setZoneName(state.previousZoneName)

		// Check if we are still in the same zone as previously
		setRequiredZone()

		def thisTemp = getAverageTemperature()
		if (thisTemp != null) {
			evaluate(thisTemp, thermostat.currentValue("thermostatSetpoint"))
			state.lastTemp = thisTemp
		}
	} else if (evt.value == "not present") {
		log.debug "Presence away - Turn heating off"
		state.presence = false
		state.previousZoneName = thermostat.currentValue("zoneName")
		thermostat.setZoneName("Presence: Away")
		heatingOff()
	}
}

def thermostatTemperatureHandler(evt) {
	def temperature = evt.doubleValue
    	//setpoint = temperature
	log.debug "Desired Temperature set to: $temperature $state.contact"
    
    	def thisTemp = getAverageTemperature()
	if (state.contact && state.presence) {
		evaluate(thisTemp, temperature)
	}
	else {
		heatingOff()
	}
}

def thermostatModeHandler(evt) {
	def mode = evt.value
	log.debug "Mode Changed to: $mode"
    	def thermostat = getThermostat()
    
    	def thisTemp = getAverageTemperature()
	if (state.contact && state.presence) {
		evaluate(thisTemp, thermostat.currentValue("thermostatSetpoint"))
	}
	else {
		heatingOff(mode == 'heat' ? false : true)
	}
}

private evaluate(currentTemp, desiredTemp) {
	log.debug "EVALUATE($currentTemp, $desiredTemp)"
	   
	if ( (desiredTemp - currentTemp >= threshold)) {
		heatingOn()
	} else if ( (currentTemp - desiredTemp >= threshold)) {
		heatingOff()
	} else if(state.current == "on") {
        	updateTimings()
    	}
}

def heatingOn() {
	if(thermostat.currentValue('thermostatMode') == 'heat' || force) {
		log.debug "Heating on Now"
		outletsOn()
		thermostat.setHeatingStatus(true)
	} else {
		heatingOff(true)
	}
}

def heatingOff(heatingOff) {
	def thisTemp = getAverageTemperature()
    
	if (thisTemp <= emergencySetpoint) {
		log.debug "Heating in Emergency Mode Now"
		ouletsOn()
		thermostat.setEmergencyMode(true)
	} else {
		log.debug "Heating off Now"
		outletsOff()
		if(heatingOff) {
			thermostat.setHeatingOff(true)
		} else {
			thermostat.setHeatingStatus(false)
		}
	}
}

def updateTempScale() {
	thermostat.setTemperatureScale(parent.getTempScale())
}

def updateTimings() {
    	def date = new Date().format("dd-MM-yy")
    
	if(state.current == "on") {
		int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
		state.lastOn = Math.round(new Date().getTime() / 1000)
	}

	if(state.date != date) {
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}

	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}

def outletsOn() {
	outlets.on()
	def date = new Date().format("dd-MM-yy")
	
	if(state.current == "on") {
		int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
	}

	if(state.date != date) {
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}
	
	state.lastOn = Math.round(new Date().getTime() / 1000)
	state.current = "on"
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}

def outletsOff() {
	outlets.off()
	def date = new Date().format("dd-MM-yy")

	if(state.current == "on") {
		int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
	}
	
	if(state.date != date) {
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}
	
	state.current = "off"
	state.lastOn = 0;
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}

def setRequiredZone() {
    
    /* Only preform the main body of this procedure if we aren't away or a window/door is open.
    Irrespective of the above, we will still reschedule this process to run in 60 seconds */ 
    if (state.contact && state.presence) {
        def calendar = Calendar.getInstance()
        calendar.setTimeZone(location.timeZone)
        def today = calendar.get(Calendar.DAY_OF_WEEK)
        def timeNow = now()
        def midnightToday = timeToday("2000-01-01T23:59:59.999-0000", location.timeZone)

        // This section is where the time/temperature schedule is set
        switch (today) {
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
                state.zone1Set = true
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone1Name,zone1Temperature)            
            }

            // Are we between 2nd time and 3rd time        	
            else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
                state.zone1Set = false
                state.zone2Set = true
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone2Name,zone2Temperature)            
            }

            // Are we between 3rd time and 4th time	
            else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = true
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone3Name,zone3Temperature)            
            }

            // Are we between 4th time and midnight, schedule next day				
            else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone4Name,zone4Temperature)            
            }

            // Are we between midnight yesterday and 1st time, schedule today
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone4Name,zone4Temperature)            
            }
            break

            case Calendar.FRIDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
                state.zone1Set = true
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone1Name,zone1Temperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
                state.zone1Set = false
                state.zone2Set = true
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone2Name,zone2Temperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = true
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone3Name,zone3Temperature)            
            }

            // Are we between 4th time Friday and midnight, we schedule Saturday
            else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone4Name,zone4Temperature)            
            }

            // Are we between midnight Friday and 1st time on Saturday, we schedule Saturday
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4Set) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone4Name,zone4Temperature)            
            }
            break

            case Calendar.SATURDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = true
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone1WeekendName,zone1WeekendTemperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = true
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone2WeekendName,zone2WeekendTemperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = true
                state.zone4WeekendSet = false

                setThermostat(zone3WeekendName,zone3WeekendTemperature)            
            }

            // Are we between 4th time and midnight, schedule the next day
            else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true

                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }

            // Are we between midnight yesterday and 1st time, schedule today
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true

                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }
            break

            case Calendar.SUNDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = true
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone1WeekendName,zone1WeekendTemperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = true
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false

                setThermostat(zone2WeekendName,zone2WeekendTemperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = true
                state.zone4WeekendSet = false

                setThermostat(zone3WeekendName,zone3WeekendTemperature)            
            }

            // Are we between 4th time Sunday and midnight, we schedule Monday
            else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true

                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }

            // Are we between midnight Sunday and 1st time on Monday, we schedule Monday
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4WeekendSet) { 
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true

                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }
            break
        }
    }
    runIn(60,setRequiredZone)
}

def setThermostat(zoneName,zoneTemperature) {
	thermostat.setHeatingSetpoint(zoneTemperature)
    	thermostat.setZoneName(zoneName)
}

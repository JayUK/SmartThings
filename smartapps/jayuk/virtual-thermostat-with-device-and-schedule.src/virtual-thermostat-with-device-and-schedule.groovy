definition(
	name: "Virtual Thermostat With Device and Schedule",
	namespace: "JayUK",
	author: "JayUK",
	description: "Control a heater in conjunction with any temperature sensor, like a SmartSense Multi.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo.png",
	parent: "JayUK:Virtual Thermostat Manager",
)
// ********************************************************************************************************************
preferences {
	section("Choose a temperature sensor(s)... (If multiple sensors are selected, the average value will be used)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true, required: true
	}
	section("Select the heater outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true, required: true
	}
	section("Only heat when a contact isn't open (optional, leave blank to not require contact sensor)..."){
		input "motions", "capability.contactSensor", title: "Contact", required: false, multiple: true, hideWhenEmpty: true
	}
 	section("Only heat when a person is present (optional, leave blank to not require presence sensor)..."){
		input "presences", "capability.presenceSensor", title: "Presence", required: false, multiple: true, hideWhenEmpty: true
	}
   	section("Presence away temperature: (optional)"){
		input "awaySetpoint", "decimal", title: "Away Temp (1-40)", range: "1..40", required: false, hideWhenEmpty: "presences"
	}
	section("Never go below this temperature (even if heating is turned off): (optional)"){
		input "emergencySetpoint", "decimal", title: "Emergency Temp (1-30)", range: "1..30", required: false
	}
    section("Temperature Threshold (Don't allow heating to go above or below this amount from set temperature)") {
		input "threshold", "decimal", "title": "Temperature Threshold (1-10)", range: "1..10", required: false, defaultValue: 1.0
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
    section("Boost") {
		input "boostDuration", "number", title: "Boost duration (5 - 60 minutes)", range: "5..60", defaultValue: 60, required: true
		input "boostTemperature", "decimal", title: "Amount to increase temperature by (1-10)", range: "1..10", defaultValue: 1, required: true
    }
}
// ********************************************************************************************************************
def installed()
{
	log.debug "Installed: Running installed"
	state.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
	state.contact = true
	state.presence = true
    state.boost = false
	state.previousZoneNamePresence = null
    state.previousZoneNameContact = null
    state.previousZoneTemperature = null
	state.todayTime = 0
	state.yesterdayTime = 0
	state.date = new Date().format("dd-MM-yy")
	state.lastOn = 0
    state.previousZoneNameBoost = null
    state.previousTemperatureBoost = null

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
// ********************************************************************************************************************
def createDevice() {
	def thermostat
	def label = app.getLabel()

	log.debug "CreateDevice: Create device with id: pmvt$state.deviceID, named: $label"
	
	try {
		thermostat = addChildDevice("JayUK", "Virtual Thermostat Device", "pmvt" + state.deviceID, null, [label: label, name: label, completedSetup: true])
	} catch(e) {
		log.error("CreateDevice: Caught exception", e)
	}
	
	return thermostat
}
// ********************************************************************************************************************
def getThermostat() {
	def child = getChildDevices().find {
	d -> d.deviceNetworkId.startsWith("pmvt" + state.deviceID)
	}
	return child
}
// ********************************************************************************************************************
def uninstalled() {
    	deleteChildDevice("pmvt" + state.deviceID)
}
// ********************************************************************************************************************
def updated() {
	log.debug "Updated: $app.label"
	unsubscribe()
	unschedule()

	def thermostat = getThermostat()

	if(thermostat == null) {
		log.debug "Updated: Creating thermostat"
		thermostat = createDevice()
	}

	if(state.todayTime == null) state.todayTime = 0
	if(state.yesterdayTime == null) state.yesterdayTime = 0
	if(state.date == null) state.date = new Date().format("dd-MM-yy")
	if(state.lastOn == null) state.lastOn = 0
	if(state.previousZoneTemperature == null) state.previousZoneTemperature = thermostat.currentValue("thermostatSetpoint")
    
	subscribe(sensors, "temperature", temperatureHandler)

	if (motions) {
		log.debug "Updated: Contact sensor(s) selected"
		subscribe(motions, "contact", motionHandler)
	} else {
		log.debug "Updated: No contact sensor selected"
	}

	if (presences) {
		log.debug "Updated: Presence sensor(s) selected"
		subscribe(presences, "presence", presenceHandler)
	} else {
		log.debug "Updated: No presence sensor selected"
	}

	subscribe(thermostat, "thermostatBoost", thermostatBoostHandler)
	subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
	subscribe(thermostat, "thermostatMode", thermostatModeHandler)
	thermostat.clearSensorData()
	thermostat.setVirtualTemperature(getAverageTemperature())
	thermostat.setTemperatureScale(parent.getTempScale())
	
    runEvery1Hour(updateTimings)
    initialize()
}
// ********************************************************************************************************************
def initialize() {
 	evaluateRoutine()
    runIn(60,initialize)
}
// ********************************************************************************************************************
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
// ********************************************************************************************************************
def temperatureHandler(evt) {
	def thermostat = getThermostat()
	thermostat.setVirtualTemperature(getAverageTemperature())

	if ((state.contact && state.presence) || emergencySetpoint) {
		evaluateRoutine()
	} else {
		heatingOff()
	}
}
// ********************************************************************************************************************
def thermostatBoostHandler(evt) {
	log.debug "ThermostatBoostHandler: Boost has been requested. Boost value: $state.boost"
      
    def thermostat = getThermostat()
    
    if (state.boost == false) {
    	log.debug "ThermostatBoostHandler: Not currently boosted, remembering previous values"
    	state.previousZoneNameBoost = thermostat.currentValue("zoneName")
        state.previousTemperatureBoost = thermostat.currentValue("thermostatSetpoint")
    
    	def boostTemp = thermostat.currentValue("thermostatSetpoint") + boostTemperature
    
    	def nowtime = now()
        
        // Add one hour to the current time
		def nowtimePlusOneHour = nowtime + (boostDuration * 60000)
        def boostEndTime = new Date(nowtimePlusOneHour)

        log.debug "ThermostatBoostHandler: Setting zonename to 'Boosted' and thermostat temperature to $boostTemp"
        
    	setThermostat("Boosted" + "\n" + "(" + boostEndTime.format('HH:mm',location.timeZone) + ")",boostTemp)
       
    	log.debug "ThermostatBoostHandler: Scheduling boost to be removed in 1 hour"
    	runIn((boostDuration*60),boostOff)
  
  		state.boost = true
  } else {
  	log.debug "ThermostatBoostHandler: Already boosted, not doing anything"
  }    
	
  evaluateRoutine()
}
// ********************************************************************************************************************
def boostOff() {

	if (state.boost) {
		log.debug "BoostOff: Restoring previous values, Zonename: $state.previousZoneNameBoost Temperature: state.previousTemperatureBoost"
		state.boost = false
        setThermostat(state.previousZoneNameBoost,state.previousTemperatureBoost)
	} else {
    	log.debug "BoostOff: Dont have to reset boosted Zone names or temperature due to thermostat temperature or zone change during boost period"
    }
}
// ********************************************************************************************************************
def motionHandler(evt) {
    def thermostat = getThermostat()
	
    state.contact = true
    
    for(motionSensor in motions) {
    			
        if (motionSensor.ContactState == "open") {
			log.debug "MotionHandler: Contact sensor open: $motionSensor"
            state.contact = false
        }
	}
    
    if (state.contact) {
    	log.debug "MotionHandler: Setting zone name back to the previous zone name (temporary until we check what Zone we should be in: $state.previousZoneName"
        thermostat.setZoneName(state.previousZoneNameContact)
        evaluateRoutine()                  
	} else {
        log.debug "MotionHandler: Contact sensor open - Turn heating off"
		state.previousZoneNameContact = thermostat.currentValue("zoneName")
		thermostat.setZoneName("Contact: Open")
		heatingOff()
	}
}
// ********************************************************************************************************************
def presenceHandler(evt) {
    def thermostat = getThermostat()
    
    state.presence = true

    // Lets loop through all the presence sensors and check their status
    for(presenceSensor in presences) {
        
        if (presenceSensor.currentPresence == "not present") {
            log.debug "PresenceHandler: Presence away: $presenceSensor"
            state.presence = false
        } 
    }

    if (state.presence) {
        log.debug "PresenceHandler: Setting zone name back to the previous zone name (temporary until we check what Zone we should be in: $state.previousZoneNamePresence"
        setThermostat(state.previousZoneNamePresence,state.previousZoneTemperature)
        evaluateRoutine()
    } else {
        log.debug "PresenceHandler: Presence away"
        state.previousZoneNamePresence = thermostat.currentValue("zoneName")
        state.previousZoneTemperature = thermostat.currentValue("thermostatSetpoint")
        
        if (awaySetpoint != null) {
        	log.debug "PresenceHandler: awaySetpoint: $awaySetpoint"
			setThermostat("Presence: Away",awaySetpoint)
            evaluateRoutine()
		} else {
        	thermostat.setZoneName("Presence: Away")
            heatingOff()
        }
    }
}
// ********************************************************************************************************************
def thermostatTemperatureHandler(evt) {
	// Function used when temperature on virtual thermostat is changed
    
    if (state.boost) {
		log.debug "ThermostatTemperatureHandler: Restoring zone name from 'Boosted' to previous name: $state.previousZoneNameBoost"
		state.boost = false
        
        def thermostat = getThermostat()
        thermostat.setZoneName(state.previousZoneNameBoost)
	} else {
    	log.debug "ThermostatTemperatureHandler: Not in 'boost' mode, nothing to reset"
    }    
    
    evaluateRoutine()
	}
// ********************************************************************************************************************
def thermostatModeHandler(evt) {
	def mode = evt.value
	log.debug "ThermostatModeHandler: Mode Changed to: $mode"
    
    if (mode == "heat") {
    	if (state.contact && (state.presence || (!state.presence && awaySetpoint != null))) {
			log.debug "ThermostatModeHandler: Contact/Presence is True, performing evaluation"
            evaluateRoutine()
		}
		else {
        	log.debug "ThermostatModeHandler: Either Presence or Contact away/open, turning off heating"
			heatingOff(mode == 'heat' ? false : true)
		}
	} else {
       	log.debug "ThermostatModeHandler: Heating off"
			heatingOff(mode == 'heat' ? false : true)
    }
}
// ********************************************************************************************************************
private evaluateRoutine() {

	setRequiredZone()
    
    def currentTemp = getAverageTemperature()
    def desiredTemp = thermostat.currentValue("thermostatSetpoint")
	def heatingMode = thermostat.currentValue('thermostatMode')
    
	log.debug "EvaluateRoutine: Current: $currentTemp, Desired: $desiredTemp, Heating mode: $heatingMode"
	   
    if (currentTemp <= emergencySetpoint) {
    	log.debug "EvaluateRountine: In Emergency Mode, turning on"
        thermostat.setEmergencyMode(true)
        outletsOn()
    } else if ((desiredTemp - currentTemp >= threshold)) {
        log.debug "EvaluateRoutine: Current temperature is below desired temperature (with threshold)"
 
 		if(thermostat.currentValue('thermostatMode') == 'heat') {
			log.debug " EvaluateRoutine: Heating is enabled"
            
            if (state.contact && (state.presence || (!state.presence && awaySetpoint != null))) {
            	if (state.presence) {
               		log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed and someone is present - Turning on"
                } else {
                	log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed, no one present but away temp set - Turning on"
                }
                thermostat.setHeatingStatus(true)
            	outletsOn()
            } else {
	            log.debug "EvaluateRoutine: Heating is enabled - But a contact is open, or no one is present and no away temperature is set - Not turning on"
    	        heatingOff()  
            }
        } else {
            log.debug " EvaluateRoutine: Heating is disabled - Not turning on"      
            heatingOff()
        }
    } else if ((currentTemp - desiredTemp >= threshold)) {
        log.debug "EvaluateRoutine: Current temperature is above desired temp (with threshold)"    
        heatingOff()
    } else {
    	log.debug "EvaluateRoutine: Current temperature matches desired temperature (within the threshold)"
    }
    
    if(state.current == "on") {
        updateTimings()
    }
}
// ********************************************************************************************************************
def heatingOff(heatingOff) {
	def thisTemp = getAverageTemperature()
    
	if (thisTemp <= emergencySetpoint) {
		log.debug "HeatingOff: In Emergency Mode, not turning off"
		outletsOn()
		thermostat.setEmergencyMode(true)
	} else {
		log.debug "HeatingOff: Heating off"
		outletsOff()
        
		if(thermostat.currentValue('thermostatMode') == 'heat') {
			log.debug "HeatingOff: setHeatingStatus to False"
            thermostat.setHeatingStatus(false)
		} else {
        	log.debug "HeatingOff: setHeatingOff to True"
			thermostat.setHeatingOff(true)
		}
	}
}
// ********************************************************************************************************************
def updateTempScale() {
	thermostat.setTemperatureScale(parent.getTempScale())
}
// ********************************************************************************************************************
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
// ********************************************************************************************************************
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
// ********************************************************************************************************************
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
// ********************************************************************************************************************
def setRequiredZone() {
    
    /* Only preform the main body of this procedure if we aren't away or a window/door is open.
    Irrespective of the above, we will still reschedule this process to run in 60 seconds */ 
    if (state.contact && state.presence) {
        def calendar = Calendar.getInstance()
        calendar.setTimeZone(location.timeZone)
        def today = calendar.get(Calendar.DAY_OF_WEEK)
        def timeNow = now()
        def midnightToday = timeToday("2000-01-01T23:59:59.999-0000", location.timeZone)

		// Reset the zone flags and update day because of day change. Covers midnight change
		if (today != state.storedDay) {
        		log.debug "setRequiredZone: The day has changed since the last zone change, reseting zone check flags"
                
        	    state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
       
                state.storedDay = today
        }
        
        // This section is where the time/temperature schedule is set
        switch (today) {
            case Calendar.MONDAY:
            case Calendar.TUESDAY:
            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
                
                log.debug "SetRequiredZone: Mon-Thu - Zone 1"
                
                state.zone1Set = true
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone1Name,zone1Temperature)            
            }

            // Are we between 2nd time and 3rd time        	
            else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 

				log.debug "SetRequiredZone: Mon-Thu - Zone 2"
                
				state.zone1Set = false
                state.zone2Set = true
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone2Name,zone2Temperature)            
            }

            // Are we between 3rd time and 4th time	
            else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
                
                log.debug "SetRequiredZone: Mon-Thu - Zone 3"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = true
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone3Name,zone3Temperature)            
            }

            // Are we between 4th time and midnight, schedule next day				
            else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
                
                log.debug "SetRequiredZone: Mon-Thu - Zone 4 (upto midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone4Name,zone4Temperature)            
            }

            // Are we between midnight yesterday and 1st time, schedule today
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4Set) { 
            
	            log.debug "SetRequiredZone: Mon-Thu - Zone 4 (after midnight)"
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone4Name,zone4Temperature)            
            }
            break

            case Calendar.FRIDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
                
                log.debug "SetRequiredZone: Friday - Zone 1"
                
                state.zone1Set = true
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone1Name,zone1Temperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
                
                log.debug "SetRequiredZone: Friday - Zone 2"
                
                state.zone1Set = false
                state.zone2Set = true
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone2Name,zone2Temperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
                
                log.debug "SetRequiredZone: Friday - Zone 3"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = true
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone3Name,zone3Temperature)            
            }

            // Are we between 4th time Friday and midnight, we schedule Saturday
            else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
                
                log.debug "SetRequiredZone: Friday - Zone 4 (upto midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone4Name,zone4Temperature)            
            }

            // Are we between midnight Friday and 1st time on Saturday, we schedule Saturday
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4Set) { 
                
                log.debug "SetRequiredZone: Friday - Zone 4 (after midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = true
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone4Name,zone4Temperature)            
            }
            break

            case Calendar.SATURDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
                
                log.debug "SetRequiredZone: Saturday - Zone 1"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = true
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone1WeekendName,zone1WeekendTemperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
                
                log.debug "SetRequiredZone: Saturday - Zone 2"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = true
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone2WeekendName,zone2WeekendTemperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
                
                log.debug "SetRequiredZone: Saturday - Zone 3"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = true
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone3WeekendName,zone3WeekendTemperature)            
            }

            // Are we between 4th time and midnight, schedule the next day
            else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
                
                log.debug "SetRequiredZone: Saturday - Zone 4 (upto midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true
                state.boost = false
                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }

            // Are we between midnight yesterday and 1st time, schedule today
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4WeekendSet) { 

				log.debug "SetRequiredZone: Saturday - Zone 4 (after midnight)"
                
				state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true
                state.boost = false
                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }
            break

            case Calendar.SUNDAY:

            // Are we between 1st time and 2nd time
            if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
                
                log.debug "SetRequiredZone: Sunday - Zone 1"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = true
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone1WeekendName,zone1WeekendTemperature)            
            }

            // Are we between 2nd time and 3rd time
            else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
                
                log.debug "SetRequiredZone: Sunday - Zone 2"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = true
                state.zone3WeekendSet = false
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone2WeekendName,zone2WeekendTemperature)            
            }

            // Are we between 3rd time and 4th time
            else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
                
                log.debug "SetRequiredZone: Sunday - Zone 3"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = true
                state.zone4WeekendSet = false
                state.boost = false
                setThermostat(zone3WeekendName,zone3WeekendTemperature)            
            }

            // Are we between 4th time Sunday and midnight, we schedule Monday
            else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
                
                log.debug "SetRequiredZone: Sunday - Zone 4 (upto midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true
                state.boost = false
                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }

            // Are we between midnight Sunday and 1st time on Monday, we schedule Monday
            else if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4WeekendSet) { 
                
                log.debug "SetRequiredZone: Sunday - Zone 4 (after midnight)"
                
                state.zone1Set = false
                state.zone2Set = false
                state.zone3Set = false
                state.zone4Set = false
                state.zone1WeekendSet = false
                state.zone2WeekendSet = false
                state.zone3WeekendSet = false
                state.zone4WeekendSet = true
                state.boost = false
                setThermostat(zone4WeekendName,zone4WeekendTemperature)            
            }
            break
        }
    }
}
// ********************************************************************************************************************
def setThermostat(zoneName,zoneTemperature) {
	thermostat.setHeatingSetpoint(zoneTemperature)
    thermostat.setZoneName(zoneName)
}
// ********************************************************************************************************************
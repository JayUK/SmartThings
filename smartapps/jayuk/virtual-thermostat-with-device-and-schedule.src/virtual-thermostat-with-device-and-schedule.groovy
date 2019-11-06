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
	section("Only heat when a contact isn't open (optional, leave blank to not require contact detection)...", hideWhenEmpty: true){
		input "contacts", "capability.contactSensor", title: "Contact", required: false, multiple: true, hideWhenEmpty: true
	}
 	section("Only heat when a person is present (optional, leave blank to not require presence detection)...", hideWhenEmpty: true){
		input "presences", "capability.presenceSensor", title: "Presence", required: false, multiple: true, hideWhenEmpty: true
        input "presenceMinimumDuration", "number", title: "Minimum duration a presence stays active for (Mins: 0-30)", range: "0..30", defaultValue: 0, required: true, hideWhenEmpty: "presences"
        input "presenceAwaySetpoint", "decimal", title: "Away Temperature (1-40)", range: "1..40", required: false, hideWhenEmpty: "presences"
	}
   	section("Only heat when a movement is detected (optional, leave blank to not require motion detection)...", hideWhenEmpty: true){
		input "motions", "capability.motionSensor", title: "Motion", required: false, multiple: true, hideWhenEmpty: true
        input "motionDuration", "number", title: "Duration a motion stays active for (Mins: 1-30)", range: "1..30", required: false, hideWhenEmpty: "motions"
        input "motionAwaySetpoint", "decimal", title: "Away Temp (1-40)", range: "1..40", required: false, hideWhenEmpty: "motions"
	}
   	section("Never go below this temperature (even if heating is turned off): (optional)"){
		input "emergencySetpoint", "decimal", title: "Emergency Temp (1-30)", range: "1..30", required: false
	}
    section("Temperature Threshold (Don't allow heating to go above or below this amount from set temperature)") {
		input "aboveThreshold", "decimal", "title": "Above Temperature Threshold (0-10)", range: "0..10", required: true, defaultValue: 0.5
        input "belowThreshold", "decimal", "title": "Below Temperature Threshold (0-10)", range: "0..10", required: true, defaultValue: 0.5
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
    section("Minimum on time") {
		input "minOnTime", "number", title: "Minimum time the outlets stay on (0-10 minutes)", range: "0..10", defaultValue: 0, required: true
    }

}
// ********************************************************************************************************************
def installed()
{
	log.debug "Installed: Running installed"
	state.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
	state.contact = true
	state.presence = true
    state.motion = true
    state.boost = false
	state.previousZoneNamePresence = null
    state.previousZoneNameContact = null
    state.previousZoneTemperaturePresence = null
	state.todayTime = 0
	state.yesterdayTime = 0
    state.turnOnTime = 0
	state.date = new Date().format("dd-MM-yy")
	state.lastOn = 0
    state.presenceTime = Math.round(new Date().getTime() / 1000)
    state.previousZoneNameBoost = null
    state.previousTemperatureBoost = null
	state.presenceAwayScheduled = false

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
 
	subscribe(sensors, "temperature", temperatureHandler)
	subscribe(thermostat, "thermostatBoost", thermostatBoostHandler)
	subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
	subscribe(thermostat, "thermostatMode", thermostatModeHandler)
	
    thermostat.clearSensorData()
	thermostat.setVirtualTemperature(getAverageTemperature())
	thermostat.setTemperatureScale(parent.getTempScale())
	
    if (contacts) {
		log.debug "Updated: Contact sensor(s) selected"
		subscribe(contacts, "contact", contactHandler)
	} else {
		log.debug "Updated: No contact sensor selected"
	}

	if (presences) {
		log.debug "Updated: Presence sensor(s) selected"
		subscribe(presences, "presence", presenceHandler)
	} else {
		log.debug "Updated: No presence sensor selected"
	}
    
    if (motions) {
		log.debug "Updated: Motion sensor(s) selected"
		subscribe(motions, "motion.inactive", motionHandler)
	} else {
		log.debug "Updated: No motion sensor selected"
	}

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

	if (state.contact && (state.motion || (motionAwaySetpoint != null)) && (state.presence || (presenceAwaySetpoint != null)) && (state.motion || (motionAwaySetpoint != null)) || emergencySetpoint) {
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
        
        // Add boost duration to the current time
		def nowtimePlusBoostDuration = nowtime + (boostDuration * 60000)
        def boostEndTime = new Date(nowtimePlusBoostDuration)

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
        
        log.debug "BoostOff: Canceling any scheduled boostOff jobs"
        unschedule (boostOff)
	} else {
    	log.debug "BoostOff: Dont have to reset boosted Zone names or temperature due to thermostat temperature or zone change during boost period"
    }
}
// ********************************************************************************************************************
def contactHandler(evt) {
    def thermostat = getThermostat()
    
    def contactClosed = true 
    
    for(contactSensor in contacts) {
    			
        if (contactSensor.ContactState == "open") {
			log.debug "ContactHandler: Contact sensor open: $contactSensor"
            contactClosed = false
        }
	}
    
    if (state.contact == false && contactClosed) {
    	log.debug "ContactHandler: Setting zone name back to the previous zone name (temporary until we check what Zone we should be in: $state.previousZoneName"
        thermostat.setZoneName(state.previousZoneNameContact)
        state.contact = true
        evaluateRoutine()                  
	} else if (state.contact == false && contactClosed == false) {
    	log.debug "ContactHandler: Already in away mode and another contact has gone open - doing nothing"
    } else if (state.contact && contactClosed == false ){
        log.debug "ContactHandler: First contact sensor to become open - Turn heating off"
		state.previousZoneNameContact = thermostat.currentValue("zoneName")
		thermostat.setZoneName("Contact: Open")
		state.contact = false
        heatingOff()
	}
}
// ********************************************************************************************************************
def presenceHandler(evt) {
    def thermostat = getThermostat()
    
    def presenceHere = false
    
    // Lets loop through all the presence sensors and check their status
    for(presenceSensor in presences) {
        if (presenceSensor.currentPresence == "present") {
            log.debug "PresenceHandler: Presence detected, sensor: $presenceSensor"
            presenceHere = true
        } 
    }
  
    if (state.presence == false && presenceHere) {
        
        if (presenceAwaySetpoint != null) {
        	log.debug "PresenceHandler: We have detected a presence and we had an away temp set, setting zone name and temp back to previous values (temporary until we check what Zone we should be in"
        	setThermostat(state.previousZoneNamePresence,state.previousZoneTemperaturePresence)
        } else {
        	log.debug "PresenceHandler: We have detected a presence and but we dont have an away temp set, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        	thermostat.setZoneName(state.previousZoneNamePresence)
        }
        state.presence = true
        state.presenceTime = Math.round(new Date().getTime() / 1000)
        unschedule(presenceAway)
        evaluateRoutine()
    } else if (state.presence == false && presenceHere == false) {
    	log.debug "PresenceHandler: Already in away mode and all presence sensors are set as away - Doing nothing"
    } else if(state.presence && presenceHere == false) {
    	log.debug "PresenceHandler: First occurance of all presence sensors being away, so scheduling/rescheduling presenceAway to run"
    	              
        state.previousZoneNamePresence = thermostat.currentValue("zoneName")  
    	state.previousZoneTemperaturePresence = thermostat.currentValue("thermostatSetpoint")
        
        if (presenceMinimumDuration > 0) {
        	def presenceMinimumDurationSeconds = presenceMinimumDuration * 60
        
            def time = Math.round(new Date().getTime() / 1000)
            def presenceDuration = time - state.presenceTime

            if (presenceDuration < presenceMinimumDurationSeconds) {
            
                log.debug "PresenceHandler: Presence duration is below specified minimum - Duration: $presenceDuration Minimum: $presenceMinimumDurationSeconds"
                
                def presenceExtraDurationSeconds = presenceMinimumDurationSeconds-presenceDuration
                def presenceAwayTime = new Date(now() + (presenceExtraDurationSeconds*1000))

				thermostat.setZoneName("Presence: Away at ${presenceAwayTime.format('HH:mm')}")
                
                if (presenceExtraDurationSeconds > 60) {
                    log.debug "PresenceHandler: Presence duration is below specified minimum, scheduling for minimum period - Scheduling to run in: $presenceExtraDurationSeconds seconds"
                    state.presenceAwayScheduled = true
                    runIn(presenceExtraDurationSeconds, presenceAway)
                } else {
                    log.debug "PresenceHandler: Remaining minimum duration is less than 60 seconds, scheduling presenceAway to run in 60 seconds"
                    state.presenceAwayScheduled = true
                    runIn(60, presenceAway)
                }
            } else {
				log.debug "PresenceHandler: Presence duration has exceeded minimum specified value, running presenceAway now"
        		presenceAway()
            }
    	} else {
        	log.debug "PresenceHandler: No minimum duration specified, running presenceAway now"
        	presenceAway()
        }
	} else if (state.presenceAwayScheduled & presenceHere) {
    	
        state.presenceAwayScheduled = false
        state.presenceTime = Math.round(new Date().getTime() / 1000)
        unschedule(presenceAway)

        log.debug "PresenceHandler: We have detected a presence while pending, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        thermostat.setZoneName(state.previousZoneNamePresence)

        evaluateRoutine()
    }
}
// ********************************************************************************************************************
def motionHandler(evt) {
               
    log.debug "MotionHandler: Event occured: $evt.value"
    
    def motionDetected = false

	for(motionSensor in motions) {
        if (motionSensor.ActivityStatus == "active") {
            log.debug "MotionHandler: A sensor is showing activity: $motionSensor"
            motionDetected = true
        }
    }

	if (state.motion == false && motionDetected) {
        log.debug "MotionHandler: Activity detected and we're in away mode. Exiting away mode: Resetting zone details and unscheduling motionOff"
        if (motionAwaySetpoint != null) {
            log.debug "MotionHandler: We have detected motion and we had an away temp set, setting zone name and temperature back to previous values (temporary until we check what Zone we should be in"
        	setThermostat(state.previousZoneNameMotion,state.previousZoneTemperatureMotion)
        } else {
        	log.debug "MotionHandler: We have detected motion and but we dont have an away temperature set, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        	thermostat.setZoneName(state.previousZoneNameMotion) 
        }
        state.motion = true
        evaluateRoutine()
        unschedule (motionOff)
    } else if (state.motion == false && motionDetected == false) {
		log.debug "MotionHandler: Motion not detected and already in away mode. Doing nothing"
    } else if (state.motion && motionDetected == false) {
    	log.debug "MotionHandler: First occurance of all motion sensors being away, so scheduling/rescheduling motionOff to run from now plus duration time"
        runIn(motionDuration*60, motionOff)
    }
}
// ********************************************************************************************************************
def motionOff() {
	
    log.debug "MotionOff: Executing"
    
    state.previousZoneNameMotion = thermostat.currentValue("zoneName")  
    state.previousZoneTemperatureMotion = thermostat.currentValue("thermostatSetpoint")
      
    state.motion = false
      
     if (motionAwaySetpoint != null) {
        	log.debug "MotionOff: motionAwaySetpoint: $motionAwaySetpoint - Adjusting thermostat accordingly and leaving heating enabled"
			setThermostat("Motion: Away",motionAwaySetpoint)
            evaluateRoutine()
		} else {
        	log.debug "MotionOff: No away temp set, turning off heating"
        	thermostat.setZoneName("Motion: Away")
            heatingOff()
        }
}
// ********************************************************************************************************************
def presenceAway() {
	
    log.debug "PresenceAway: Executing"
          
    state.presence = false
    state.presenceTime = 0
    state.presenceAwayScheduled = false
    
     if (presenceAwaySetpoint != null) {
        	log.debug "PresenceAway: presenceAwaySetpoint: $presenceAwaySetpoint - Adjusting thermostat accordingly and leaving heating enabled"
			setThermostat("Presence: Away",presenceAwaySetpoint)
            evaluateRoutine()
		} else {
        	log.debug "PresenceAway: No away temp set, turning off heating"
        	thermostat.setZoneName("Presence: Away")
            heatingOff()
        }
}
// ********************************************************************************************************************
def thermostatTemperatureHandler(evt) {
	// Function used when temperature on virtual thermostat is changed
    
    if (state.boost) {
		log.debug "ThermostatTemperatureHandler: Restoring zone name from 'Boosted' to previous name: $state.previousZoneNameBoost"
		state.boost = false
        unschedule (boostOff)
        
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
    	if (state.contact && (state.presence || (presenceAwaySetpoint != null)) && (state.motion || (motionAwaySetpoint != null))) {
			log.debug "ThermostatModeHandler: Contact/Presence is True, performing evaluation"
            evaluateRoutine()
		}
		else {
        	log.debug "ThermostatModeHandler: Either no presence (or presence temp not set), or Contact open, no motion (or motion temp not set), turning off heating"
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
    } else if ((desiredTemp - currentTemp) >= belowThreshold) {
        log.debug "EvaluateRoutine: Current temperature is below desired temperature (with threshold)"
 
 		if(thermostat.currentValue('thermostatMode') == 'heat') {
			log.debug " EvaluateRoutine: Heating is enabled"
            
            if (state.contact && (state.motion || (motionAwaySetpoint != null)) && (state.presence || (presenceAwaySetpoint != null)) && (state.motion || (motionAwaySetpoint != null))) {
            	if (state.presence && presences) {
               		log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed and someone is present - Turning on"
                } else {
                	log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed, no one present but presence away temp set - Turning on"
                }
                if (state.motion && motions) {
               		log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed and someone is moving - Turning on"
                } else {
                	log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed, no one is moving but motion away temp set - Turning on"
                }
                thermostat.setHeatingStatus(true)
            	outletsOn()
            } else {
	            log.debug "EvaluateRoutine: Heating is enabled - But a contact is open, or no one is present (or not and no away temp set), or no one is moving (or not and no away temp set) - Turning off"
    	        heatingOff()  
            }
        } else {
            log.debug " EvaluateRoutine: Heating is disabled - Turning off"      
            heatingOff()
        }
    } else if ((currentTemp - desiredTemp) >= aboveThreshold) {
        log.debug "EvaluateRoutine: Current temperature is above desired temp (with threshold) - Turning off"    
        heatingOff()
    } else {
    	log.debug "EvaluateRoutine: Current temperature matches desired temperature (within the thresholds) - Doing nothing"
    }
    
    if(state.current == "on") {
        updateTimings()
    }
}
// ********************************************************************************************************************
def heatingOff(heatingOff) {
	def thisTemp = getAverageTemperature()
    
    def time = Math.round(new Date().getTime() / 1000)
    def onFor = time - state.turnOnTime
    def minOnTimeSeconds = minOnTime * 60
    
    log.debug "HeatingOff: state.turnOnTime: $state.turnOnTime  time: $time   difference: $onFor     minOnTime: $minOnTimeSeconds"
    
	if (thisTemp <= emergencySetpoint) {
		log.debug "HeatingOff: In Emergency Mode, not turning off"
		outletsOn()
		thermostat.setEmergencyMode(true)
	} else {
    	if (onFor >= minOnTimeSeconds) {
            if (thermostat.currentValue('thermostatMode') == 'heat') {
            	log.debug "HeatingOff: Time on is greater than specified minimum on period - turning off"
                thermostat.setHeatingStatus(false)
            } else {
            	log.debug "HeatingOff: setHeatingOff to True - Thermostat has been turned off"
                thermostat.setHeatingOff(true)
            }
            
            outletsOff()
            
		} else {
        	log.debug "HeatingOff: Time on is less than than specified minimum on period - doing nothing"
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
    state.turnOnTime = Math.round(new Date().getTime() / 1000)
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
    state.turnOnTime = 0
	state.lastOn = 0
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}
// ********************************************************************************************************************
def setRequiredZone() {
    
    /* Only preform the main body of this procedure if we aren't away or a window/door is open.
    Irrespective of the above, we will still reschedule this process to run in 60 seconds */ 
    if (state.contact && (state.motion || (motionAwaySetpoint != null)) && (state.presence || (presenceAwaySetpoint != null)) && (state.motion || (motionAwaySetpoint != null)) || emergencySetpoint) {
        def calendar = Calendar.getInstance()
        calendar.setTimeZone(location.timeZone)
        def today = calendar.get(Calendar.DAY_OF_WEEK)
        def timeNow = now()
        def midnightToday = timeToday("2000-01-01T23:59:59.999-0000", location.timeZone)
/* This shouldnt be needed
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
*/        
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
                unschedule(boostOff)
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
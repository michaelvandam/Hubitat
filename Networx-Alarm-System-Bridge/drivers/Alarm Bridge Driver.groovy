/**
 *  Driver for connecting to an Interlogix Networx alarm system outfitted with the alarm system bridge device
 *  developed by Leef Automation.
 * 
 *  Copyright 2019 R. Michael van Dam
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  VERSION 1.0.0
 *  UPDATE HISTORY:
 *  2019-12-15 - R. Michael van Dam - Redesigned as a driver and app for Hubitat
 *  2019-08-02 - LeeF Automation - Original SmartThings version
 * 
 * CREDITS:
 *  This code was heavily modified from LeeF Automation's SmartThings version, including dividing it into
 *  a separate Driver and SmartApp.
 */

import groovy.json.JsonSlurper

metadata
{
	definition (name: "Alarm System Driver Interlogix", namespace: "alarm", author: "R. Michael van Dam")
	{
		capability "Refresh"
		capability "Configuration"
		capability "Alarm"
		capability "Polling"

        // Alarm panel status
		attribute "status", "string"       
        
        // Primary commands
		command "alarmArmAway"
		command "alarmArmHome"
		command "alarmDisarm"
        command "alarmPanic", [[name:"type*", type:"ENUM", constraints:["police", "medical", "fire"]]]
		command "alarmTrigger"
        
        // Bypass commands
        command "zoneBypass", [[name:"zoneId*", type:"NUMBER", description: "Zone Number"], [name:"action*", type:"ENUM", constraints:["bypass","clear"]]]
        command "zoneBypassClearAll"
        command "alarmArmHomeBypass"
        command "alarmArmAwayBypass"

        // Utility commands		
        command "createZoneDevices"
		command "removeZoneDevices"
        command "alarmBridgeReset"
		
        // Capability-related commands
		command "stop"
		command "off"
		command "strobe"
		command "siren"
		command "both"
	}

	preferences {
        // QUESTION: should preferences be here or in the app?
        // QUESTION: what does "displayDuringSetup" mean?
		input name: "ip", type: "string", title:"Alarm IP address", description: "e.g. 192.168.1.10", required: true, displayDuringSetup: true
		input name: "zone_name_prefix", type: "string", title:"Zone name prefix", description: "e.g. 'Zone' would give 'Zone Kitchen'", required: false, displayDuringSetup: true
        input name: "zone_dni_prefix", type: "string", title:"Zone DNI prefix", default_value: "alarm_zone_", description: "e.g. 'zone_' would give 'zone_5'", required: false, displayDuringSetup: true
		input name: "inactivityseconds", type: "string", title:"Motion sensor inactivity timeout", description: "override the default of 20s (60s max)", required: false, displayDuringSetup: false
		input name: "password", type: "password", title:"Password", description: "Password to log into the web interface of the bridge device", required:false, displayDuringSetup:false
        input name: "triggerzone", type: "integer", title: "Trigger zone", description: "Zone number used for triggering alarm (0 if unused)", required: false, displayDuringSetup: true
        input name: "disarmzone", type: "integer", title: "Disarm zone", description: "Zone number used for disarming alarm (0 if unused)", required: false, displayDuringSetup: true
        input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: true

	}

}

// ------------------------------------------------------------------------------------------------------------------------
// UTILITY FUNCTIONS
// ------------------------------------------------------------------------------------------------------------------------

/* Add a message to the debugging log. Adds a prefix to enable searching */
private logDebug(message)
{
    def prefix = "Alarm System Driver - "
	if (settings?.debugOutput) {
        log.debug "$prefix$message"
    }
}

/* Return a list of all (child) zone devices */
private getZoneDevices()
{
    return getChildDevices()
}

/* Return a particular zone device */
private getZoneDevice(zone_id)
{
    return getChildDevice(buildZoneDeviceId(zone_id))
}

/* Build zone name using settings.zone_name_prefix */
private buildZoneName(raw_name)
{
    if (settings.zone_name_prefix == null) { return raw_name }
    else { return settings.zone_name_prefix + raw_name }
}

/* Build zone device network ID using settings.zone_dni_prefix */
private buildZoneDeviceId(raw_id)
{
    return settings.zone_dni_prefix + raw_id
}


// ------------------------------------------------------------------------------------------------------------------------
// COMMANDS
// ------------------------------------------------------------------------------------------------------------------------

/* Arm away */
def alarmArmAway()
{
	logDebug("Inisde alarmArmAway()")
	getAction("/armaway")
}

/* Arm Home */
def alarmArmHome()
{
	logDebug("Inside alarmArmHome()")
	getAction("/armhome")
}

/* Disarm
   NOTE: Requires hardware support! This will only work if the appropriate output from the bridge device
   is connected to a zone wired as a keyswitch zone.
*/
def alarmDisarm()
{
    logDebug("Inside alarmDisarm()")
    getAction("/disarm")
}

/* alarmPanic(type)
   Trigger a panic on the alarm panel.
   NOTE: The panic function works even if the panel is not armed.
   - type - type of panic ("police", "medical", "fire")
*/
def alarmPanic(type="police")
{
    logDebug("Inside alarmPanic(${type})")
    getAction("/?action=panic&type=${type}")
}

/* Trigger alarm
   NOTE: Requires hardware support! This will only work if the appropriate output from the
   bridge device is connected to a zone. Will only trigger an alarm if system is armed, or
   if the zone is configured as '24-hour audible'.  But keep in mind this is a momentary toggle
   only. If tripped while alarm is not armed, the panel will be alarming but status will report
   as "disarmed" or "notready". (i.e. Need to keep track elsewhere of the panic state.)
*/
def alarmTrigger()
{
	logDebug("Inside alarmTrigger()")
	getAction("/triggeralarm?output=7")
}

/* zoneBypass(zone_id, action)
   Instruct alarm panel to bypass (or clear) a zone.
   NOTE: command will be ignored if the system is currently armed.
   NOTE: bypasses are all cleared after system is armed then disarmed.
   NOTE: triggerzone and disarmzone cannot be bypassed
   - zone_id - zone number
   - action - "bypass" or "clear"
*/
def zoneBypass(zone_id, action)
{
    logDebug("Inside zoneBypass (${zone_id}, ${action})")
    if ((zone_id == settings.triggerzone) || (zone_id == settings.disarmzone)) {
         logDebug("Ignoring bypass request (can't bypass triggerzone or disarmzone)")
         return
    }
    else {
        def request = ""
        if (action == "bypass") {
            request = "/?bypass&zonenum=${zone_id}"
        } else if (action == "clear") {
            request = "/?bypass=clear&zonenum=${zone_id}"
        } else {
            logDebug("Ignoring zone bypass request (invalid 'action' value: ${action})")
            return
        }
        getAction(request)
    }
}

/* Clear all zone bypasses */
def zoneBypassClearAll()
{
    logDebug("Inside zoneBypassClearAll()")
    devices = getZoneDevices()
    def i = 0
    devices?.each {device ->
        if (device.currentValue("bypass") == "bypass") {
            zone_id = device.getDataValue("zone_id")
            // NOTE that "runIn" is needed to put a delay between the calls. Without a delay some calls get missed.
            runIn(i, zoneBypass, [overwrite: false, data: [zone_id,"clear"]])
            i++
        }
    }
}

/* Arm Home, but bypass all currently open zones. Only contact sensors are checked. */
def alarmArmHomeBypass()
{
    logDebug("In alarmArmHomeBypass()")
    zones = getZoneDevices()
    i = 0
	zones?.each { zone ->
        if (zone.capabilities.find {it.name == "ContactSensor"}) {
            if (zone.currentValue("contact")=="open") {
                zone_id = zone.getDataValue("zone_id")
                // NOTE that "runIn" is needed to put a delay between the calls. Without a delay some calls get missed.
                runIn(i, zoneBypass, [overwrite: false, data: [zone_id, "bypass"]])
                i++
            }
        }
    }
    runIn(i, alarmArmHome)
}

/* Arm Away, but bypass all currently open zones. Only contact sensors are checked. */
def alarmArmAwayBypass()
{
    logDebug("In alarmArmAwayBypass()")
    zones = getZoneDevices()
    i = 0
	zones?.each { zone ->
        if (zone.capabilities.find {it.name == "ContactSensor"}) {
            if (zone.currentValue("contact")=="open") {
                zone_id = zone.getDataValue("zone_id")
                // NOTE that "runIn" is needed to put a delay between the calls. Without a delay some calls get missed.
                runIn(i, zoneBypass, [overwrite: false, data: [zone_id, "bypass"]])
                i++
            }
        }
    }
    runIn(i, alarmArmAway)
}

/* Create (child) zone devices for all zones. This function issues the HTTP request.
   The actual devices are created in the handler delegated by the response parser.
*/
def createZoneDevices()
{
	logDebug("In createZoneDevice() - requesting list of alarm zones")
	getAction("/getzonenames")
}

/* Remove all (child) zone devices */
def removeZoneDevices()
{
	logDebug("In removeZoneDevices()")
    getZoneDevices()?.each {
		try
		{
            logDebug("Deleting child device (${it.deviceNetworkId})")
        	deleteChildDevice(it.deviceNetworkId)
		}
		catch (e)
		{
			logDebug("Error deleting ${it.deviceNetworkId}; possibly locked into a SmartApp: ${e}")
		}
	}
}

/* Reset the alarm bridge device.
   NOTE: currently does not reset the panel
*/
def alarmBridgeReset()
{
    logDebug("In alarmBridgeReset()")
    getAction("/reset")
}

/* Initialize */
def initialize()
{
    logDebug("Inside initialize()") 
    
    // Initialize attributes
    // No suitable default value for state.status. Leave as is.
}

/* Configure the device. Sets up hub communication and motion sensor settings */
def configure()
{
    logDebug("Inside configure()")

    // Re-initialize the device
    initialize()

	def requeststring = "/config?ip_for_st=${device.hub.getDataValue("localIP")}&port_for_st=${device.hub.getDataValue("localSrvPortTCP")}"
	
    if (inactivityseconds?.isInteger())	{
		requeststring = requeststring + "&inactivity_seconds=${settings.inactivityseconds}"
	}
	
	getAction(requeststring)
}

/* Send a request for the device status (and zone status). The data will be received separately
   in a handler delegated by the response parser.
*/
def refresh()
{
	logDebug("Inside refresh()")

    // QUESTION: what does this do? This "192.168.1.7 Port:xxxxx" seems very clunky.  Who is reading this?
    // QUESTION: what does this old comment mean (and should it be sent for every command or just refresh):  SendEvents should be before any getAction, otherwise getAction does nothing
	sendEvent(name: "ip", value: device.hub.getDataValue("localIP")+"\r\nPort: "+device.hub.getDataValue("localSrvPortTCP"))

	getAction("/refresh")
}

/* Called when device is installed */
def installed()
{
	logDebug("In installed()")
    initialize()  
}

/* Called when device is uninstalled */
def uninstalled()
{
    logDebug("In uninstalled()")
	removeChildZones()
}

/* Called when device is updated */
// QUESTION: is this when preferences are changed, or when driver code changes, or both?
def updated()
{
	logDebug("In updated()")
	configure()
}

// ------------------------------------------------------------------------------------------------------------------------
// COMMANDS RELATED TO CAPABILITIES
// ------------------------------------------------------------------------------------------------------------------------

// QUESTION: do we need polling and 'ping' function?
/* Ping */
def ping()
{
	logDebug("In ping()")
	getAction("/ping")
}

def stop()
{
    // Disarm the alarm system
	alarmDisarm()
}

def off()
{
    // Disarm the alarm system
	alarmDisarm()
}

def strobe()
{
    // Formerly was AlarmArmHome()
    // Does nothing (strobe not supported)
    logDebug("WARNING: strobe() not supported")
}

def siren()
{
	// Formerly was AlarmArmAway()
    // Does nothing (siren not supported as independent function)
    logDebug("WARNING: siren() not supported")
}

def both()
{
	// Formerly was AlarmTrigger()
    strobe()
    siren()
}


// ------------------------------------------------------------------------------------------------------------------------
// RESPONSE PARSING
// ------------------------------------------------------------------------------------------------------------------------

/* Main parser
   This depends heavily on detailed API of Leef Automation alarm system bridge. Avoid changes unless you know what you are doing
*/
def parse(description)
{
	def map = [:]
	def events = []
	def cmds = []
	
	if(description == "updated") return
    
	def descMap = parseDescriptionAsMap(description)
	
	if (descMap == null)
	{
		logDebug("Not valid json response/message")
		logDebug(description)
		return
	}

	def body = new String(descMap["body"].decodeBase64())

	def slurper = new JsonSlurper()
	def result;
	try
	{
		result = slurper.parseText(body)
	}
	catch (e)
	{
		logDebug("Invalid response from system: ")
        //logDebug(body)
        logDebug("Uncomment in the code to see the body")
		return
	}
	
	if (result.containsKey("update_type"))
	{
		switch (result.update_type)
		{
            // Alarm status update
			case ["system_status"]: 
				handleAlarmStatus(result.stat_str)
                // TODO: what does this do...?
				// If we receive a key containing 'stat_update_from' then it is an alarm status so add it to the event log and update tile
				if (result.containsKey("stat_update_from"))
				{
					def dateTime = new Date()
					def sensorStateChangedDate = dateTime.format("yyyy-MM-dd HH:mm", location.timeZone)
					def status_string = result.stat_str + " by " + result.stat_update_from + " at " + sensorStateChangedDate
					// Send the status string that we have built
					sendEvent(name: "events", value: "${status_string}", displayed: true, isStateChange: true)
				}
				break
			
            // Single zone status change
			case ["zone_status"]:
				handleZoneStatus(result.zone_id, result.zone_status)
				break
			
            // Refresh status of panel and all zones
            // check which was "stat_str" (overall alarm status, will now include simple status for each zone)
			case ["refresh"]:	
				handleRefresh(result.stat_str, result.zones)
				break

            // List of zone names (for building child devices)
			case ["zone_names"]:
                handleCreateZones(result.zones)
				break			
		}
	}
}

/* Handler for alarm status update */
private handleAlarmStatus(statusString)
{
	switch (statusString)
	{
		case ["Disarmed", "Disarm", "Ready"]:
			logDebug("Disarmed Status found")
			sendEvent(name: "status", value: "disarmed")
			break
				
		case ["Not Ready"]:
			logDebug("Not-ready Status found")
			sendEvent(name: "status", value: "notready")
			break

		case ["Armed Away", "Arm Away"]:
            logDebug("Armed Away Status found")
			sendEvent(name: "status", value: "armaway")
			break

		case ["Armed Home", "Arm Home"]:
			logDebug("Armed Home Status found")
			sendEvent(name: "status", value: "armhome")
			break

		case ["Exit Delay"]:
			logDebug("Exit Delay Status found")
            sendEvent(name: "status", value: "exitdelay")
			break
 
        // QUESTION: is this correct "Delay Alarm" means an alarm was tripped?
        // NOTE: This is a momentary alert only and panel will revert to other status even if alarm continues
		case ["Delay Alarm", "Confirm Alarm"]:
			logDebug("Alarm Status found")
			sendEvent(name: "status", value: "alarm")
			break

		default:
			logDebug("Unknown Alarm status received = ${statusString}")
			break
	}
}

/* Handler for zone status update */
private handleZoneStatus(zoneId, zoneStatus)
{
    // logDebug("Inside handleZoneStatus(zoneId=${zoneId},zoneStatus)")
    def curdevice = null
    try
    {
        curdevice = getZoneDevice(zoneId)
    }
    catch (e)
    {
		logDebug("Failed to find child zone (" + zoneId + "). Error: ${e}")
    }
    if (curdevice != null)
    {
        def thisZoneDeviceId = buildZoneDeviceId(zoneId)

		// Check the device type for this child, since the different child device types need different event types
		boolean isContactDevice = (curdevice.capabilities.find { it.name == "ContactSensor" } != null)
		boolean isMotionDevice = (curdevice.capabilities.find { it.name == "MotionSensor" } != null)
		boolean isSmokeDevice = (curdevice.capabilities.find { it.name == "SmokeDetector" } != null)

        // Check if the device matches the trigger zone number
        // TODO: Also check if matches disarm zone
        boolean isTriggerZone = (zoneId == settings.triggerzone)

		// Handle the specific zone status result
		switch (zoneStatus)
		{				  
			case "Active":
				logDebug("Got Active zone: " + zoneId + " (" + curdevice + ")")
				if (isMotionDevice) {
					curdevice?.sendEvent(name: "motion", value: "active")
				}
				else if (isSmokeDevice) {
					curdevice?.sendEvent(name: "smoke", value: "detected")					
				}
                else if (isContactDevice) {
					curdevice?.sendEvent(name: "contact", value: "open")
				}

                // Set bypass status to "clear"
                curdevice.sendEvent(name:"bypass", value: "clear")
            
				break					

			case "Inactive":
				logDebug("Got Inactive zone: " + zoneId + " (" + curdevice + ")")
				if (isMotionDevice) {
					curdevice?.sendEvent(name: "motion", value: "inactive")
				}
                else if (isSmokeDevice) {
					curdevice?.sendEvent(name: "smoke", value: "clear")					
				}
                else if (isContactDevice) {
					curdevice?.sendEvent(name: "contact", value: "closed")
				}
            
                // Set bypass status to "clear"
                curdevice.sendEvent(name:"bypass", value: "clear")
            
				break 			

			case "Bypassed - Active":
				logDebug("Got Active Bypassed zone: " + zoneId + " (" + curdevice + ")")
				if (isMotionDevice) {
					curdevice?.sendEvent(name: "motion", value: "active")
				}
                else if (isSmokeDevice) {
					curdevice?.sendEvent(name: "smoke", value: "detected")					
				}
                else if (isContactDevice) {
					curdevice?.sendEvent(name: "contact", value: "open")
				}

                // Set bypass status to "bypass"
                curdevice.sendEvent(name:"bypass", value: "bypass")
            
                break

			case "Bypassed - Inactive":
				logDebug("Got Inactive Bypassed zone: " + zoneId + " (" + curdevice + ")")
				if (isMotionDevice) {
					curdevice?.sendEvent(name: "motion", value: "inactive")
				}
				else if (isSmokeDevice) {
					curdevice?.sendEvent(name: "smoke", value: "clear")					
				}
                else if (isContactDevice) {
					curdevice?.sendEvent(name: "contact", value: "closed")
				}

                // Set bypass status to "bypass"
				curdevice.sendEvent(name:"bypass", value: "bypass")
            
                break

			case "Tamper":
				logDebug("Got Tamper zone: " + zoneId + " (" + curdevice + ")")
				// TODO: not yet properly implemented
                // We'll set it to open for now, since at least that gives an indication something is wrong!
				if (isMotionDevice) {
					curdevice?.sendEvent(name: "motion", value: "active")
				}
				else if (isSmokeDevice) {
					curdevice?.sendEvent(name: "smoke", value: "detected")					
				}
                else if (isContactDevice) {
					curdevice?.sendEvent(name: "contact", value: "open")
				}

                // TODO: does this mean bypass should be "clear"?
            
                break

			default:
				logDebug("Unknown zone status received: ${zoneId} is ${zoneStatus}")
				break
		}
	}
}

/* Handle refresh update */
private handleRefresh(stat_str, zones)
{
	// Handle the alarm status
	handleAlarmStatus(stat_str)
	
	// And now handle the status for each zone
	for (def curzone in zones)
	{
		handleZoneStatus(curzone.zone_id, curzone.zone_status)
	}
}

/* Handler for zone list. Builds the (child) zone devices */
private handleCreateZones(zones)
{
    logDebug("Inside handleCreateZones(zones)")
	for (def curzone in zones)
	{
        thiszonename = buildZoneName (curzone.zonename)
        thisdeviceid = buildZoneDeviceId (curzone.zone_id)

        def curzonedevice = null
        try
        {
            curzonedevice = getZoneDevice(thisdeviceid)
        }
        catch (e)
        {
            logDebug("Couldnt find zone device (${thisdeviceid}); probably doesn't exist so safe to add a new one. Error message: ${e}")
        }
				
		// If we don't have a matching child already, and the name isn't Unknown, then we can finally start creating the child device
        if (curzonedevice == null)
        {
            // TODO: what is this for?  Does it come from alarm system bridge?
            if (curzone.zonename != "Unknown")
    		{
	    		try
		    	{
			    	switch (curzone.zonetype)
				    {
          				// If it is a magnetic sensor then add it as a contact sensor
    					case ["Magnet", "Contact", "Entry/Exit"]:
                            try
                            {
    			    			addChildDevice("alarm", "Alarm Virtual Contact Sensor", thisdeviceid, [name: thiszonename, zone_id: curzone.zone_id])
	    			    		logDebug("Created contact zone child device: " + curzone.zone_id + " " + thiszonename)
                            }
                            catch (e)
                            {
                                logDebug("ERROR: Couldn't create contact zone child device (zone ${curzone.zone_id}). Error: ${e}")
                            }
                            break

           				// If it is a motion or interior sensor then add it as a motion detector device
        				case ["Motion", "Interior", "Wired"]:
                            try
                            {
    			    			addChildDevice("alarm", "Alarm Virtual Motion Sensor", thisdeviceid, [name: thiszonename, zone_id: curzone.zone_id])
	    			    		logDebug("Created motion zone child device: " + curzone.zone_id + " " + thiszonename)
                            }
                            catch (e)
                            {
                                logDebug("ERROR: Couldn't create motion zone child device (zone ${curzone.zone_id}). Error: ${e}")
                            }
                            break

    					case ["Smoke", "Fire"]:
                            try
                            {
    			    			addChildDevice("alarm", "Alarm Virtual Smoke Detector", thisdeviceid, [name: thiszonename, zone_id: curzone.zone_id])
	    			    		logDebug("Created contact zone child device: " + curzone.zone_id + " " + thiszonename)
                            }
                            catch (e)
                            {
                                logDebug("ERROR: Couldn't create smoke zone child device (zone ${curzone.zone_id}). Error: ${e}")
                            }
                            break

	    				// Add the remainders as motion detectors for now - unfortunately this will display motion/no-motion instead of active/inactive 
                        // TODO: reconsider. For example, 'Shock' should probably be contact sensor. 
    					case ["Shock", "Vibration", "Gas", "Panic", "KeySwitch"]:
		    			    try
                            {
    			    			addChildDevice("alarm", "Alarm Virtual Motion Sensor", thisdeviceid, [name: thiszonename, zone_id: curzone.zone_id])
	    			    		logDebug("Created motion zone child device: " + curzone.zone_id + " " + thiszonename)
                            }
                            catch (e)
                            {
                                logDebug("ERROR: Couldn't create motion zone child device (zone ${curzone.zone_id}). Error: ${e}")
                            }
                            break

    					default:
                            logDebug("ERROR: Unknown sensor type found (${curzone.zonetype}); cannot create zone child device")
		    				break
			    	}
			    }
			    catch (e)
			    {
				    log.error "Couldnt add device, probably already exists: ${e}"
			    }
            }
            else
            {
                logDebug("Zone name for zone ${curezone.zone_id} is unknown - not adding child device")
            }
		}
        else
        {
            logDebug("Child device named ${thisdeviceid} already exists - not adding child device")
        }
	}
}

/* Execute a command on the alarm bridge device (all functionality implemented via GET requests) */
private getAction(uri)
{ 
    logDebug("Inside getAction(uri=${uri})")

    updateDNI()

	def userpass

    // If password defined, use basic authentication
    if(password != null && password != "") 
		userpass = encodeCredentials("admin", password)
	
	def headers = getHeader(userpass)
  
	def hubAction = new hubitat.device.HubAction(
		method: "GET",
		path: uri,
		headers: headers
		)
    logDebug("About to call the actual hubAction")
	return hubAction	
}

def parseDescriptionAsMap(description)
{
	description.split(",").inject([:])
	{
		map, param ->
		def nameAndValue = param.split(":")
		if (nameAndValue.size() > 1)
			map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private getHeader(userpass = null)
{
	def headers = [:]
	headers.put("Host", getHostAddress())
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	if (userpass != null)
	   headers.put("Authorization", userpass)
	return headers
}

private encodeCredentials(username, password)
{
	def userpassascii = "${username}:${password}"
	def userpass = "Basic " + userpassascii.bytes.encodeBase64().toString()
	return userpass
}

private updateDNI()
{ 
	if (state.dni != null && state.dni != "" && device.deviceNetworkId != state.dni)
	{
	   device.deviceNetworkId = state.dni
	}
}

private getHostAddress()
{
	if(getDeviceDataByName("ip") && getDeviceDataByName("port"))
	{
		return "${getDeviceDataByName("ip")}:${getDeviceDataByName("port")}"
	}
	else
	{
		return "${ip}:80"
	}
}


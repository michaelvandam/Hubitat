/**
 *  Alarm System Interlogix
 *
 *  This Hubitat App provides an interface to an Interlogix alarm system connected via
 *  the Alarm System Bridge developed by LeeF Automation.
 *
 *  The following features are supported:
 *  - Installs alarm bridge driver device
 *  - Provides Virtual Switch (momentary) devices to control standard functions (Arm Home, Arm Away, Disarm, Panic)
 *  - Supports notifications of Alarm, Panic, Arm Success, and Disarm Success
 *  - Supports bypassing of zones
 *
 *  The following are planned for the future:
 *  - Consistent use of exception handling
 *  - Improve the SmartApp GUI to integrate more of setup all in one place (create bridge device, configuration settings,
 *      add zones, configure and add switches)
 *  - Improve the SmartApp GUI to provide more control over zones (e.g. renaming, changing type, bypassing/clearing)
 *  - Improve the SmartApp GUI to provide more functional operating interface and zone view
 *  - Improve the SmartApp GUI to provide features like rebooting and refreshing the bridge device
 *  - Add feature to notify of arm home or away
 *  - Integrate with TileMaster or SuperTile or etc... to streamline the setup process
 *  - Integrate better with HSM (e.g. maybe a single virtual sensor can be monitored by HSM)
 *  - Add installation feature that cleans up various devices
 * 
 *  Copyright 2019 R. Michael van Dam
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * UPDATE HISTORY:
 *  2019-12-20 - R. Michael van Dam - Initial design and release
 *  2019-08-02 - LeeF Automation - Original SmartThings version
 * 
 * CREDITS:
 *  This code was heavily modified from LeeF Automation's SmartThings version, including dividing it into
 *  a separate Driver and SmartApp.
 */


definition(
    name: "Alarm System Interlogix",
    namespace: "alarm",
    author: "R. Michael van Dam",
    description: "Provides interface to Interlogix alarm systems equipped with network bridge from LeeF Automation",
    category: "Security",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true) { }

preferences {
    page (name: "page_main")
    page (name: "page_config")
    page (name: "page_install_driver")
    page (name: "page_create_zones")
    page (name: "page_remove_zones")
    page (name: "page_create_switches")
    page (name: "page_remove_switches")
}

// ------------------------------------------------------------------------------------------------------------------------
// APP PAGES
// ------------------------------------------------------------------------------------------------------------------------

/* Main page */
def page_main() {

/*
    // see if we are already installed
    def foundMsg = ""
    def mainDevice = getChildDevice("${state.ip}")
    if (mainDevice) foundMsg = "**** AlarmDecoder already installed ****"
*/
    
    dynamicPage(name: "page_main", title: "Main page", refreshInterval: 1) {
        section("Configure settings") {
            href("page_config", required: false, title: "Configuration page", description: "")
        }
        section("Operate System") {
            section("Alarm panel status") {
                paragraph "Panel status: " + getStatusText(state.status)
            }
            // TODO:
			// Show bypassed zones?
            // Show buttons for arm, disarm
            // Show alarm status (refreshed)
        }
    }
}

def page_config() {
    dynamicPage(name: "page_config", title: "Configuration page", install: true) {
        section("Debugging options") {
            input "debug_mode", "bool", title: "Enable debug logging?", required: false, defaultValue: false, submitOnChange: true
        }
        section("Install driver") {
            // TODO: figure out how to add an existing device (and change network id) and then add as child device to the app
            // Currently, you must create the device from here
            input "alarm_bridge_dni", "string", required: true, defaultValue: "4E6574777278", submitOnChange: true, title: "Driver device network id (do not change)"
            href("page_install_driver", required: false, title: "Create driver device", description: "")
            // TODO: in future, put all preferences here
            paragraph "After the device is created, navigate to the Device via the Hubitat interface, and set the preferences"
        }
        section("Configure Alarm Zones") {
            href("page_remove_zones", required: false, title: "Remove all alarm zone devices", description: "")
            href("page_update_zones", required: false, title: "Add / update all alarm zone devices", description: "")
        }
        section("Configure Alarm Switches") {
            paragraph "If you are changing these values, make sure to remove the switch devices first, then make changes, then re-create the switch devices"
            // TODO: in the future do some error checking to make dummy proof
            input "switch_dni_prefix", "string", title: "Prefix for network ID of switch devices. 'alarm_switch_' would give 'alarm_switch_disarm', etc.", required: true, defaultValue: "alarm_switch_", submitOnChange: true
            href("page_create_switches", required: false, title: "Create switch devices", description: "")
            href("page_remove_switches", required: false, title: "Remove switch devices", description: "")
        }
        section("Configure Notifications") {
            input "notification_devices", "capability.notification", multiple: true, title: "Select devices to notify (alarms, arm failures, etc.)", required:false, submitOnChange: true
        }
    }
}

def page_install_driver() {
    dynamicPage(name: "page_install_driver") {
        section("") {
            def bridge = addChildDevice("alarm", "Alarm System Driver Interlogix", settings.alarm_bridge_dni)
        } 
    }
}
                 
def page_remove_zones() {
    dynamicPage(name: "page_remove_zones") {
        section("") { paragraph "Not yet implemented. You can remove all the zones from the Hubitat Driver interface." }
    }
}

def page_update_zones() {
    dynamicPage(name: "page_update_zones") {
        section("") { paragraph "Not yet implmeneted. You can add all the zones from the Hubitat Driver interface." }
    }
}

def page_create_switches() {
    dynamicPage(name: "page_create_switches") {
        createSwitchDevices()
    }
}

def page_remove_switches() {
    dynamicPage(name: "page_remove_switches") {
        removeSwitchDevices()
    }
}


// ------------------------------------------------------------------------------------------------------------------------
// UTILITY FUNCTIONS
// ------------------------------------------------------------------------------------------------------------------------

/* Add a message to the debugging log. Prepends a prefix for searching. */
private logDebug(message)
{
    def prefix = "Alarm System Interface - "
	if (settings?.debug_mode) {
        log.debug "$prefix$message"
    }
}

/* Return text to display for each status value */
// TODO: CURRENTLY UNUSED.  DELETE?
def getStatusText(status)
{
    switch(status)
    {
        case ["disarmed"]:    return "Disarmed (Ready)"
        case ["notready"]:    return "Disarmed (Not Ready)"
        case ["exitdelay"]:   return "Exit Delay"
        case ["away"]:        return "Armed Away"
        case ["home"]:        return "Armed Home"
        case ["alarm"]:       reuturn "Alarm!"
        default:
            logDebug("getStatusText(s): unknown status value (${status})")
            break
    }
}

/* Send a notification message to devices in settings.notification_devices */
private sendNotification(message)
{
    settings.notification_devices?.each { device ->
        device.deviceNotification(message)
    }
}

/* Build the DNI for a switch based on settings.switch_dni_prefix */
private buildSwitchDeviceId(raw_id)
{
    return settings.switch_dni_prefix + raw_id
}

// TODO: implement creation of arm/disarm/panic switches
def createSwitchDevices()
{
    def switches = ["disarm", "armhome", "armaway", "panic", "armhome_bypass", "armaway_bypass"]
    switches.each {
        // Add a way to give better names?
        def switch_name = "Alarm Switch " + it.value
        def switch_device_id = buildSwitchDeviceId(it.value)
        try {
            // TODO: I'm not able to set the "name" and other parameters (gives no method signature error)
            // TODO: figure out how to set the attributes of the device
            def child = addChildDevice("hubitat", "Virtual Switch", switch_device_id) //, [name: "${switch_name}"]
            child.updateSetting("autoOff", "1s") // DOESN'T WORK - what are the name and allowed values of the setting?
            // Note, set 1sec for switch; the 500 msec doesn't seem to work well with DashBoard
            child.Off()
        }
        catch (e) {
            logDebug ("Error while creating switch device (${it.value}): $e")
        }
    }
}

// TODO: delete the arm/disarm/panic switch devices
def removeSwitchDevices()
{
    def switches = ["disarm", "armhome", "armaway", "panic", "armhome_bypass", "armaway_bypass"]
    switches.each {
        switch_device_id = buildSwitchDeviceId(it.value)
        try {
            logDebug("Deleting child device ($switch_device_id)")
            deleteChildDevice(switch_device_id)
        }
        catch (e) {
            logDebug("Error deleting switch device (${it.value}): $e")
        }
    }
}


// ------------------------------------------------------------------------------------------------------------------------
// EVENT SUBSCRIPTIONS
// ------------------------------------------------------------------------------------------------------------------------

/* Subscribe to alarm bridge events (status) */
private subscribeBridgeEvents()
{
    bridge = getChildDevice(settings.alarm_bridge_dni)
    subscribe(bridge, "status", handleAlarmStatus)
}


// TODO: How to get the zone number to the handler?  Is it part of the 'event'?
/* Subscribe to zone events (bypass, contact, motion, smoke) */
private subscribeZoneEvents()
{
    bridge = getChildDevice(settings.alarm_bridge_dni)
    zones = bridge.getZoneDevices()
    zones.each { zone ->
        // Currently no handler for bypass events. Uncomment if need to implement
        // subscribe(zone, "bypass", handleBypass)
        if (zone.hasCapability("ContactSensor")) {
            subscribe(zone, "contact", handleContact)
        }
        if (zone.hasCapability("MotionSensor")) {
            subscribe(zone, "motion", handleMotion)
        }
        if (zone.hasCapability("SmokeDetector")) {
            subscribe(zone, "smoke", handleSmoke)
        }
    }

}

/* Subscribe to switch events (disarm, armhome, armaway, panic) */
private subscribeSwitchEvents()
{
    device = getChildDevice(buildSwitchDeviceId("disarm"))
    subscribe(device, "switch", handleSwitchDisarm)

    device = getChildDevice(buildSwitchDeviceId("armhome"))
    subscribe(device, "switch", handleSwitchHome)

    device = getChildDevice(buildSwitchDeviceId("armaway"))
    subscribe(device, "switch", handleSwitchAway)

    device = getChildDevice(buildSwitchDeviceId("panic"))
    subscribe(device, "switch", handleSwitchPanic)
    
    device = getChildDevice(buildSwitchDeviceId("armhome_bypass"))
    subscribe(device, "switch", handleSwitchHomeBypass)

    device = getChildDevice(buildSwitchDeviceId("armaway_bypass"))
    subscribe(device, "switch", handleSwitchAwayBypass)
}


// ------------------------------------------------------------------------------------------------------------------------
// EVENT HANDLERS
// ------------------------------------------------------------------------------------------------------------------------

def handleAlarmStatus(evt) {
    //logDebug("Inside handleAlarmStatus(${evt})")
    switch (evt.value) {
        case "disarmed":
            state.status = "disarmed"
            state.alarm_active = false
            // NOTE: we don't disable state.panic_active in case it was used during Disarm state
            logDebug("'disarmed' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            if (state.attempt_armhome || state.attempt_armaway) {
                // Arming failed?
                //state.attempt_armhome = false
                //state.attempt_armaway = false
                // System does NOT report this status if open zone during arm. Need another way to detect arming failures
                // sendNotification("Alarm: ARM FAILED " + evt.getDate()) // Find way to list open zones?
            }
            if (state.attempt_disarm) {
                // Disarm successful
                state.attempt_disarm = false
                sendNotification("Alarm: DISARMED " + evt.getDate())
            }
            // Notify HSM to update status
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
			// Update laststatus
			state.laststatus = state.status
            break
        
        case "notready":
            state.status = "notready"
            state.alarm_active = false
            // NOTE: we don't disable state.panic_active in case it was used during Disarm state
            logDebug("'notready' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            if (state.attempt_armhome || state.attempt_armaway) {
                // Arming failed?
                //state.attempt_armhome = false
                //state.attempt_armaway = false
                // System does NOT report this status if open zone during arm. Need another way to detect arming failures
                // sendNotification("Alarm: ARM FAILED " + evt.getDate()) // Find way to list open zones?
            }
            if (state.attempt_disarm) {
                // Disarm success
                state.attempt_disarm = false
                sendNotification("Alarm: DISARMED " + evt.getDate())
            }
            // Notify HSM to update status
            sendLocationEvent(name: "hsmSetArm", value: "disarm")
			// Update laststatus
			state.laststatus = state.status
            break

        case "armaway":
            state.status = "armaway"
            logDebug("'armaway' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            if (state.attempt_armhome) {
                // Wrong arming status found
                logDebug("ERROR: Was attempting to arm home, but arm away status found")
                state.attempt_armhome = false
                sendNotification("Alarm: ARMED(AWAY) " + evt.getDate())
            }
            if (state.attempt_armaway) {
                // Arming success
                state.attempt_armaway = false
                sendNotification("Alarm: ARMED(AWAY) " + evt.getDate())
            }
            if (state.attempt_disarm) {
                // Disarm failure
                logDebug("ERROR: Found armed away status while disarming")
                // TODO: for now, wait for the next status... check logs if issues are happening
                // TODO: maybe retry?
                // runIn (1,....) // Need a delay since relay toggle lasts for a significant amount of time
            }
            // Notify HSM to update status
            sendLocationEvent(name:"hsmSetArm", value:"armAway")
			// Update laststatus
			state.laststatus = state.status
            break
        
        case "armhome":
            state.status = "armhome"
            logDebug("'armhome' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            if (state.attempt_armhome) {
                // Arming success
                state.attempt_armhome = false                
                sendNotification("Alarm: ARMED(HOME) " + evt.getDate())
            }
            if (state.attempt_armaway) {
                // Wrong arming status
                logDebug("ERROR: Was attempting to arm away, but arm home status found")
                state.attempt_armaway = false
                sendNotification("Alarm: ARMED(HOME) " + evt.getDate())
            }
            if (state.attempt_disarm) {
                // Disarm failure
                logDebug("ERROR: Found armed home status while disarming")
                // TODO: for now, wait for he next status... check logs if issues are happening
                // TODO: maybe retry?
                // runIn (1,....) // Need a delay since relay toggle lasts for a significant amount of time
            }
            // Notify HSM to update status
            sendLocationEvent(name:"hsmSetArm", value:"armHome")
			// Update laststatus
			state.laststatus = state.status
            break
        
        case "exitdelay":
            state.status = "exitdelay"
            logDebug("'exitdelay' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            if (state.attempt_disarm) {
                // Disarm failure
                logDebug("ERROR: Found exit delay status while disarming")
                // TODO: for now, wait for he next status... check logs if issues are happening
                // TODO: maybe retry?
                // runIn (1,....) // Need a delay since relay toggle lasts for a significant amount of time
            }
            // TODO: some kind of event to send to HSM for arming delays?
			// Update laststatus
			state.laststatus = state.status
            break
        
        case "alarm":
            state.status = "alarm"
            state.alarm_active = true
            // NOTE: this is a momentary status and will revert to normal armed or disarmed status soon
            // NOTE: if using zone triggering with zone-type of 24-hour audible, and it is triggered during
            //       disarmed state, this can be confusing, as panel will next show disarmed or notready status.
            logDebug("'alarm' status found (attempt_armhome=${state.attempt_armhome}, attempt_armaway=${state.attempt_armaway}, attempt_disarm=${state.attempt_disarm})")
            sendNotification("Alarm: ALARMED! " + evt.getDate())
            // TODO: anything to do if "attempt_disarm" or other flags are set?
            // TODO: how to properly notify HSM?
			// Update laststatus
			state.laststatus = state.status
            break

        default:
            logDebug("ERROR: Unknown status received from panel ($evt.value)")
            break
    }
}
        
/* Handle Disarm switch press. Disarm the system. */
def handleSwitchDisarm(evt)
{
    logDebug("Inside handleSwitchDisarm(${evt})")
    if (evt.value == "on")
    {
        // TODO: add error checking if can't find device
        device = getChildDevice(settings.alarm_bridge_dni)
	
	    // NOTE: Technically we would only want to try disarming if the status is either 'home', 'away',
	    // 'exitdelay' or 'alarm'. However, if using a "24 hour audible" zone type for AlarmTrigger(),
	    // then alarm could be sounding while status appears to be disarmed. (The status only reports
	    // 'alarm' momentarily.) Thus we attempt to disarm regardless of the status. 
	    // NOTE: If the bridge device toggles the disarm relay while the system is already disarmed,
	    // then the alarm system will attempt to arm away. Thus, we set a flag to indicate the disarm
	    // attempt, and retry disarming one time if we don't find the 'disarmed' or 'notready' status. 

        // Do we need an event here?
        state.attempt_disarm = true
    
	    // If in panic mode, turn off panic mode
        // LIMITATION: this will turn off panic mode only if done from the dashboard.
    	// If the panic alarm is turned off from the keypad, we won't know about it.
        state.panic_active = false
        
        device.alarmDisarm()
    }           
}

/* Handle ArmHome switch press. Arm the system. */
def handleSwitchHome(evt)
{
    logDebug("Inside handleSwitchHome(${evt})")
    if (evt.value == "on")
    {
        device = getChildDevice(settings.alarm_bridge_dni)

    	// Only attempt to arm if currently disarmed
        if ((state.status == "disarmed") || (state.status == "notready"))
    	{
            // NOTE: we don't have any exit delay as we rely on what's programmed in the panel
            state.attempt_armhome = true
        	device.alarmArmHome()
        }
        else
    	{
	    	logDebug("Ignoring Arm Home request (system not currently disarmed)")
    	}
    }
}

/* Handle ArmAway switch press. Arm the system. */
def handleSwitchAway(evt)
{
    logDebug("Inside handleSwitchAway(${evt})")
    if (evt.value == "on")
    {
        device = getChildDevice(settings.alarm_bridge_dni)

        // Only attempt to arm if currently disarmed
        if ((state.status == "disarmed") || (state.status == "notready"))
	    {
            // NOTE: we don't have any exit delay as we rely on what's programmed in the panel
            state.attempt_armaway = true
    	    device.alarmArmAway()
        }
        else
	    {
		    logDebug("Ignoring Arm Away request (system not currently disarmed)")
	    }
    }
}

/* Handle Panic switch press. Trigger the panic function. */
def handleSwitchPanic(evt)
{
    logDebug("Inside handleSwitchPanic(${evt})")
    if (evt.value == "on" /*&& !state.panic_active*/)    // NOTE: we alert each time panic is pressed even if active
    {
        state.panic_active = true
        device = getChildDevice(settings.alarm_bridge_dni)
        device.alarmPanic()
        // TODO: set up a way to customize this and other notification strings
        sendNotification("Alarm: PANIC " + evt.getDate())
    }
}

/* Handle ArmHome with Bypass switch press. Arm the system. */
def handleSwitchHomeBypass(evt)
{
    logDebug("Inside handleSwitchHomeBypass(${evt})")
    if (evt.value == "on")
    {
        device = getChildDevice(settings.alarm_bridge_dni)

    	// Only attempt to arm if currently disarmed
        if ((state.status == "disarmed") || (state.status == "notready"))
    	{
            // NOTE: we don't have any exit delay as we rely on what's programmed in the panel
            state.attempt_armhome = true
        	device.alarmArmHomeBypass()
        }
        else
    	{
	    	logDebug("Ignoring Arm Home Bypass request (system not currently disarmed)")
    	}
    }
}

/* Handle ArmAway with Bypass switch press. Arm the system. */
def handleSwitchAwayBypass(evt)
{
    logDebug("Inside handleSwitchAwayBypass(${evt})")
    if (evt.value == "on")
    {
        device = getChildDevice(settings.alarm_bridge_dni)

        // Only attempt to arm if currently disarmed
        if ((state.status == "disarmed") || (state.status == "notready"))
	    {
            // NOTE: we don't have any exit delay as we rely on what's programmed in the panel
            state.attempt_armaway = true
    	    device.alarmArmAwayBypass()
        }
        else
	    {
		    logDebug("Ignoring Arm Away Bypass request (system not currently disarmed)")
	    }
    }
}



/* Handle contact open event.
   Report intrusion if zone is open, bypass is clear, and panel status is armhome or armaway or alarm
*/
// QUESTION: would it be easier just to not subscribe to relevant events when armed or not?
// QUESTION: do we want to report events only when "alarm_active" is true?  Maybe, but I'm worried about missing the first event
// TODO: we should also respect EXIT DELAY. When arming home, our panel immediately goes to "ARM HOME" state, but won't report
// alarm states until after some delay.
def handleContact(evt)
{
    logDebug("Inside handleContactEvent(${evt}). Current panel status: ${state.status}")
    device = evt.getDevice()
    if ((evt.value == "open") && (device.currentValue("bypass") == "clear") && ((state.status == "armhome" || state.status == "armaway" || state.status == "alarm"))) {
        // Intrusion detected
        sendNotification("Alarm INTRUSION (" + device.getName() + ") " + evt.getDate())
    }
}

/* Handle motion detection event.
   Report intrusion if motion was detected, bypass is clear, and panel status is armhome or armaway or alarm
*/
def handleMotion(evt)
{
    logDebug("Inside handleMotionEvent(${evt}). Current panel status: ${state.status}")
    device = evt.getDevice()
    if ((evt.value == "motion") && (device.currentValue("bypass") == "clear") && ((state.status == "armhome" || state.status == "armaway" || state.status == "alarm"))) {
        // Intrusion detected
        sendNotification("Alarm INTRUSION (" + device.getName() + ") " + evt.getDate())
    }
}

/* Handle smoke detection event. */
def handleSmoke(evt)
{
    logDebug("Inside handleSmokeEvent(${evt}). Current panel status: ${state.status}")
    device = evt.getDevice()
    if ((evt.value == "smoke") && (device.currentValue("bypass") == "clear")) {
        // Smoke detected
        sendNotification("Alarm SMOKE (" + device.getName() + ") " + evt.getDate())
    }
}

/* Handle zone bypass events */
def handleBypass(evt)
{
    // For now we do nothing
}


// ------------------------------------------------------------------------------------------------------------------------
// STANDARD CALLBACKS
// ------------------------------------------------------------------------------------------------------------------------

def installed() {
    logDebug("Inside installed(). Installed with settings: ${settings}")
    initialize()
}

def updated() {
    logDebug("Inside updated(). Updated with settings: ${settings}")
    initialize()
}

def uninstalled() {
    logDebug("Inside uninstalled()")

    // Unsubscribe from all events and remove all schedule
    unsubscribe()
    unschedule()

    // Remove all devices and children
    logDebug("ERROR: removal of devices during uninstalled not yet implemented")
}

def initialize() {
    logDebug("Inside initialize()")

    // Unsubscribe from all events and remove all schedules
    unsubscribe()
    unschedule()

    // Initialize state
    state.status = getChildDevice(settings.alarm_bridge_dni).currentValue("status")
	state.laststatus = null
    // NOTE: There is a remote possibility these could be out of sync briefly if the app
    // is initiatilzed during arming/panic/alarm state
    state.attempt_armaway = false
    state.attempt_armhome = false
    state.attempt_disarm = false
    state.panic_active = false 
    state.alarm_active = false

    // Subscribe to all events
    subscribeBridgeEvents()
    subscribeZoneEvents() 
    subscribeSwitchEvents()
    // subscribe(location, "hsmAlert", handleHSMAlert)
    // subscribe(location, "hsmStatus", handleHSMStatus)
}

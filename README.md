<h1>Networx Alarm System Bridge for Hubitat</h1>
<h2>Overview</h2>
<p> This code is used to operate the NetworX Alarm System Bridge (developed by LeeF Automation) on a Hubitat Elevate home automation hub. The bridge is a small WiFi-enabled device that allows your Interlogix NetworX alarm system to communicate with your hub. The hub code was originally written for the SmartThings hub (by LeeF Automation). Here, I've restructured the code (and added new features) to run on Hubitat. <p> This is still a work in progress and there is room for improvement, but hopefully it can help to get you up and running.
<h2>Installation</h2>
<h3>Requirements</h3>
<ul>
<li>Networx Alarm System Bridge, with optional disarm kit
<li>Firmware version: NetworxAlarmSystemBridge-0.02.04-beta2-withBypass.bin
</ul>
<p>You can contact LeeF automation to obtain the bridge here: https://community.smartthings.com/t/ge-networx-interlogix-caddx-nx8-security-system-integration/48182/41
<h3>Procedure</h3>
<ul>
<li> Set up the Interlogix Alarm System Bridge, as per instructions from LeeF Automation
<li> Log into to you hub
<li> Install the App: Alarm Bridge App
<li> Install the Driver: Alarm Bridge Driver
<li> Install the Drivers: Alarm Virtual Contact Sensor, Alarm Virtual Motion Sensor, Alarm Virtual Smoke Detector
<li> Go to Apps | Click on "Alarm System Interlogix"
<li> Go to Configuration Page
<li> Click "Create Bridge Device" (don't change the device network ID)
<li> Go to Devices | Select the "Alarm System Interlogix Driver" device
<li> Configure all of the preferences. Save preferences.
<li> Click on "createZoneDevices"
<li> Go to Devices and check that all of your zones have been created
<li> Go to Apps | Click on "Alarm System Interlogix"
<li> Go to Configuration Page
<li> Skip the zone devices section
<li> Enter a prefix for the switches
<li> Click on create switches
<li> Configure notification options (select devices to send notifications to)
<li> Select "OK"
<li> Go to Devices
<li> For each of the new switches (six):
<ul>
<li> Click on the name of the switch
<li> Change preference to automatically turn off after 1s. Save preferences
<li> Change "name" if desired and click Save device
</ul>
<li> Configure your dashboards.  I have one dashboard showing the state of all the alarm zone devices. I have another dashboard with the desired switches I want to use (and I also have a tile for the Alarm System Interlogix Driver templated as HSM to show the status of the alarm).
</ul>
<h2>Limitations</h2>
<ul>
<li>Installation and app are clunky for now. This will hopefully improve in the future.
<li>The system cannot be controlled from the HSM.  It is one-way communication to the HSM to show status.  Note that you have to configure the HSM to monitor at least one zone for it to operate. I typically set it to monitor the "disarm" zone.
</ul>
<h2>Feedback</h2>
If you find this useful, and/or have questions or suggestions, I'd be happy to hear from you!

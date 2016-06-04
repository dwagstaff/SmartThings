/**
 *  Openhab Light
 *
 *  Copyright 2016 David Wagstaff
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
 */
metadata {
	definition (name: "Openhab Light", namespace: "dwagstaff", author: "David Wagstaff") {

    	capability "Actuator"
        capability "Configuration"
        capability "Refresh"
		capability "Sensor"
        capability "Switch"
		capability "Switch Level"
        capability "Polling"
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821", nextState:"turningOff"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState:"turningOn"
 			state "turningOn", label:'${name}', action: "switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
            state "turningOff", label:'${name}', action: "switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false, range:"(0..100)") {
			state "level", action:"switch level.setLevel"
		}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label: 'Level ${currentValue}%'
		}

		main(["switch"])
		details(["switch", "level", "levelSliderControl", "refresh"])
	}

	    preferences {

        	input("dimRate", "enum", title: "Dim Rate", options: ["Instant", "Normal", "Slow", "Very Slow"], defaultValue: "Normal", required: false, displayDuringSetup: true)
            input("dimOnOff", "enum", title: "Dim transition for On/Off commands?", options: ["Yes", "No"], defaultValue: "No", required: false, displayDuringSetup: true)

    }
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Doing parse"
	log.trace description

}

def poll() {
}

def updated() {
}

def on() {
	state.lvl = "00"
    state.trigger = "on/off"
    log.debug "Turning On"
    try {
        def result = new physicalgraph.device.HubAction(
            method: "GET",
            path: "/index.html",
            headers: [
                HOST: "192.168.1.39:80"
            ]
    //        query: [param1: "value1", param2: "value2"]
        )
	    log.debug result
    	return result
    }
    catch (Exception e) 
    {
    	log.debug "Hit Exception on $hubAction"
    	log.debug e
    }
}

def off() {
	state.lvl = "00"
    state.trigger = "on/off"

    // log.debug "off()"
}

def refresh() {
	log.debug "Refresh invoked" 
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/rest",
        headers: [
            HOST: "192.168.1.104"
        ],
        query: [param1: "value1", param2: "value2"]
    )
    return result
}

def setLevel(value) {

    state.trigger = "setLevel"
    state.lvl = "${level}"
}

def configure() {

	log.debug "Configuring Reporting and Bindings."
	def configCmds = [

        //Switch Reporting
        "zcl global send-me-a-report 6 0 0x10 0 3600 {01}", "delay 500",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1000",

        //Level Control Reporting
        "zcl global send-me-a-report 8 0 0x20 5 3600 {0010}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 1", "delay 1500",

        "zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}", "delay 1000",
		"zdo bind 0x${device.deviceNetworkId} 1 1 8 {${device.zigbeeId}} {}", "delay 500",
	]
    return configCmds + refresh() // send refresh cmds as part of config
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

def getStatus() {
  log.debug "Executing 'getStatus'"
def today= new Date()
log.debug "https://mytotalconnectcomfort.com/portal/Device/CheckDataSession/${settings.honeywelldevice}?_=$today.time"



    def params = [
        uri: "http://192.168.1.104:8889/rest",
        headers: [
              'Accept': '*/*',
              'DNT': '1',
              'Accept-Encoding': 'plain',
              'Cache-Control': 'max-age=0',
              'Accept-Language': 'en-US,en,q=0.8',
              'Connection': 'keep-alive',
              'Host': '192.168.1.104',
              'Referer': 'https://mytotalconnectcomfort.com/portal',
              'X-Requested-With': 'XMLHttpRequest',
              'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36'
        ]
     ]

        httpGet(params) { response ->
        log.debug "Request was successful, $response.status"

        
//        def curTemp = response.data.latestData.uiData.DispTemperature
//        log.trace("IndoorHumidity: ${response.data.latestData.uiData.IndoorHumidity}")

	//Send events 
//        sendEvent(name: 'thermostatOperatingState', value: operatingState)
//        sendEvent(name: 'thermostatFanMode', value: fanMode)
//        sendEvent(name: 'thermostatMode', value: switchPos)
//        sendEvent(name: 'coolingSetpoint', value: coolSetPoint as Integer)
//        sendEvent(name: 'heatingSetpoint', value: heatSetPoint as Integer)
//        sendEvent(name: 'temperature', value: curTemp as Integer, state: switchPos)
//        sendEvent(name: 'relativeHumidity', value: curHumidity as Integer)
    }

}

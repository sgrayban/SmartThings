/**
 *  Shelly as Switch Device Handler
 *
 *  Copyright 2019 Scott Grayban
 *
 *  Licensed under the GNU GENERAL PUBLIC LICENSE version 3 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      https://www.gnu.org/licenses/gpl-3.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *
 * Shelly http POST at http://IP/relay/0 ( can be 0/1 for shelly 2) with body form-urlencoded:
 *   turn=on
 *   turn=off
 *   ison=boolean
 *
 */

metadata {
	definition (name: "Shelly as Switch", namespace: "sgrayban", author: "Scott Grayban") {
	capability "Actuator"
	capability "Sensor"
        capability "Refresh" // refresh command
        capability "Switch"
	}

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOn", defaultState: true
                attributeState "turningOn", label:'Turning On', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOn"
                attributeState "turningOff", label:'Turning Off', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOff"
            }
        }

        standardTile("explicitOn", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "On", action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }
        standardTile("explicitOff", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Off", action: "switch.off", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }

        standardTile("refresh", "device.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh", nextState: "disabled"
            state "disabled", label:'', action:"", icon:"st.secondary.refresh"
        }

        main(["switch"])
        details(["switch", "explicitOn", "explicitOff","refresh"])

    }


    preferences {
        input("ip", "string", title:"IP", description:"Shelly IP Address", defaultValue:"" , required: false, displayDuringSetup: true)
        input("channel", "string", title:"Channel", description:"Channel (only for Shelly2):", defaultValue:"0" , required: false, displayDuringSetup: true)
    }

}

def getCheckInterval() {
    log.debug "getCheckInterval"
    return 4 * 60 * 60
}

def installed() {
    log.debug "Installed"
    sendEvent(name: "checkInterval", value: checkInterval, displayed: false)
    refresh()
}

def updated() {
    log.debug "Updated"
    if (device.latestValue("checkInterval") != checkInterval) {
        sendEvent(name: "checkInterval", value: checkInterval, displayed: false)
    }
    refresh()
}

def parse(description) {
    log.debug "Parsing result $description"
    
    def msg = parseLanMessage(description)
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
    
    log.debug "Data"
    log.debug data
    
    def evt1 = null
    if ( data.ison == true ) {
        log.debug "CreateEvent ison=true"
        evt1 = createEvent(name: "switch", value: "on", displayed: false)
    } else  {
        log.debug "CreateEvent ison=false"
        evt1 = createEvent(name: "switch", value: "off", displayed: false)
    }

    return evt1
}

//switch.on
def on() {
    log.debug "Executing switch.on"
    sendSwitchCommand "turn=on"
}

//switch.off
def off() {
    log.debug "Executing switch.off"
    sendSwitchCommand "turn=off"
}

def ping() {
    log.debug "Ping"
    refresh()
}

def refresh() {
    log.debug "Refresh - Getting Status"
    sendHubCommand(new physicalgraph.device.HubAction(
      method: "GET",
      path: "/relay/" + channel,
      headers: [
        HOST: getShellyAddress(),
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
}

def sendSwitchCommand(action) {
    log.debug "Calling /relay/ with $action"
    sendHubCommand(new physicalgraph.device.HubAction(
      method: "POST",
      path: "/relay/" + channel,
      body: action,
      headers: [
        HOST: getShellyAddress(),
        "Content-Type": "application/x-www-form-urlencoded"
      ]
    ))
    runIn(25, refresh)
}

private getShellyAddress() {
    def port = 80
    def iphex = ip.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
    def porthex = String.format('%04x', port.toInteger())
    def shellyAddress = iphex + ":" + porthex
    log.debug "Using IP " + ip + ", PORT 80 and HEX ADDRESS " + shellyAddress + " for device: ${device.id}"
    return shellyAddress
}

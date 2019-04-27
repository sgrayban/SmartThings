/**
 *  Hubitat Bridge
 *
 *  Author: sgrayban
 */
// for the UI
metadata {
        // Automatically generated. Make future change here.
        definition (name: "Hubitat Bridge", namespace: "sgrayban", author: "sgrayban") {
                capability "Bridge"
                capability "Health Check"

                attribute "networkAddress", "string"
                // Used to indicate if bridge is reachable or not, i.e. is the bridge connected to the network
                // Possible values "Online" or "Offline"
                attribute "status", "string"
                // Id is the number on the back of the hub, Hue uses last six digits of Mac address
                // This is also used in the Hue application as ID
                attribute "idNumber", "string"
        }

        simulator {
                // TODO: define status and reply messages here
        }

        tiles(scale: 2) {
        multiAttributeTile(name: "rich-control", type: "generic", width: 6, height: 4, canChangeIcon: true) {
                tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
                        attributeState "Offline", label: '${currentValue}', action: "", icon: "st.Lighting.light99-hue", backgroundColor: "#ff0000"
                        attributeState "Online", label: '${currentValue}', action: "", icon: "st.Lighting.light99-hue", backgroundColor: "#01ba01"
                        }
                        }
                valueTile("doNotRemove", "v", decoration: "flat", height: 2, width: 6, inactiveLabel: false) {
                        state "default", label:'If removed, Hubitat will not work properly'
                }
                valueTile("idNumber", "device.idNumber", decoration: "flat", height: 2, width: 6, inactiveLabel: false) {
                        state "default", label:'ID: 1A0932'
                }
                valueTile("networkAddress", "device.networkAddress", decoration: "flat", height: 2, width: 6, inactiveLabel: false) {
                        state "default", label:'IP: ${currentValue}'
                }

                main (["rich-control"])
                details(["rich-control", "doNotRemove", "idNumber", "networkAddress"])
        }
}

def initialize() {
        sendEvent(name: "DeviceWatch-Enroll", value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}", displayed: false)
}

void installed() {
        log.debug "installed()"
        initialize()
}

def updated() {
        log.debug "updated()"
        initialize()
}

// parse events into attributes
def parse(description) {
        log.debug "Parsing '${description}'"
        def results = []
        def result = parent.parse(this, description)
        if (result instanceof physicalgraph.device.HubAction){
                log.trace "Hubitat BRIDGE HubAction received -- DOES THIS EVER HAPPEN?"
                results << result
        } else if (description == "updated") {
                //do nothing
                log.trace "Hubitat BRIDGE was updated"
        } else {
                def map = description
                if (description instanceof String)  {
                        map = stringToMap(description)
                }
                if (map?.name && map?.value) {
                        log.trace "Hubitat BRIDGE, GENERATING EVENT: $map.name: $map.value"
                        results << createEvent(name: "${map.name}", value: "${map.value}")
                } else {
                        log.trace "Parsing description"
                        def msg = parseLanMessage(description)
                        if (msg.body) {
                                def contentType = msg.headers["Content-Type"]
                                if (contentType?.contains("json")) {
                                        def bulbs = new groovy.json.JsonSlurper().parseText(msg.body)
                                        if (bulbs.state) {
                                                log.info "Bridge response: $msg.body"
                                        }
                                } else if (contentType?.contains("xml")) {
                                        log.debug "Hubitat BRIDGE ALREADY PRESENT"
                                        parent.hubVerification(device.hub.id, msg.body)
                                }
                        }
                }
        }
        results
}

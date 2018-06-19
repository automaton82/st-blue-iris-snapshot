/**
 *  Blue Iris Snapshot. Takes the 'index' photo (all the camera images) and shows them.
 **/
metadata {
	definition (name: "Blue Iris Snapshot", namespace: "automaton", author: "automaton") {
        capability "Image Capture"
	}

    preferences {
        input("BlueIrisIP", "string", title:"Blue Iris IP Address", description: "Enter Blue Iris's IP Address",required: true, displayDuringSetup: true)
        input("BlueIrisPort", "string", title:"Blue Iris Port", description: "Enter Blue Iris's Port", defaultValue: 80 , required: true, displayDuringSetup: true)
        input("BlueIrisUser", "string", title:"Blur Iris User", description: "Enter Blue Iris's username", required: false, displayDuringSetup: true)
        input("BlueIrisPassword", "string", title:"Blue Iris Password", description: "Enter Blue Iris's password", required: false, displayDuringSetup: true)
        input("ImageQuality", "string", title:"Image Quality", description: "Enter the image quality (0-100)", defaultValue: 80, required: false, displayDuringSetup: true)
        input("ImageScale", "string", title:"Image Scale", description: "Enter the image scale size (0-100)", defaultValue: 80, required: false, displayDuringSetup: true)
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {        
        standardTile("take", "device.image", width: 6, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.dlink-indoor", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.dlink-indoor", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }
        main "take"
        details([ "cameraDetails", "take"])
	}
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def parse(String description) {
	log.debug "parse $description"
    
     def map = stringToMap(description)

    if (map.tempImageKey) {
        try {
			def pictureName = getPictureName()
            storeTemporaryImage(map.tempImageKey, pictureName)
        } catch (Exception e) {
            log.error e
        }
    } else if (map.error) {
        log.error "Error: ${map.error}"
    }
}

// handle commands
def configure() {
	log.debug "Executing 'configure'"
    sendEvent(name:"switch", value: "on")
}

// handle commands
def take() {
	def userpassascii = "${BlueIrisUser}:${BlueIrisPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def host = BlueIrisIP 
    def hosthex = convertIPtoHex(host).toUpperCase()
    def porthex = convertPortToHex(BlueIrisPort).toUpperCase()

	device.deviceNetworkId = "$hosthex:$porthex" 
    log.debug "The device id configured is: $device.deviceNetworkId"
    
    def path = "/image/Index?q=$ImageQuality&s=$ImageScale" 

	def headers = [:] 
    //headers.put("HOST", "$hosthex:$porthex")
    headers.put("HOST", "$BlueIrisIP:$BlueIrisPort")
    headers.put("Authorization", userpass)
    
    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
    	path: path,
    	headers: headers
        )
        	
    hubAction.options = [outputMsgToS3:true]
    log.debug hubAction

	return hubAction
    
    }
    catch (Exception e) {
    	log.debug "Hit Exception $e on $hubAction"
    }    
}

private getPictureName() {
    return java.util.UUID.randomUUID().toString().replaceAll('-', '')
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

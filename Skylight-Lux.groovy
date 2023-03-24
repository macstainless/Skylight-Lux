definition(
    name: "Skylight Lux",
    namespace: "Skylight Lux",
    author: "MacStainless & ChatGPT",
    description: "Automatically adjust outdoor light brightness based on the outside illuminance",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    section("Outdoor light") {
        input "outdoorLight", "capability.switchLevel", title: "Which light?", required: true
//        input "maxBrightness", "number", title: "Maximum brightness of the light (in lumens)", required: false, defaultValue: 2800
//The above preference does nothing right now.
    
    }
    section("Dimming control") {
        input "dimmingEnabled", "bool", title: "Enable dimming?", defaultValue: true
        input "dimmingRatio", "number", title: "Dimming Ratio (Lux:Percentage)*", required: true, defaultValue: 357
    }
    section("Outside illuminance sensor") {
        input "illuminanceSensor", "capability.illuminanceMeasurement", title: "Which sensor?", required: true
    }
    section("Sunset/sunrise offset") {
        input "sunsetOffset", "number", title: "Minutes after sunset", required: false, defaultValue: 0
        input "sunriseOffset", "number", title: "Minutes before sunrise", required: false, defaultValue: 0
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(illuminanceSensor, "illuminance", illuminanceHandler)
    schedule("0 * * * * ?", updateHandler)
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(illuminanceSensor, "illuminance", illuminanceHandler)
    unschedule()
    schedule("0 * * * * ?", updateHandler)
}

def illuminanceHandler(evt) {
    log.debug "Illuminance level is ${evt.doubleValue} lux"
    updateBrightness()
}

def updateHandler() {
    log.debug "Scheduling brightness update"
    updateBrightness()
}

def updateBrightness() {
    def maxBrightness = 2800
    def ratio = maxBrightness / 10000.0
    def currentLux = illuminanceSensor.currentIlluminance
    def calculatedBrightness = Math.round(currentLux * ratio)
    def adjustedSetpoint = (calculatedBrightness / maxBrightness) * 100
    log.debug "Setting brightness to ${adjustedSetpoint}%"
    outdoorLight.setLevel(adjustedSetpoint.intValue())
}

def isDark() {
    def sun = location.sunset(timeZone: location.timeZone)
    def localSunset = sun.plusMinutes(sunsetOffset)
    def localSunrise = location.sunrise(timeZone: location.timeZone).minusMinutes(sunriseOffset)
    def now = new Date().time
    if (now > localSunset.time || now < localSunrise.time) {
        return true
    } else {
        return false
    }
}

Warnings & Alerts
=================

During the normal course of operation, Purple Robot may emit a variety of warnings and errors that reflect how changing environmental conditions may adversely affect the optimal functioning of the app. This document describes those errors and warnings.

*Note that this is a temporary document intended for use until the official Purple Robot documentation package comes together.*

#Sanity Checks#

*Sanity checks* are routine scheduled jobs that Purple Robot runs to identify a variety of potential problems. A sanity check can return one of three states: error, warning, and okay. 

In pre-5.0 Android devices, the state of the sanity checks are reflected in the color of the Purple Robot icon. A red icon indicates that at least one error condition was observed. A yellow icon indicates at least one warning condition. A white icon signals that no errors or warning conditions were observed.

In post-5.0 Android devices, icon colors were deprecated for a plain monochrome presentation. An error state is represented by an square icon with an inset circle containing an exclamation point. Warnings are indicated by a triangular exclamation points. No errors or warnings is indicated with a graph icon inset in the rounded square.

All sanity warnings and errors are visible within the diagnostics screen of the Purple Robot app and individual conditions may be tapped to take the user to a location on the system where corrective action may be taken.

###Android 5.0 Memory Check###

Android 5.0 shipped with a serious memory issue that degrades Purple Robot's data collection ability after an extended runtime:

`Your device is likely to experience widespread instability due to a system memory leak, causing Purple Robot and other apps to crash. Please restart your device to reclaim the leaked resources.`

This sanity check emits a warning when an Android 5.0.x device running Purple Robot has been running for more than 18 hours.

**Corrective action:** Reboot the device.


###Bluetooth Enabled Check###

This sanity check inspects the device's Bluetooth configuration to determine if any of the enabled Bluetooth probes will experience issues because of the local configuration:

This check emits a warning if a Bluetooth probe is enabled and no Bluetooth hardware exists on the device for completing the probe's task.

`Some probes are unable to collect data due to missing Bluetooth hardware components. Please select a different device for use with this configuration.`

**Corrective action:** None.

This check emits an error if Bluetooth hardware is present on the device, but has been disabled by the system or the user.

`Required Bluetooth features are currently disabled. Tap here to re-enable Bluetooth features.`

**Corrective action:** Enable Bluetooth from the system settings.


###By-Minute Trigger Check###

This sanity check inspects the local triggers on the device and emits a warning if a trigger uses the `FREQ=MINUTELY` and `BYMINUTE` parameters in the iCalendar recurrence string. Ths 

`TRIGGER trigger defined using both FREQ=MINUTELY and BYMINUTE, introducing performance issues.`

**Corrective action:** The trigger must be corrected in the remote configuration or via scripting and the triggers must be reloaded on the device.


###Charging-Required Upload Check###

If the device is configured to upload payloads only when the device is connected to power, this check emits an error when the device is unconnected from power and more than 1000 pending payloads have accumulated on the device.

`Data uploads are restricted to when the device is plugged-in and a significant amount of data is accumulated. Please plug in the device to upload the accumulated files.`

**Corrective action:** Connect the device to power until all payloads have been uploaded from the device.


###Configuration Setup Check###

If the device is configured to refresh its configuration more than once every ten minutes (600 seconds) or data uploads are configured to attempt more than once every five minutes (300 seconds), this sanity check will emit a warning due to the negative effects these settings will have on power usage.

`The configuration refresh interval is less than 10 minutes. Consider extending this interval to extend battery life. (Settings > Refresh Interval)`

`The HTTP data upload interval is less than 10 minutes. Consider extending this interval to extend battery life. (Settings > HTTP Upload Settings > HTTP Upload Interval)`

**Corrective action:** Update the device configuration to slow down configuration refreshes or data uploads.


###Disk Space Check###

This sanity check emits an warning if less than 10MB of disk space is available locally or an error if free space is less than 4MB.

`Less than 4MB of space is available on local storage.`

`Less than 10MB of space is available on local storage.`

`Less than 4MB of space is available on external storage (SD Card).`

`Less than 10MB of space is available on external storage (SD Card).`

`Unable to inspect external storage (SD Card).`

**Corrective action:** Clear unneeded files from the device to free more disk space.


###Google Services Check###

This sanity check verifies that the local device has a sufficiently-recent version of the Google Play Services installed.

`Google Play Services are missing.`

`Google Play Services are out of date.`

`Google Play Services are disabled.`

`Google Play Services are invalid.`

**Corrective action:** Install, update or enable Google Play Services to meet Purple Robot requirements.


###Last Upload Check###

This sanity check verifies that data payloads have been uploaded recently. If the last upload has been more than 24 hours, an error is emitted. If the last upload has been between 12 and 24 hours, a warning is emitted.

`Sensor data has not ever been uploaded.`

`Sensor data has not been uploaded in the last day.`

`Sensor data has not been uploaded in the last 12 hours.`

**Corrective action:** Place the device in a context (power/WiFi) that will permit it to begin uploading payloads from the device.


###Location Services Enabled Check###

This sanity check verifies that device location services are enabled if a location-aware probe is enabled.

`Required location services are currently disabled. Tap here to correct the issue.`


**Corrective action:** Enable location services on the device.


###Log Event Check###

This sanity check verifies than an excessive number of log events have not accumulated on the device. If more than 512 events are awaiting transmission, an error is emitted. If between 256 and 512 events are pending, a warning is emitted.

`A very significant number of log events are awaiting transmission. Connect your device to the network as soon as possible.`

`A significant number of log events log events are awaiting transmission. Connect your device to the network soon.`

**Corrective action:** Place the device in a context (power/WiFi) that will permit it to begin uploading events from the device.


###Multiple Uploaders Enabled Check###

This sanity check verifies that only one HTTP data uploader is enabled at a time.

`Multiple data upload plugins are currently enabled. This will likely result in duplicate readings at the data destination.`


**Corrective action:** Disable the extraneous HTTP data uploaders.


###Pebble Probe Checks###

This check emits a warning if more than one probe using a Pebble wearable device is enabled.

`Multiple Pebble probes are currently enabled. Please disable all but one.`

**Corrective action:** Disable the unneeded Pebble probes.


###Scheme Configuration Check###

This check emits a warning if the local configuration varies from a remote Scheme configuration file and errors if the remote configuration is invalid or missing.

`Missing configuration value: KEY`

`Changed configuration value: KEY`

`Invalid Scheme configuration file`

**Corrective action:** Correct the configuration or configuration location on the local device.


###Upload Progress Check###

This check emits errors or warnings if the local device's data accumulation rate exceeds its ability to transmit data payloads to the server in a quick-enough manner to prevent data from filling up the device. An error is emitted when accumulation exceeds transmission. A warning is emitted when accumulation exceeds 50% of transmission rate.

`Current sensor acquisition exceeds the available outbound bandwidth.`

`Current sensor acquisition consumes a significant amount of the available outbound bandwidth.`

`Unable to compare available bandwidth with current sensor data acquisition rates.`

**Corrective action:** Either configure the device to collect less data over a given duration or situate the device in a networking context that is capable of keeping up with data accumulation.


###WiFi-Enabled Check###

This check determines if WiFi is enabled when upload plugins are configured to wait until WLAN connectivity is available to transmit payload data. If more than 100 payloads are pending, an is emitted, otherwise a warning is used.

`Data uploads are restricted to WiFi networks, but the device's WiFi connection is disabled.`

`Data uploads are restricted to WiFi networks, but the device's WiFi connection is disabled and a significant amount of data is accumulating on the device.`

**Corrective action:** Situate the device in a network context where WiFi is available and the device is configured to use a local WiFi network.


#Probe Checks#

In addition to the sanity checks that run on a fixed schedule, probes may also emit errors and warnings when the device is misconfigured or in a suboptimal state. The following errors and warnings may be emitted by probes.

###Online Service Logins###

The following probes emit warnings when a probe uses an online service and Purple Robot has not been authenticated to use the service:

* Facebook
* GitHub
* Instagram
* Jawbone
* Twitter
* Foursquare
* Fitbit
* iHealth

**Corrective action:** Tap the warning from the diagnostics screen and authenticate witht the given service.


###Android Wear Devices###

If an Android Wear probe is enababled and the device has not been set up with a Wear device or the device itself is experiencing issues, a variety of warnings may be emitted.

`Please install the Android Wear companion app to use the Android Wear probe.`

`The connected Android Wear device is running low on power. Please charge the device soon.`

**Corrective action:** Install and configure an Android Wear device or being the wearable device out of the warning state by addressing the active issue (e.g. charge its battery).


###Pebble Devices###

If a probe uses Pebble wearables and the device isn't connected or configured, the probes may emit a variety of warnings.

`Please connect your Pebble device to the Pebble companion app.`

`Received data from an obsolete Livewell Pebble watchface. Please upgrade your watchface.`

**Corrective action:** Install, connect or upgrade the Pebble wearable.


###Location Labeling###

If a location probe has calibration enabled, Purple Robot will request the name of location clusters for associating semantic details with raw latitude and longitude values.

`Please provide some information about places you have visited to help the system function better.`

**Corrective action:** Label location clusters as the device requests.


###Contact Labelling###

If a communication probe has calibration enabled, Purple Robot will request information about contact records for associating relationship details with the call and text log.

`Please provide some information about your friends and family to help the system function better.`

**Corrective action:** Label contact records as the device requests.


###Weather Underground Key###

If the Weather Underground probe is enabled and no key has been provided, Purple Robot will emit a warning.

`A custom Weather Underground API key is required for fetching weather data. Please obtain one and enter it in the Settings. (Probe Configuration > External Service Probes > Weather Underground > API Key)`

**Corrective action:** Obtain a Weather Underground key and enter the key in the probe configuration.


#Upload Warnings#

In addition to the sanity checks, Purple Robot may also report any data upload issues on its main screen. The following messages may be visible:

`Waiting for WiFi...`: Device is configured to upload over WiFi, but no such network is available or configured.

`Waiting for power...`: Device is configured to upload when charging, but the device is not plugged in.

`Checksum error.`: Server did not return the expected checksum. Possible error in data transmission.

`Application error: ERROR`: Unspecified server error. Review the server logs for specifics.

`HTTP connection failure.`: HTTP protocol error. Verify that the upload server is reachable.

`Socket timeout.`: Network connection timed out. Verify that the local network is configured properly.

`Socket error: ERROR`: Network error. Verify that the local network is configured properly.

`Upload unavailable.`: Remote upload server is unreachable. Verify that the local network is configured properly.

`Server response error.`: Received a misformatted response. Inspect the server logs or response contents to diagnose the error.

`Unverified server.`: Server does not have a valid SSL certificate.

`General error: ERROR`: Unknown or uncaught error. Contents of `ERROR` will indicate next diagnosis steps.

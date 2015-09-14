Warnings & Alerts
=================

During the normal course of operation, Purple Robot may emit a variety of warnings and errors that reflect how changing environmental conditions may adversely affect the optimal functioning of the app. This document describes those errors and warnings.

*Note that this is a temporary document intended for use until the official Purple Robot documentation package comes together*

#Sanity Checks#

*Sanity checks* are routine scheduled jobs that Purple Robot runs to identify a variety of potential problems. A sanity check can return one of three states: error, warning, and okay. 

In pre-5.0 Android devices, the state of the sanity checks are reflected in the color of the Purple Robot icon. A red icon indicates that at least one error condition was observed. A yellow icon indicates at least one warning condition. A white icon signals that no errors or warning conditions were observed.

In post-5.0 Android devices, icon colors were deprecated for a plain monochrome presentation. An error state is represented by an square icon with an inset circle containing an exclamation point. Warnings are indicated by a triangular exclamation points. No errors or warnings is indicated with a graph icon inset in the rounded square.

All sanity warnings and errors are visible within the diagnostics screen of the Purple Robot app and individual conditions may be tapped to take the user to a location on the system where corrective action may be taken.

###Android 5.0 Memory Check###

Android 5.0 shipped with a serious memory issue that degrades Purple Robot's data collection ability after an extended runtime:

name_sanity_android_five_memory_warning

This sanity check emits a warning when an Android 5.0.x device running Purple Robot has been running for more than 18 hours.

**Corrective action:** Reboot the device.


###Bluetooth Enabled Check###

This sanity check inspects the device's Bluetooth configuration to determine if any of the enabled Bluetooth probes will experience issues because of the local configuration:

This check emits a warning if a Bluetooth probe is enabled and no Bluetooth hardware exists on the device for completing the probe's task.

name_sanity_bluetooth_missing_error

**Corrective action:** None.

This check emits an error if Bluetooth hardware is present on the device, but has been disabled by the system or the user.

name_sanity_bluetooth_disabled_error

**Corrective action:** Enable Bluetooth from the system settings.


##By-Minute Trigger Check##

This sanity check inspects the local triggers on the device and emits a warning if a trigger uses the `FREQ=MINUTELY` and `BYMINUTE` parameters in the iCalendar recurrence string. Ths 

name_sanity_trigger_byminute_warning

**Corrective action:** The trigger must be corrected in the remote configuration or via scripting and teh triggers must be reloaded on the device.


##Charging-Required Upload Check##

If the device is configured to upload payloads only when the device is connected to power, this check emits an error when the device is unconnected from power and more than 1000 pending payloads have accumulated on the device.

name_sanity_charging_required_error

**Corrective action:** Connect the device to power until all payloads have been uploaded from the device.


##Configuration Setup Check##

If the device is configured to refresh its configuration more than once every ten minutes (600 seconds) or data uploads are configured to attempt more than once every five minutes (300 seconds), this sanity check will emit a warning due to the negative effects these settings will have on power usage.

name_sanity_configuration_refresh_warning

name_sanity_configuration_upload_warning

**Corrective action:** Update the device configuration to slow down configuration refreshes or data uploads.


##Disk Space Check##

This sanity check emits an warning if less than 10MB of disk space is available locally or an error if free space is less than 4MB.

name_sanity_disk_space_local_error

name_sanity_disk_space_local_warning

name_sanity_disk_space_external_error

name_sanity_disk_space_external_warning

name_sanity_disk_space_external_unknown_warning

**Corrective action:** Clear unneeded files from the device to free more disk space.


##Google Services Check##

This sanity check verifies that the local device has a sufficiently-recent version of the Google Play Services installed.

name_sanity_google_play_update_invalid

name_sanity_google_play_update_disabled

name_sanity_google_play_update_required

name_sanity_google_play_missing

**Corrective action:** Install or update Google Play Services to meet Purple Robot requirements.


##Last Upload Check##

This sanity check verifies that data payloads have been uploaded recently. If the last upload has been more than 24 hours, an error is emitted. If the last upload has been between 12 and 24 hours, a warning is emitted.

name_sanity_last_upload_never

name_sanity_last_upload_error

name_sanity_last_upload_warning

**Corrective action:** Place the device in a context (power/WiFi) that will permit it to begin uploading payloads from the device.


##Location Services Enabled Check##

This sanity check verifies that device location services are enabled if a location-aware probe is enabled.

name_sanity_location_services_enabled_warning

**Corrective action:** Enable location services on the device.


##Log Event Check##

This sanity check verifies than an excessive number of log events have not accumulated on the device. If more than 512 events are awaiting transmission, an error is emitted. If between 256 and 512 events are pending, a warning is emitted.

name_sanity_log_events_error

name_sanity_log_events_warning

**Corrective action:** Place the device in a context (power/WiFi) that will permit it to begin uploading events from the device.

##Multiple Uploaders Enabled Check##

This sanity check verifies that only one HTTP data uploader is enabled at a time.

name_sanity_multiple_uploaders_enabled_warning

**Corrective action:** Disable the extraneous HTTP data uploaders.


##Pebble Probe Checks##

This check emits a warning if more than one probe using a Pebble wearable device is enabled.

name_sanity_pebble_probes_warning

**Corrective action:** Disable the unneeded Pebble probes.


##Scheme Configuration Check##

This check emits a warning if the local configuration varies from a remote Scheme configuration file and errors if the remote configuration is invalid or missing.

scheme_config_check_changed

scheme_config_check_missing

scheme_config_invalid_config

**Corrective action:** Correct the configuration or configuration location on the local device.


##Upload Progress Check##

This check emits errors or warnings if the local device's data accumulation rate exceeds its ability to transmit data payloads to the server in a quick-enough manner to prevent data from filling up the device. An error is emitted when accumulation exceeds transmission. A warning is emitted when accumulation exceeds 50% of transmission rate.

name_sanity_upload_progress_unknown

name_sanity_upload_progress_unknown

name_sanity_upload_progress_error

name_sanity_upload_progress_warning

**Corrective action:** Either configure the device to collect less data over a given duration or situate the device in a networking context that is capable of keeping up with data accumulation.


##WiFi-Enabled Check##

This check determines if WiFi is enabled when upload plugins are configured to wait until WLAN connectivity is available to transmit payload data. If more than 100 payloads are pending, an is emitted, otherwise a warning is used.

name_sanity_wifi_enabled_error

name_sanity_wifi_enabled_warning

**Corrective action:** Situate the device in a network context where WiFi is available and the device is configured to use a local WiFi network.


#Probe Checks#

In addition to the sanity checks that run on a fixed schedule, probes may also emit errors and warnings when the device is misconfigured or in a suboptimal state.


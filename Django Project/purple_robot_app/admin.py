from django.contrib import admin

from purple_robot_app.models import *

class PurpleRobotConfigurationAdmin(admin.ModelAdmin):
    list_display = ('name', 'slug', 'added')
    list_filter = ['added',]
    search_fields = ['name', 'slug', 'contents']

admin.site.register(PurpleRobotConfiguration, PurpleRobotConfigurationAdmin)

class PurpleRobotPayloadAdmin(admin.ModelAdmin):
    list_display = ('user_id', 'added', 'process_tags',)
    list_filter = ['added', 'user_id']
    search_fields = ['payload', 'errors', 'process_tags']

admin.site.register(PurpleRobotPayload, PurpleRobotPayloadAdmin)

class PurpleRobotEventAdmin(admin.ModelAdmin):
    list_display = ('event', 'logged', 'user_id')
    list_filter = ['event', 'logged', 'user_id']
    search_fields = ['event', 'user_id', 'payload']

admin.site.register(PurpleRobotEvent, PurpleRobotEventAdmin)

class PurpleRobotReadingAdmin(admin.ModelAdmin):
    list_display = ('probe', 'user_id', 'logged')
    list_filter = ['probe', 'user_id', 'logged']
    search_fields = ['probe', 'user_id', 'payload']

admin.site.register(PurpleRobotReading, PurpleRobotReadingAdmin)

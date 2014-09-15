# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Adding model 'PurpleRobotConfiguration'
        db.create_table(u'purple_robot_app_purplerobotconfiguration', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(max_length=1024)),
            ('slug', self.gf('django.db.models.fields.SlugField')(unique=True, max_length=1024)),
            ('contents', self.gf('django.db.models.fields.TextField')(max_length=1048576)),
            ('added', self.gf('django.db.models.fields.DateTimeField')()),
        ))
        db.send_create_signal(u'purple_robot_app', ['PurpleRobotConfiguration'])

        # Adding model 'PurpleRobotPayload'
        db.create_table(u'purple_robot_app_purplerobotpayload', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('added', self.gf('django.db.models.fields.DateTimeField')(auto_now_add=True, blank=True)),
            ('payload', self.gf('django.db.models.fields.TextField')(max_length=8388608)),
            ('process_tags', self.gf('django.db.models.fields.CharField')(max_length=1024, null=True, blank=True)),
            ('errors', self.gf('django.db.models.fields.TextField')(max_length=65536, null=True, blank=True)),
        ))
        db.send_create_signal(u'purple_robot_app', ['PurpleRobotPayload'])

        # Adding model 'PurpleRobotEvent'
        db.create_table(u'purple_robot_app_purplerobotevent', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('event', self.gf('django.db.models.fields.CharField')(max_length=1024)),
            ('logged', self.gf('django.db.models.fields.DateTimeField')()),
            ('user_id', self.gf('django.db.models.fields.CharField')(max_length=1024)),
            ('payload', self.gf('django.db.models.fields.TextField')(max_length=8388608, null=True, blank=True)),
        ))
        db.send_create_signal(u'purple_robot_app', ['PurpleRobotEvent'])


    def backwards(self, orm):
        # Deleting model 'PurpleRobotConfiguration'
        db.delete_table(u'purple_robot_app_purplerobotconfiguration')

        # Deleting model 'PurpleRobotPayload'
        db.delete_table(u'purple_robot_app_purplerobotpayload')

        # Deleting model 'PurpleRobotEvent'
        db.delete_table(u'purple_robot_app_purplerobotevent')


    models = {
        u'purple_robot_app.purplerobotconfiguration': {
            'Meta': {'object_name': 'PurpleRobotConfiguration'},
            'added': ('django.db.models.fields.DateTimeField', [], {}),
            'contents': ('django.db.models.fields.TextField', [], {'max_length': '1048576'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '1024'}),
            'slug': ('django.db.models.fields.SlugField', [], {'unique': 'True', 'max_length': '1024'})
        },
        u'purple_robot_app.purplerobotevent': {
            'Meta': {'object_name': 'PurpleRobotEvent'},
            'event': ('django.db.models.fields.CharField', [], {'max_length': '1024'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'logged': ('django.db.models.fields.DateTimeField', [], {}),
            'payload': ('django.db.models.fields.TextField', [], {'max_length': '8388608', 'null': 'True', 'blank': 'True'}),
            'user_id': ('django.db.models.fields.CharField', [], {'max_length': '1024'})
        },
        u'purple_robot_app.purplerobotpayload': {
            'Meta': {'object_name': 'PurpleRobotPayload'},
            'added': ('django.db.models.fields.DateTimeField', [], {'auto_now_add': 'True', 'blank': 'True'}),
            'errors': ('django.db.models.fields.TextField', [], {'max_length': '65536', 'null': 'True', 'blank': 'True'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'payload': ('django.db.models.fields.TextField', [], {'max_length': '8388608'}),
            'process_tags': ('django.db.models.fields.CharField', [], {'max_length': '1024', 'null': 'True', 'blank': 'True'})
        }
    }

    complete_apps = ['purple_robot_app']
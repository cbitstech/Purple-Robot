import json
import hashlib
import datetime
import pytz
import traceback
import urllib

from dateutil.parser import parse

from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, HttpResponseNotFound
from django.shortcuts import render, render_to_response, get_object_or_404, redirect
from django.template import RequestContext
from django.template.loader import render_to_string
from django.views.decorators.csrf import csrf_exempt

from models import *

@csrf_exempt
def ingest_payload(request):
    result = {}
    result['Status'] = 'error'
    result['Payload'] = "{}"
    
    try:
        json_str = request.POST['json']
        
        json_obj = json.loads(json_str)
        
        payload_str = json_obj['Payload']
           
        m = hashlib.md5()
        m.update((json_obj['UserHash'] + json_obj['Operation'] + json_obj['Payload']).encode('utf-8'))
        
        checksum_str = m.hexdigest()
        
        result = {}
        result['Status'] = 'error'
        result['Payload'] = "{}"
        
        if checksum_str == json_obj['Checksum']:
            result['Status'] = 'success'
        
            payload_json = json.loads(payload_str)
        
            payload = PurpleRobotPayload(payload=json.dumps(payload_json, indent=2, ensure_ascii=False), user_id=json_obj['UserHash'])
            payload.save()
        
            m = hashlib.md5()
            m.update(result['Status'] + result['Payload'])
        
            result['Checksum'] = m.hexdigest()
        else:
            result['Error'] = 'Source checksum ' + json_obj['Checksum'] + ' doesn\'t match destination checksum ' + checksum_str + '.'
    except Exception, e:
        result['Error'] = str(e)
        
    return HttpResponse(json.dumps(result), content_type='application/json')
    

@csrf_exempt
def ingest_payload_print(request):
    result = {}
    result['Status'] = 'error'
    result['Payload'] = "{}"
    
    try:
        json_str = request.POST['json']
        
        json_obj = json.loads(json_str)
        
        payload_str = json_obj['Payload']
           
        m = hashlib.md5()
        m.update((json_obj['UserHash'] + json_obj['Operation'] + json_obj['Payload']).encode('utf-8'))
        
        checksum_str = m.hexdigest()
        
        result = {}
        result['Status'] = 'error'
        result['Payload'] = "{}"
        
        if checksum_str == json_obj['Checksum']:
            result['Status'] = 'success'
        
            payload_json = json.loads(payload_str)
            
            print('PAYLOAD: ' + json.dumps(payload_json, indent=2, ensure_ascii=False))
        
            m = hashlib.md5()
            m.update(result['Status'] + result['Payload'])
        
            result['Checksum'] = m.hexdigest()
        else:
            result['Error'] = 'Source checksum ' + json_obj['Checksum'] + ' doesn\'t match destination checksum ' + checksum_str + '.'
    except Exception, e:
        result['Error'] = str(e)
        
    return HttpResponse(json.dumps(result), content_type='application/json')

@csrf_exempt
def log_event(request):
    payload = json.loads(request.POST['json'])
    
    logged = datetime.datetime.fromtimestamp(int(payload['timestamp']))
    
    if PurpleRobotEvent.objects.filter(logged=logged, event=payload['event_type']).count() == 0:
        try:
            if len(payload['user_id'].strip()) == 0:
                payload['user_id'] = '-'
        except KeyError:
            payload['user_id'] = '-'
        
        event = PurpleRobotEvent(payload=json.dumps(payload, indent=2))
        event.logged = logged
        event.event = payload['event_type']
        event.user_id = payload['user_id']
        
        event.save()
    
    return HttpResponse(json.dumps({ 'result': 'success' }), content_type='application/json')


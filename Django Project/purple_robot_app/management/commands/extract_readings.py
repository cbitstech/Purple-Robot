from datetime import datetime
import json
import pytz
import urllib
import urllib2

from django.core.management.base import BaseCommand, CommandError

from purple_robot_app.models import *

class Command(BaseCommand):
    def handle(self, *args, **options):
        tag = 'extracted_readings'
        
        for payload in PurpleRobotPayload.objects.exclude(process_tags__contains=tag):
            items = json.loads(payload.payload)

            user_id = payload.user_id
            
            for item in items:
                reading = PurpleRobotReading(probe=item['PROBE'], user_id=payload.user_id)
                reading.payload = json.dumps(item, indent=2)
                reading.logged = datetime.utcfromtimestamp(item['TIMESTAMP']).replace(tzinfo=pytz.utc)
                    
                reading.save()
                
            tags = payload.process_tags
                
            if tags == None or tags.find(tag) == -1:
                if tags == None or len(tags) == 0:
                    tags = tag
                else:
                    tags += ' ' + tag
                        
                payload.process_tags = tags
                    
                payload.save()

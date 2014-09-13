import sys
sys.stdout = sys.stderr

import atexit
import cherrypy
import datetime
import hashlib
import os
import psycopg2
import threading
import time

from json import dumps, loads

# CONFIG = {
#    'global': {
#        'server.socket_port': 9080,
#        'server.socket_host': '0.0.0.0'
#    }
# }

DATABASE = 'purple_robot'
USERNAME = 'p_robot'
PASSWORD = 'your_password_here'

cherrypy.config.update({'environment': 'embedded'})
cherrypy.config.update({'request.show_tracebacks': True})

if cherrypy.__version__.startswith('3.0') and cherrypy.engine.state == 0:
    cherrypy.engine.start(blocking=False)
    atexit.register(cherrypy.engine.stop)

class RobotPost:
    def index(self, json=None):
        conn = psycopg2.connect('dbname=' + DATABASE + ' user=' + USERNAME + ' password=' + PASSWORD + ' host=127.0.0.1')
        
        cur = conn.cursor()
        
        try:
            cur.execute('SELECT COUNT(id) FROM readings;')
        except psycopg2.ProgrammingError:
            cur.close()
            conn.close()

            conn = psycopg2.connect('dbname=' + DATABASE + ' user=' + USERNAME + ' password=' + PASSWORD + ' host=127.0.0.1')
            cur = conn.cursor()

            cur.execute('CREATE TABLE readings (id SERIAL PRIMARY KEY, user_id text NOT NULL, payload JSON NOT NULL, logged TIMESTAMP);')
    
        json_obj = loads(json)
        
        payload_str = json_obj['Payload']
        user_hash = json_obj['UserHash']
        
        m = hashlib.md5()
        m.update(json_obj['UserHash'] + json_obj['Operation'] + json_obj['Payload'])
        
        checksum_str = m.hexdigest()

        result = {}
        result['Status'] = 'error'
        result['Payload'] = "{}"
        
        if checksum_str == json_obj['Checksum']:
            result['Status'] = 'success'
            
            cur.execute('INSERT INTO readings (user_id, payload, logged) VALUES (%s, %s, %s);', (user_hash, dumps(json_obj, indent=2), datetime.datetime.now()))
            
            conn.commit()
        
        m = hashlib.md5()
        m.update(result['Status'] + result['Payload'])

        result['Checksum'] = m.hexdigest()
        
        cherrypy.response.headers['Content-Type']= 'application/json'
        
        cur.close()
        conn.close()

        return dumps(result, indent=2)
        
    index.exposed = True

# cherrypy.quickstart(RobotPost(), config=CONFIG)

application = cherrypy.Application(RobotPost(), script_name=None, config=None)

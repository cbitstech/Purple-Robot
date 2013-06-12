import cherrypy
import hashlib
import os
import time

from json import dumps, loads

CONFIG = {
    'global': {
        'server.socket_port': 9080,
        'server.socket_host': '0.0.0.0'
    }
}

class RobotPost:
    def index(self, json=None):
        json_obj = loads(json)
        
        payload_str = json_obj['Payload']
        
        m = hashlib.md5()
        m.update(json_obj['UserHash'] + json_obj['Operation'] + json_obj['Payload'])
        
        checksum_str = m.hexdigest()

        result = {}
        result['Status'] = 'error'
        result['Payload'] = "{}"
        
        if checksum_str == json_obj['Checksum']:
            result['Status'] = 'success'
            
            timestamp = str(time.time())
            
            path = 'files' + os.sep + json_obj['UserHash'] + os.sep + timestamp + '.json'
            
            d = os.path.dirname(path)
            
            if not os.path.exists(d):
                os.makedirs(d)
                
            f = open(path, 'w')
            f.write(dumps(json_obj, indent=2))
            f.close()
        
        m = hashlib.md5()
        m.update(result['Status'] + result['Payload'])

        result['Checksum'] = m.hexdigest()
        
        cherrypy.response.headers['Content-Type']= 'application/json'

        return dumps(result, indent=2)
        
    index.exposed = True

cherrypy.quickstart(RobotPost(), config=CONFIG)

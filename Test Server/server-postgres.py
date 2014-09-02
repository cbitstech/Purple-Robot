import cherrypy
import datetime
import hashlib
import os
import psycopg2
import time

from json import dumps, loads

CONFIG = {
    'global': {
        'server.socket_port': 9080,
        'server.socket_host': '0.0.0.0'
    }
}

USERNAME = 'p_robot'
PASSWORD = 'p_robot_password'

class RobotPost:
    def index(self, json=None):
        conn = psycopg2.connect('dbname=p_robot user=p_robot password=p_robot_password host=127.0.0.1')

        cur = conn.cursor()

        try:
            cur.execute('SELECT COUNT(id) FROM readings;')
        except psycopg2.ProgrammingError:
            cur.close()
            conn.close()

            conn = psycopg2.connect('dbname=p_robot user=p_robot password=p_robot_password host=127.0.0.1')
            cur = conn.cursor()

            cur.execute('CREATE TABLE readings (id SERIAL PRIMARY KEY, payload JSON NOT NULL, logged TIMESTAMP);')

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

            cur.execute('INSERT INTO readings (payload, logged) VALUES (%s, %s);', (dumps(json_obj, indent=2), datetime.datetime.now()))

            conn.commit()

        m = hashlib.md5()
        m.update(result['Status'] + result['Payload'])

        result['Checksum'] = m.hexdigest()

        cherrypy.response.headers['Content-Type']= 'application/json'

        cur.close()
        conn.close()

        return dumps(result, indent=2)

    index.exposed = True

cherrypy.quickstart(RobotPost(), config=CONFIG)

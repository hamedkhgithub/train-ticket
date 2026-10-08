"""Personal API boundary. Raja live integration is intentionally unimplemented."""
import os, json, secrets, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

class ProviderUnavailable(Exception): pass

def validate_query(params):
    q = {k: params[k][0].strip() for k in ('origin', 'destination', 'date', 'passengers')}
    if not q['origin'] or not q['destination'] or q['origin']==q['destination']:
        raise ValueError('invalid route')
    if max(len(q['origin']),len(q['destination']))>100: raise ValueError('route too long')
    date=datetime.date.fromisoformat(q['date'])
    today=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=3,minutes=30))).date()
    if date<today: raise ValueError('past departure')
    q['passengers']=int(q['passengers'])
    if not 1<=q['passengers']<=6: raise ValueError('passengers out of range')
    return q

def search_raja(query):
    # Replace with a VERIFIED Raja integration. Do not invent endpoints,
    # station codes, sold-out trips or booking deep links.
    raise ProviderUnavailable('Raja live adapter has not been connected or verified')

def validate_trains(trains, query):
    if not isinstance(trains,list): raise ValueError('invalid provider payload')
    seen=set()
    for t in trains:
        for k in ('id','name','departure','price_toman','seats','status'): t[k]
        if not isinstance(t['id'],str) or not t['id'] or t['id'] in seen: raise ValueError('invalid ID')
        seen.add(t['id'])
        if type(t['seats']) is not int or t['seats']<0: raise ValueError('invalid capacity')
        if type(t['price_toman']) is not int or t['price_toman']<0: raise ValueError('invalid price')
        if t['status'] not in ('available','sold_out','unknown'): raise ValueError('invalid status')
        if t['status']=='sold_out' and t['seats']!=0: raise ValueError('inconsistent capacity')
        if t['status']=='available' and t['seats']>=query['passengers']:
            u=urlparse(t['purchase_url'])
            if u.scheme!='https' or not u.hostname or not (u.hostname=='raja.ir' or u.hostname.endswith('.raja.ir')):
                raise ValueError('invalid purchase link')
    return trains

class Handler(BaseHTTPRequestHandler):
    def respond(self,status,data):
        body=json.dumps(data,ensure_ascii=False).encode()
        self.send_response(status);self.send_header('Content-Type','application/json; charset=utf-8')
        self.send_header('Cache-Control','no-store');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
    def do_GET(self):
        if not secrets.compare_digest(self.headers.get('Authorization',''), 'Bearer '+os.environ['TRAINWATCH_TOKEN']):
            return self.respond(401,{'error':'unauthorized'})
        u=urlparse(self.path)
        if u.path!='/search': return self.respond(404,{'error':'not found'})
        try:q=validate_query(parse_qs(u.query))
        except (KeyError,ValueError,IndexError):return self.respond(400,{'error':'invalid search'})
        try:
            trains=validate_trains(search_raja(q),q)
            self.respond(200,{'demo':False,'checked_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'trains':trains})
        except ProviderUnavailable as e:self.respond(503,{'error':str(e)})
        except Exception:self.respond(502,{'error':'provider failed; capacity unknown'})
    def log_message(self,*args):pass # No tokens/routes in request logs.

if __name__=='__main__':
    if len(os.environ.get('TRAINWATCH_TOKEN',''))<24:raise SystemExit('Set TRAINWATCH_TOKEN to a random value of at least 24 characters')
    ThreadingHTTPServer(('127.0.0.1',int(os.environ.get('PORT','8080'))),Handler).serve_forever()

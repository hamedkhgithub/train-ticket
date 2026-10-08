import unittest,datetime
from server import validate_query,validate_trains,search_raja,ProviderUnavailable
class Tests(unittest.TestCase):
 def query(self):return {'origin':['تهران'],'destination':['مشهد'],'date':[(datetime.date.today()+datetime.timedelta(days=5)).isoformat()],'passengers':['2']}
 def train(self):return {'id':'x','name':'test','departure':'10:00','price_toman':100,'seats':2,'status':'available','purchase_url':'https://ticket.raja.ir/'}
 def test_valid(self):self.assertEqual(validate_query(self.query())['passengers'],2)
 def test_route(self):
  q=self.query();q['destination']=q['origin']
  with self.assertRaises(ValueError):validate_query(q)
 def test_count(self):
  q=self.query();q['passengers']=['0']
  with self.assertRaises(ValueError):validate_query(q)
 def test_link(self):
  t=self.train();t['purchase_url']='https://raja.ir.evil.example/'
  with self.assertRaises(ValueError):validate_trains([t],{'passengers':2})
 def test_inconsistent(self):
  t=self.train();t['status']='sold_out'
  with self.assertRaises(ValueError):validate_trains([t],{'passengers':2})
 def test_unconnected_never_fakes_capacity(self):
  with self.assertRaises(ProviderUnavailable):search_raja({})
if __name__=='__main__':unittest.main()

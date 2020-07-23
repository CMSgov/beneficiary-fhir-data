import random
import events
from datetime import datetime, timedelta

class App:  
  def generate_session(self, start_time: datetime, bene):
    return []

  def random_hash(self):
    hex_letters = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f']
    return ''.join(random.choice(hex_letters) for _ in range(32))

class Humana(App):
  def app_id(self):
    return 13
  
  def app_name(self):
    return "Humana"
  
  def dev_id(self):
    return 180
  
  def dev_name(self):
    return "mscrimshire+humana@gmail.com"

  def generate_session(self, start_time: datetime, bene):
    token_hash = self.random_hash()
    current_time = start_time
    call_period = timedelta(seconds=0.4)

    if random.randint(0,30) == 0: 
      yield events.my_medicare_login(bene, self, current_time)
      current_time += call_period

    yield events.token_response(bene, self, current_time, token_hash)
    current_time += call_period

    for _ in range(random.randint(1, 9)):
      yield events.eob_response(bene, self, current_time, token_hash)
      current_time += call_period

    yield events.patient_response(bene, self, current_time, token_hash)


class Evidation(App):
  def app_id(self):
    return 32
  
  def app_name(self):
    return "Evidation on behalf of Heartline"
  
  def dev_id(self):
    return  11770
  
  def dev_name(self):
    return "mscrimshire+evidationheartline@gmail.com"

  def generate_session(self, start_time: datetime, bene):
    token_hash = self.random_hash()
    current_time = start_time
    call_period = timedelta(seconds=0.3)

    if random.randint(0,20) == 0: 
      yield events.my_medicare_login(bene, self, current_time)
      current_time += call_period

    yield events.token_response(bene, self, current_time, token_hash)
    current_time += call_period

    yield events.user_info_response(bene, self, current_time, token_hash)
    current_time += call_period

    yield events.eob_response(bene, self, current_time, token_hash)
    current_time += call_period

    yield events.patient_response(bene, self, current_time, token_hash)
    current_time += call_period

    yield events.coverage_response(bene, self, current_time, token_hash)

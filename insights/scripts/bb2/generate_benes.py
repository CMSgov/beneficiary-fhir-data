import uuid
import random

print("sls_id,fhir_id,random")

benes = set([])
while len(benes) < 1000: 
  benes.add(random.choice(['-1999', '-2000', '-2014']) + '000000' + str(random.randint(0, 9999)).zfill(4))

for bene in benes:
  random_value = str(random.choice(range(100)))
  sls_id = str(uuid.uuid4())
  print(sls_id + ','  + bene + ',' + random_value)
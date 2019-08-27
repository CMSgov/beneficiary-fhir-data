#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim: ai ts=4 sts=4 et sw=4
# Written by Alan Viars

from bs4 import BeautifulSoup
try:
    # For Python 3.0 and later
    from urllib.request import urlopen
except ImportError:
    # Fall back to Python 2's urllib2
    from urllib2 import urlopen
import re
import glob
import sys
import csv
from subprocess import call
from datetime import datetime
from collections import OrderedDict

def make_meta(my_url = "http://localhost:4000/resources/variables"):
    html_page = urlopen(my_url)
    soup = BeautifulSoup(html_page, "html.parser")
    all_meta = []
    for link in soup.findAll('a'):
          #print(link.get('href',''), link.string)
          url = "%s/%s" % (my_url, link.get('href'))
          print(url)      
          try:
              page = urlopen(url)
              bisque  = BeautifulSoup(page, "html.parser")
              #Get the title
              t = bisque.find('h1')
              
              # print("title", t.text)
              meta = OrderedDict()
              meta['name'] = link.get('href', " ")[:-1]
              title_line = t.text.split(':')
              if len(title_line) == 2:
                  meta['title']= title_line[1].strip()
              else:
                  meta['title']=""
              a = bisque.h1.next_sibling
              d= a.next_sibling
              #print(d.text)
              meta['description']  = d.text
               

              for ultag in bisque.find_all('ul'):
                 for litag in ultag.find_all('li'):
                     if litag.text.startswith("Short Name"):
                         meta['short_name'] = litag.text.split(':')[1].strip()
                     if litag.text.startswith("Long Name"):
                         meta['long_name'] = litag.text.split(':')[1].strip()
                     if litag.text.startswith("Type"):
                         meta['type'] = litag.text.split(':')[1].strip()
                     if litag.text.startswith("Length"):
                         meta['length'] = litag.text.split(':')[1].strip()
                     if litag.text.startswith("Source"):
                         meta['source'] = litag.text.split(':')[1].strip()
                     if litag.text.startswith("Value Format"):
                         meta['value_format'] = litag.text.split(':')[1].strip()
              #print(meta)
              if not meta['name'].startswith('?') or not meta['name']=="0":
                  all_meta.append(meta) 
              filename = "%s_meta.csv" % (meta['name']) 
              with open(filename, 'w') as f:  # Just use 'w' mode in 3.x
                    w = csv.DictWriter(f, meta.keys())
                    w.writeheader()
                    w.writerow(meta)

              # now lets get the tables when they exist
              l =[] 
              try:
                  l =[] 
                  line = OrderedDict()
                  table = bisque.find( "table", {"class":"ds-c-table"} )
                  for row in table.findAll("tr"):
                      #print(row)
                      values = row.find_all('th', {'class':'odd'})
                      descriptions = row.find_all('td', {'class': 'even'})
                      column_marker = 0 
                      for v in values:
                          #print(v)
                          line = OrderedDict()
                          line['value'] = v.get_text()
                          line['description'] = descriptions[column_marker].get_text()
                          column_marker += 1    
                          #print("LINE",line)
                          

                          l.append(line)
                  filename = "%s.csv" % (meta['name']) 
                  if l:
                      with open(filename, 'w') as f:  # Just use 'w' mode in 3.x
                          w = csv.DictWriter(f, ['value', 'description'])
                          w.writeheader()
                          for i in l:     
                              w.writerow(i) 
                      
  


            
              except AttributeError:
                  pass   

          except ValueError:
                pass
    filename = "all_meta.csv"
    # print(all_meta) 
    with open(filename, 'w') as f:  # Just use 'w' mode in 3.x
        w = csv.DictWriter(f, meta.keys())
        w.writeheader()
        w.writerows(all_meta)

if __name__ == "__main__":

    # Get the file from the command line
    make_meta()


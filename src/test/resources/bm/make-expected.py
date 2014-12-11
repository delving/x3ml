#!/usr/bin/env python 

from lxml import etree
import requests

bm20 = etree.parse('./BM20.xml')

for prn in bm20.xpath('//bm_prn'):

    graph = 'http://collection.britishmuseum.org/id/object/' + prn.text + '/graph'
    query = 'CONSTRUCT {?s ?p ?o} WHERE {GRAPH <' + graph + '> {?s ?p ?o}}'

    body = {'query': query}
    headers = {'Accept': 'text/plain'}

    statements = requests.post('http://sparql.researchspace.org', data=body, headers=headers)

    print statements.text.replace('http://erlangen-crm.org/current/', 'http://www.cidoc-crm.org/cidoc-crm/') 


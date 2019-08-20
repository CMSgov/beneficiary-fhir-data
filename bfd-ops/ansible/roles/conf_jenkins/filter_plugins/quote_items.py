from ansible import errors, vars
import json

# Inspired by this StackOverflow answer: <http://stackoverflow.com/a/15515929/1851299>

def quote_items(list_to_quote):
    return ["\"%s\"" % list_item for list_item in list_to_quote]

class FilterModule (object):
    def filters(self):
        return {"quote_items": quote_items}


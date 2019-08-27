from ansible import errors, vars
import json

# Inspired by this StackOverflow answer: <http://stackoverflow.com/a/24829795/1851299>

def get_members(hostvars, groups, target = 'all'):
    if type(hostvars) != vars.hostvars.HostVars:
        raise errors.AnsibleFilterError("|failed expects a HostVars")

    if type(groups) != dict:
        raise errors.AnsibleFilterError("|failed expects a Dictionary")

    data = []
    for host in groups[target]:
        data.append(hostvars[host])
    return data

class FilterModule (object):
    def filters(self):
        return {"get_members": get_members}


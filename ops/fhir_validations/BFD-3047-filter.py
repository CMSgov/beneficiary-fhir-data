import json
import sys

def filter(args):
    '''
    Filters a json payload response file such that only the paths required by CARIN IG and AB2D remain.
    Works on minimized and non-minimized json. Created for investigative spike work in BFD-3047, not to be
    used as a solution to actually filter real responses, but should result in a realistic filtered response
    which helps determine the size saved over-the-wire by doing filtering on real responses.
    '''
    f = open(args[0], "r")
    jsonData = json.load(f)
    
    ## paths start from inside entry; we will loop through each entry and apply the paths
    
    ## CARIN required fields for EOB profile
    ## one thing that's hard to validate is the unique id identifier is required, but this requires looking at the type of unique id value
    ## Due to this, im just going to include all identifiers. I think we only return unique ID types anyway.
    ## Added the required top level fields in the list, but adding them copies the whole object, so commented them out
    requiredFhirEobPaths = [
        #"meta",
        "meta.lastUpdated",
        "identifier", ## see above, only required for unique ids but its hard to determine so just return them all
        "identifier.value", ##only technically required for the unique id identifiers
        "status",
        "type",
        "use", ## >> required field we do not return if no data
        "patient",
        #"billablePeriod",
        "billablePeriod.start",
        "insurer",
        "provider",
        #"related", ## >> required field we do not return if no data
        "related.relationship",
        "related.reference",
        #"payee", ## >> required field we do not return if no data
        "payee.type",
        "payee.party",
        "outcome", ## >> required field we do not return if no data
        #"careTeam",
        "careTeam.provider",
        "careTeam.role",
        "supportingInfo", ## >> required field we do not return if no data
        #"insurance",
        "insurance.focal", ## >> required field we do not return if no data
        "insurance.coverage",
        #"item",
        "item.sequence",
        "item.noteNumber", ## >> required field we do not return if no data
        "item.adjudication",
        "item.adjudication.category",
        "total", ## >> required field we do not return if no data
        #"payment", ## >> required field we do not return if no data
        "payment.type",
        "processNote", ## >> required field we do not return if no data
        ## patient is optional per CARIN, but if exists must have:
        #"patient.meta",
        "patient.meta.lastUpdated", ## >> required field we do not return if no data + patient exists
        "patient.identifier", ## >> required field we do not return if no data + patient exists
        "patient.deceased", ## >> required field we do not return if no data + patient exists
        "patient.address.country" ## >> required field we do not return if no data + patient exists
    ]
    
    requiredAb2dPaths = [
        "billablePeriod.end",
        "id",
        "resourceType",
        "item.extension",
        "item.locationCodeableConcept",
        "item.productOrService",
        "item.diagnosisSequence",
        "item.locationAddress.state",
        "item.quantity.value",
        "item.servicedDate",
        "item.servicedPeriod.end",
        "item.servicedPeriod.start",
        "meta.profile",
        "patient",
        "procedure.date",
        "procedure.procedureCodeableConcept.coding.code",
        "procedure.procedureCodeableConcept.coding.system",
        "diagnosis",
        "extension",
        "facility.extension", ## facility also known as institutional in CARIN IG, but not part of EOB profile
        "type.coding"
    ]
    
    ## Combine the lists to make a master filter list
    fullPathKeepList = requiredFhirEobPaths + requiredAb2dPaths
    
    
    
    ## copy the dict but null out the entry, so we have all the things outside of entry
    ## we'll copy over only the parts of each entry we care about
    newJsonDict = jsonData.copy()
    newJsonDict["entry"] = []
    
    ## iterate over the list of entries to copy from
    for unfilteredEntry in jsonData["entry"]:
        #print("copying " + unfilteredEntry["resource"]["id"])
        newResource = {}
        for path in fullPathKeepList:
            pathSegments = path.split('.')
            
            ## if the path we want doesnt exist in the original data, nothing to copy
            if not pathExists(pathSegments, unfilteredEntry["resource"]):
                #print("Path " + path + " does not exist in data, skipping this path")
                continue
            else:
                filterData(newResource, unfilteredEntry["resource"], pathSegments)
        
        entry = { "resource": newResource }
        newJsonDict["entry"].append(entry)
    
    ## add indent=4 to prettify it
    print(json.dumps(newJsonDict))


## copies stuff into a new resource item
def filterData(newResourceData, originalJsonData, pathSegments):
    '''
    Copies data specified by the pathSegment from the original json resource data into
    the new resource data.
    '''
        
    ##for each part of the path, copy over what we need to ensure we have all parts of the path
    remainingPathSegments = pathSegments.copy()
    newResource = newResourceData
    dataToCopy = originalJsonData
    
    for currentPathSegment in pathSegments:

        #print(remainingPathSegments)
        ## if we're on the last path, it doesnt matter what we're looking at, just copy the whole node
        if len(remainingPathSegments) == 1:
            ## not all leaf nodes might have data at the requested path, so ignore if not
            if currentPathSegment in dataToCopy.keys():
                #print("copying from " + str(dataToCopy[currentPathSegment]))
                newResource[currentPathSegment] = dataToCopy[currentPathSegment]
            #else:
                #print(currentPathSegment + " not found in " + str(dataToCopy.keys()))
            return
        
        ## json nodes can be either dicts or lists
        ## check if this next segment is a dict by looking at the structure of the data to copy at this level
        isDict = isinstance(dataToCopy[currentPathSegment], dict)
        #print(currentPathSegment + " is dict: " + str(isDict))
        
        ## if it's a dict, we can just create the level normally if it doesnt exist and move down to it
        if isDict:
            addPathShellIfNotExists(newResource, currentPathSegment)
            
            ## move to the next level down, since we know the next level is a dict
            newResource = newResource[currentPathSegment]
            dataToCopy = dataToCopy[currentPathSegment]
            
            ## decrement remaining parts and change the current path
            #print("Removing " + currentPathSegment + " from " + str(remainingPathSegments))
            remainingPathSegments.remove(currentPathSegment)
        else:
            ## if its not a dict, weve got a list. call this method for each item in the list (which will each be a dict)
            #print(currentPathSegment + " is a list, iterating over list items...")
            
            #print("Removing " + currentPathSegment + " from " + str(remainingPathSegments))
            remainingPathSegments.remove(currentPathSegment)
            
            existsItems = False
            ## this is a hacky way to just add to existing items by keeping track of an index if we have existing items
            existingItemCount = 0
            newList = []
            
            
            ## if we have an existing set of items, use that. else make a new list
            if currentPathSegment in newResource.keys() and isinstance(newResource[currentPathSegment], list) and len(newResource[currentPathSegment]) > 0:
                #print("Using existing items")
                existsItems = True
            
            ## for each item in the list, either add it if the item hasnt been copied before
            ## or add the new field to the existing item if its been copied
            for item in dataToCopy[currentPathSegment]:
                if not existsItems:
                    newItem = {}
                    filterData(newItem , item, remainingPathSegments)
                    newList.append(newItem)
                else:
                    filterData(newResource[currentPathSegment][existingItemCount] , item, remainingPathSegments)
                    existingItemCount = existingItemCount + 1
            
            ## add the new list if we didnt just modify a bunch of existing items
            if not existsItems:
                newResource[currentPathSegment] = newList
            
            break

def addPathShellIfNotExists(newResourceSegment, currentPathSegment):
    '''
    Creates a 'shell' structure for a given path (so the final node has a full
    generated fhir structure to live in)
    '''
    ## check if this path shell already exists; if so do nothing
    if currentPathSegment in newResourceSegment.keys():
        #print(currentPathSegment + " path already exists in " + str(newResourceSegment.keys()))
        return
    else:
        #print("Creating path for " + currentPathSegment)
        newResourceSegment[currentPathSegment] = {}
        
def pathExists(pathParts, dictOrList):
    '''
    Check if the given path (represented as a list of split up path pieces) exists in the 
    given json data (represented as a dict or list)
    '''
    ## if we've run out of path parts, the path exists
    if not pathParts or len(pathParts) == 0:
        #print("No path parts remain, returning true")
        return True
    
    for pathPart in pathParts:
        if isinstance(dictOrList, dict) and pathPart in dictOrList.keys():
            #print("Checking for " + pathPart + " in " + str(dictOrList.keys()))
            ## for dict, just check if the path is in the keys
            if dictOrList[pathPart]:
                return pathExists(pathParts[1:], dictOrList[pathPart])
        if isinstance(dictOrList, list):
            #print("Checking for " + pathPart + " in each item in its list")
            ## for a list, check _each_ item's path and see if ANY have it
            for listItem in dictOrList:
                if pathExists(pathParts, listItem):
                    return True
            ## if we hit here, no items had it, return false
            return False
        else:
            return False

## Runs the program via run args when this file is run
if __name__ == "__main__":
    """
    Takes one arg, the json file full path.
    """
    filter(sys.argv[1:])
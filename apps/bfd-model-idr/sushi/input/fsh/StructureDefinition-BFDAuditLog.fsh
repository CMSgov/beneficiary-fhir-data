Logical: BFDAuditLog
Id: BFDAuditLog
Title: "IDR BFDAuditLog Base. Ignore in DD Generation"
Description: "IDR BFDAuditLog Base. Ignore in DD Generation"
* ^url = "http://hl7.org/fhir/StructureDefinition/BFDAuditLog"
* ^name = "BFDAuditLog"
* ^status = #draft
* ^abstract = false
* ^type = "BFDAuditLog"
* ^baseDefinition = "http://hl7.org/fhir/StructureDefinition/Base"
* . ^label = "IDR BFDAuditLog Base. Ignore in DD Generation"

* matchedBeneSk 1..1 string "Matched Beneficiary FHIR ID" "The FHIR ID of the beneficiary that was matched."
* purposeOfEvent 1..1 string "Purpose of Event" "The purpose of the audit event (e.g., PATRQT)."
* timeStamp 1..1 dateTime "Timestamp" "The time the audit event occurred."
* clientIp 1..1 string "Client IP" "The IP address of the client."
* clientName 1..1 string "Client Name" "The name of the client application."
* finalDetermination 1..1 string "Final Determination" "The combination used to determine the unique match."

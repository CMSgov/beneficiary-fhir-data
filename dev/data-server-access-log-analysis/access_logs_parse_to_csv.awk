#! /usr/bin/gawk -f

BEGIN {
  # Set the field pattern to handle the dumb quoted and bracketed fields thing going on.
  FPAT = "(\"[^\"]+\")|(\\[[^\\]]+\\])|([^ ]+)"

  # Build a map of short-month-names (e.g. "Jan") to left-padded month-numbers (e.g. "01").
  split("Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec", monthShortNames, ",")
  for(monthNum = 1; monthNum <= 12; monthNum++) {
    monthNumsByShortName[monthShortNames[monthNum]] = sprintf("%02d", monthNum)
  }
}

{
  ##
  # Workaround https://jira.cms.gov/browse/BLUEBUTTON-1141, where the
  # remoteAuthenticatedUser field is not properly quoted for many old records.
  ##
  if ($3 ~ /^EMAILADDRESS=/) {
    # Prepend a quote to remoteAuthenticatedUser.
    $3 = sprintf("\"%s", $3)

    # Chomp until we hit the timestamp field, appending the fields to $3.
    while ($4 !~ /^\[/) {
      # Append the next field to remoteAuthenticatedUser.
      $3 = sprintf("%s %s", $3, $4)

      # Shift all the fields left.
      for (i = 4; i <= NF; i++) {
        nextField = i + 1
        if (nextField <= NF) {
          $i = $nextField
        }
      }
    }

    # Append a quote to remoteAuthenticatedUser.
    $3 = sprintf("%s\"", $3)
  }

  ##
  # Assign all of the fields a human friendly name.
  ##
  remoteHostname = $1
  remoteLogicalUsername = $2
  remoteAuthenticatedUser = $3
  timestampRaw = $4
  request = $5
  queryString = $6
  statusCode = $7
  bytes = $8
  durationMilliseconds = $9
  originalQueryId = $10
  originalQueryCounter = $11
  originalQueryTimestampRaw = $12
  developerId = $13
  developerName = $14
  applicationId = $15
  applicationName = $16
  userId = $17
  userName = $18
  beneficiaryId = $19

  ##
  # Parse out the timestamp into its pieces, then glue them back together in ISO 8601 format, e.g. '2018-12-18T03:33:46+00:00'.
  # Timestamps come in as timestampRaw = '[01/Dec/2018:00:00:04 -0500]'.
  ##

  # Strip out the leading and trailing bracket.
  gsub(/\[/, "", timestampRaw)
  gsub(/\]/, "", timestampRaw)

  # Split by ' ' to break off the timestamp offset at the end.
  split(timestampRaw, timestampTokensBySpace, " ")

  # Split by ':' to break into combined-date and separate-time tokens.
  split(timestampTokensBySpace[1], timestampTokensByColon, ":")

  # Split the combined-date into its pieces (by '/').
  split(timestampTokensByColon[1], dateTokens, "/")

  # Add a colon to the timezone offset (some versions of SQLite require this).
  timezoneOffsetStart = substr(timestampTokensBySpace[2], 1, 3)
  timezoneOffsetEnd = substr(timestampTokensBySpace[2], 4)

  # Assign all the pieces.
  year = dateTokens[3]
  month = monthNumsByShortName[dateTokens[2]]
  day = dateTokens[1]
  hour = timestampTokensByColon[2]
  minute = timestampTokensByColon[3]
  second = timestampTokensByColon[4]
  timezoneOffset = sprintf("%s:%s", timezoneOffsetStart, timezoneOffsetEnd)

  # Combine all the pieces back together correctly.
  timestamp = sprintf("%s-%s-%sT%s:%s:%s%s", year, month, day, hour, minute, second, timezoneOffset)

  ##
  # Other miscellaneous data cleaning.
  ##

  # Remove the leading double-question-mark in the query string.
  gsub(/^"\?\?/, "?", queryString)

  # Re-format originalQueryTimestampRaw, which comes in looking like "[2018-12-01 05:07:20.370803]" or "[-]", to ISO 8601.
  gsub(/\[/, "", originalQueryTimestampRaw)
  gsub(/\]/, "", originalQueryTimestampRaw)
  originalQueryTimestampTokensCount = split(originalQueryTimestampRaw, originalQueryTimestampTokens, " ")
  if (originalQueryTimestampTokensCount == 2) {
    originalQueryTimestamp = sprintf("%sT%sZ", originalQueryTimestampTokens[1], originalQueryTimestampTokens[2])
  } else {
    originalQueryTimestamp = ""
  }

  # Convert "-" to empty fields, where appropriate.
  gsub(/^-$/, "", remoteLogicalUsername)
  gsub(/^-$/, "", remoteAuthenticatedUser)
  gsub(/^-$/, "", originalQueryId)
  gsub(/^-$/, "", originalQueryCounter)
  gsub(/^-$/, "", originalQueryTimestamp)
  gsub(/^-$/, "", developerId)
  gsub(/^"-"$/, "", developerName)
  gsub(/^-$/, "", applicationId)
  gsub(/^"-"$/, "", applicationName)
  gsub(/^-$/, "", userId)
  gsub(/^"-"$/, "", userName)
  gsub(/^-$/, "", beneficiaryId)

  ##
  # Grand Finale!
  ##

  # Print out the whole record.
  printf "%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", localHostname, remoteHostname, remoteLogicalUsername, remoteAuthenticatedUser, timestamp, request, queryString, statusCode, bytes, durationMilliseconds, originalQueryId, originalQueryCounter, originalQueryTimestamp, developerId, developerName, applicationId, applicationName, userId, userName, beneficiaryId
}

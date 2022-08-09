
This script randomly generates and encrypts (one way salted hash) a random database password. 

The output includes both the encrypted and non-ecrypted password, the latter of which should be sent to a database administrator so they can update the account. The user should save the unencrypted password to their password manager of choice.

## Quickstart
1. Get the script from BFD's github repo or from an adminstrator.
2. Open a terminal and run the script to generate a password.

```
$ cd ops/ccs-ops-misc/pg-password-generator
$ python3 pgpass-gen.py
 Generating random password..      [OK]
 Validating password complexity..  [OK]

 Instructions:
  1. Store your new password in a password manager of choice (keep it safe!):
      YOUR NEW PASSWORD: SomeSuperSecretPassword

  2. Send the following hashed password to your db admin so they can update your account (ask them how!):

   'SCRAM-SHA-256$...SomeVeryLongStringOfRandomChars...'
```
## Crypto notes

The password is salted and hashed many times to make brute-forcing realistically impossible (currently anyway), assuming the password is of suffecient length and complexity before being hashed- the purpose of this script.

Theoritically, an attacker could take the hashed password and attempt to build a set of rainbow tables to brute force; but currently, this would take many years. That said, the hashed password should not be made public and shared using appropriate TLS enrypted channels such as Box, private slack message, etc.

Ask your db admin how they prefer it being sent.

## Notes for database admins

You apply a SCRAM encrypted password the same way you would apply a plain text password, just sub the plaintext password with the long `SCRAM..` string. Ie:

```sql
ALTER ROLE joe_user WITH LOGIN PASSWORD 'SCRAM-SHA-256$4096:QvFQ9c8S...==$VonRpO5K9...nENlN0=:mdAL8...ArF/Ufy4n...bQyc=';
```

But don't forget to set or extend the users password expiration date while updating their creds. Ie:

```sql
DO $$
DECLARE
  users_name TEXT := 'foobar'; -- CHANGE ME (ie firstname_lastname)
  users_scram TEXT := 'SCRAM-SHA-256$4096:QvFQ9c8S...==$VonRpO5K9...nENlN0=:mdAL8...ArF/Ufy4n...bQyc='; -- AND ME TOO
  valid_until timestamp := current_timestamp + interval '60 days'; -- NUMBER OF DAYS THEY NEED ACCESS (NOT LONGER THAN 60 DAYS IN PROD!)
BEGIN
  EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L VALID UNTIL %L;', users_name, users_scram, valid_until);
END $$ LANGUAGE plpgsql
```

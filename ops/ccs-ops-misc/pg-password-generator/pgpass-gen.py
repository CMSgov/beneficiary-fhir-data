from base64 import standard_b64encode
from hashlib import pbkdf2_hmac, sha256
from os import urandom
import hmac
import sys
import string
import random
import re


# special characters allowed in the password
# - RDS does not like /, `, or @ (there may be others depending on the age of the db driver)
allowed_specials = "~!%#^&][)(}{_><+=.;?|"

# lists of available characters
uppers = list(string.ascii_uppercase)
lowers = list(string.ascii_lowercase)
digits = list(string.digits)
specials = list(allowed_specials)
characters = uppers + lowers + digits + specials

# regex pattern to match a valid password (using regex lookaheads (?=...))
# contains at least two upper chars: (?=(?:.*[A-Z]){2})
# contains at least two lower chars: (?=(?:.*[a-z]){2})
# contains at least two numbers: (?=(?:.*[0-9]){2})
# contains at least 2 specials: (?=(?:.*[{allowed_specials}]){2})'
# and at least min_length long: .{min_length,}
min_uppers = 2
min_lowers = 2
min_numbers = 2
min_specials = 2
min_length = 21
password_re = f'^(?=(?:.*[a-z]){{{min_lowers}}})(?=(?:.*[A-Z]){{{min_uppers}}})(?=(?:.*[0-9]){{{min_numbers}}})(?=(?:.*[{re.escape(allowed_specials)}]){{{min_specials}}}).{{{min_length},}}$'


# scram hash settings (gleaned from https://github.com/postgres/postgres/blob/07044efe00762bdd04c4d392adb8f6425b13369b/src/include/common/scram-common.h#L35)
salt_size = 16
digest_len = 32
iterations = 4096


# generate a random password
def generate_random_password(length: int = min_length):

    # initialize an empty password
    password = []

    # add the minimum number of random uppers
    for i in range(min_uppers):
        password.append(random.choice(uppers))

    # and lowers
    for i in range(min_lowers):
        password.append(random.choice(lowers))

    # and numbers
    for i in range(min_numbers):
        password.append(random.choice(digits))

    # and specials
    for i in range(min_specials):
        password.append(random.choice(specials))

    # fill the rest with random characters until the password is of appropriate length
    if len(password) < length:
        random.shuffle(characters)
        for i in range(length - len(password)):
            password.append(random.choice(characters))

    # shuffle until the password does not start with, end with, or contain two consecutive special characters
    # (?!...) is a negative lookahead
    match_re = f'^[a-zA-Z0-9](?!.*[{re.escape(allowed_specials)}]{{2}}).*[a-zA-Z0-9]$'
    match = None
    while match is None:
        random.shuffle(password)
        match = re.search(match_re, ''.join(password))

    return ''.join(password)

# prep the string


def b64enc(b: bytes) -> str:
    return standard_b64encode(b).decode('utf8')

# scram it


def pg_scram_sha256(passwd: str) -> str:
    salt = urandom(salt_size)
    digest_key = pbkdf2_hmac('sha256', passwd.encode('utf8'), salt, iterations,
                             digest_len)
    client_key = hmac.digest(digest_key, 'Client Key'.encode('utf8'), 'sha256')
    stored_key = sha256(client_key).digest()
    server_key = hmac.digest(digest_key, 'Server Key'.encode('utf8'), 'sha256')
    return (
        f'SCRAM-SHA-256${iterations}:{b64enc(salt)}${b64enc(stored_key)}:{b64enc(server_key)}'
    )


def main():
    # parse args
    args = sys.argv[1:]
    if args and len(args) > 0:
        print("pgpass-gen.py takes no arguments")
        sys.exit(1)

    # generate a random password
    print("Generating random password..", end='  ')
    passwd = generate_random_password()
    if not passwd:
        print('[ERR]')
        sys.exit(1)
    print("    [OK]")

    # validate the password before continuing
    print('Validating password complexity..', end='  ')
    match = re.search(password_re, passwd)
    if match:
        print("[OK]\n")
    else:
        print(f"[FAIL]\n'{passwd}' is an invalid password.")
        sys.exit(1)

    # scram it
    encrypted_password = pg_scram_sha256(passwd)
    if not encrypted_password:
        print("Error encrypting password using SCRAM-SHA-256")
        sys.exit(1)

    # print instructions
    print('Instructions:')
    print("  1. Store your new password in a password manager of choice (keep it safe!):")
    print(f"      YOUR NEW PASSWORD: {passwd}", end='\n\n')
    print("  2. Send the following hashed password to your db admin so they can update your account (ask them how!):")
    print(f"\n   '{encrypted_password}'\n")


if __name__ == "__main__":
    main()

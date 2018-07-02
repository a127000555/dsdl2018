import socket
import sys
import select
import pexpect
import json
import subprocess
def get_address():
    get_my_address = subprocess.check_output('./bitcoin-cli getnewaddress', shell = True)
    my_address = get_my_address.decode('ascii')
    #print('my address :',my_address)
    my_address = my_address.replace('\n','')
    return my_address
def get_raw_trans(raw):
    get_my_address = pexpect.spawn('bitcoind decoderawtransaction '+raw)
    get_my_address.expect(pexpect.EOF)
    my_address = get_my_address.before.decode('ascii')
    my_address = json.loads(my_address)
    print('my address :',my_address)
    get_my_address.close()
    return my_address
def sign(raw, string):
    get_my_address = pexpect.spawn('bitcoind signrawtransaction '+raw+' '+string)
    get_my_address.expect(pexpect.EOF)
    my_address = get_my_address.before.decode('ascii')
    print('my address :',my_address)
    get_my_address.close()
    return my_address

def main():
    my_address = get_address()
    print(my_address)

if __name__ == "__main__":
    main()

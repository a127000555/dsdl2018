import socket
import sys
import select
import json
import subprocess
def new_client(sock_fd):
    client_socket, address = sock_fd.accept()
    print('new connection from',address)
    #print(client_socket.fileno())
    return client_socket, address
def start_sending(client_fd):
    address = client_fd.recv(1024)
    address = address.decode('ascii')
    print('target address :',address)
    ind = 0
    for i in range(len(address)):
        if address[i] == '~':
            ind = i
    address = address[ind+1:]
    print(address)

    money = '1'

    get_send_id = subprocess.check_output('./bitcoin-cli sendtoaddress '+address+' '+money, shell = True)
    send_id = get_send_id.decode('ascii')
    print('send_id :',send_id)
    send_id = send_id.replace('\n','')

    get_raw_trans = subprocess.check_output('./bitcoin-cli getrawtransaction '+send_id+' 1', shell = True)
    raw_trans = get_raw_trans.decode('ascii')
    print('raw_tras :',raw_trans)
    raw_trans = json.loads(raw_trans)

    get_my_address = subprocess.check_output('./bitcoin-cli getnewaddress', shell = True)
    my_address = get_my_address.decode('ascii')
    print('my address :',my_address)
    my_address = my_address.replace('\n','')

    first = [{"txid":raw_trans['txid'],"vout":raw_trans["vin"][0]["vout"]}]
    second = {my_address:int(money)}
    script_key = raw_trans['vout'][0]['scriptPubKey']['hex']
    print('first para : {}'.format(first))
    print('second para : {}'.format(second))
    print('scriptPubKey : {}'.format(script_key))

    create_raw_trans = subprocess.check_output('./bitcoin-cli createrawtransaction '+"'"+str(first).replace(' ','').replace("'",'"')+"'"+' '+"'"+str(second).replace(' ','').replace("'",'"')+"'", shell = True)
    real_raw_trans = create_raw_trans.decode('ascii')
    real_raw_trans = real_raw_trans.replace('\n','')
    ret = {'key':script_key, 'raw_trans':real_raw_trans, 'txid':raw_trans['txid'], 'vout':raw_trans['vin'][0]["vout"]}
    print('return : {}'.format(ret))

    ret = (str(ret)+'~').encode('ascii')
    client_fd.send(ret)
def end_sending(client):
    signed_trans = client.recv(2048)
    print(signed_trans)
    signed_trans = signed_trans[5:]
    signed_trans = signed_trans.decode('ascii')
    signed_trans = signed_trans.replace('\n','').replace('~','')
    print(signed_trans)

    get_txid = subprocess.check_output('./bitcoin-cli sendrawtransaction '+signed_trans, shell = True)
    txid = get_txid.decode('ascii')

    print('txid :',txid)
    client.send('Done'.encode('ascii'))
    return

def main(argv):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print(argv[1])
    sock.bind((argv[1],int(argv[2])))
    sock.listen(100)
    fd_list = [sock,sys.stdin]
    status = 0
    loop = True
    while loop:
        readable, writable, errored = select.select(fd_list, [], [])
        for fd in readable:
            if fd is sock:
                client_fd, target_ip = new_client(sock)
                #if target_ip not in status.keys():
                #    status[target_ip] = 0
                print('Start task : {}'.format(client_fd))
                #print('status  {}'.format(status[target_ip]))
                if status == 0:
                    start_sending(client_fd)
                else:
                    end_sending(client_fd)
                status = (status+1)%2
                print('End task : {}'.format(client_fd))
                client_fd.close()
            elif fd is sys.stdin:
                s = sys.stdin.readline()
                if s == "quit\n":
                    loop = False
    sock.close()

if __name__ == "__main__":
    main(sys.argv)


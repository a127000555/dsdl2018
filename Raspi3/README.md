# Raspi3 Section
---
In this directory, we will show you all codes we programed on raspi3.

Here we provide a step-by-step tutorial on how to build up your raspi3 cold wallet BLE server.

## Part 1 Bitcoin Setup

We assume that tou have successfully install raspian OS on your raspi3 and can ssh into the machine.

Because we want to let our raspi3 to be a cold wallet and sign transaction, first we need to build up bitcoin on raspi3. Although [Bitcoin Official Repo](https://github.com/bitcoin/bitcoin) has a detailed explanation on how to install it (at `doc` directory). However, there isn't any instruction for raspian. 

### Installation

1. Install Some Needed Package
```
sudo apt-get install build-essential autoconf automake libssl-dev libboost-dev
sudo apt-get install libboost-chrono-dev libboost-filesystem-dev
sudo apt-get install libboost-program-options-dev libboost-system-dev 
sudo apt-get install libboost-test-dev libboost-thread-dev libtool
sudo apt-get install libtool autotools-dev pkg-config libevent-dev bsdmainutils 
```

2. Because `libdb4.8-dev libdb4.8++-dev` can't be easily installed through `apt`, thus, you should download it and compile it yourself.
```
wget http://download.oracle.com/berkeley-db/db-4.8.30.NC.tar.gz
sudo tar -xzvf db-4.8.30.NC.tar.gz
cd db-4.8.30.NC/build_unix
../dist/configure --enable-cxx
make -j4
sudo make install
```

3. Install Bitcoin
```
git clone https://github.com/bitcoin/bitcoin.git
cd bitcoin
./autogen
./configure --without-gui CXXFLAGS="--param ggc-min-expand=1 --param ggc-min-heapsize=32768" CPPFLAGS="-I/usr/local/BerkeleyDB.4.8/include -O2" LDFLAGS="-L/usr/local/BerkeleyDB.4.8/lib" --with-libressl
make
sudo make install
```

Now you have successfully install the bitcoin latest version on raspi3.

### Bitcoin Usage

For conveniently, there is a directory `testnet` in this repo, which contains a simple and easy shell script for our program to call bitcoin rpc. You can see it yourself. 

## Part 2 BLE server

The second part of building raspi3 cold wallet is to setup BLE server. Here we use `pybluez` package and hands on coding =)

### Useful command line

Due to the usage of BLE, we will use some linux command:

```
# Setup Service
sudo systemctl restart bluetooth.service
sudo pulseaudio --start # set up for android 
# BLE interface setting
sudo hciconfig hci0 piscan
sudo hciconfig hci0 leadv0  # advertisement
```

### Pybluez 

Codes are in the `xble_server.py` code. (the server code should put in the same directory with `bitcoin.conf`). We write our own service and characteristic.

In `class TestCharacteristic(Characteristic):`, two important methods `ReadValue, WriteValue` will deal with the iteraction with smartphone. The smartphone can write request to raspi3 and read value from raspi3.

Three important functions are `sign` and `get_raw_trans` and `get_address`. The will fork a subprocess and call `bitcoin-cli` rpc calls to signrawtransaction, decoderawtransaction, and getnewaddress respectively.

Noted that we also need to run `advertise.py` to keep our raspi3 advertising.

## Part 3 Other Issues

### Daemon Process

For the sake of build a cold wallet, we need to make all process as daemon process. If you want to set your systemd, you should edit one file in `/etc/systemd/system`. For example, we should edit one file:

```
[Unit]
Description=Raspberry PI Bluetooth Server
After=bluetooth.target
 
[Service]
Type=simple
User=root
Group=root
WorkingDirectory=home/pi/testnet/a
ExecStart=home/pi/testnet/a/xble_server.py -l home/pi/testnet/a/xble.log
 
[Install]
WantedBy=multi-user.target
```

Then, we can type `systemctl start [service-name].service` and enable it.

### Security

For the sake of security, we should disable some port, such as `ssh`.

```
sudo iptables -A INPUT -p tcp --dport 22 -j DROP
sudo iptables-save
```

However, it is not convenience for developers, like us, to develop service, thus we recommend you to add your ip to whitelist using iptables =)


#!/usr/bin/env python3

import dbus
import dbus.exceptions
import dbus.mainloop.glib
import dbus.service
import binascii
import array
try:
  from gi.repository import GObject
except ImportError:
  import gobject as GObject
import sys, os
import subprocess
import json
from random import randint

mainloop = None

BLUEZ_SERVICE_NAME = 'org.bluez'
GATT_MANAGER_IFACE = 'org.bluez.GattManager1'
DBUS_OM_IFACE =      'org.freedesktop.DBus.ObjectManager'
DBUS_PROP_IFACE =    'org.freedesktop.DBus.Properties'

GATT_SERVICE_IFACE = 'org.bluez.GattService1'
GATT_CHRC_IFACE =    'org.bluez.GattCharacteristic1'
GATT_DESC_IFACE =    'org.bluez.GattDescriptor1'

def get_address():
    get_my_address = subprocess.check_output('/home/pi/testnet/a/bitcoin-cli getnewaddress', shell = True)
    my_address = get_my_address.decode('ascii')
    #print('my address :',my_address)
    my_address = my_address.replace('\n','')
    return my_address

class InvalidArgsException(dbus.exceptions.DBusException):
    _dbus_error_name = 'org.freedesktop.DBus.Error.InvalidArgs'

class NotSupportedException(dbus.exceptions.DBusException):
    _dbus_error_name = 'org.bluez.Error.NotSupported'

class NotPermittedException(dbus.exceptions.DBusException):
    _dbus_error_name = 'org.bluez.Error.NotPermitted'

class InvalidValueLengthException(dbus.exceptions.DBusException):
    _dbus_error_name = 'org.bluez.Error.InvalidValueLength'

class FailedException(dbus.exceptions.DBusException):
    _dbus_error_name = 'org.bluez.Error.Failed'


class Application(dbus.service.Object):
    """
    org.bluez.GattApplication1 interface implementation
    """
    def __init__(self, bus):
        self.path = '/'
        self.services = []
        dbus.service.Object.__init__(self, bus, self.path)
        # self.add_service(HeartRateService(bus, 0))
        # self.add_service(BatteryService(bus, 1))
        self.add_service(TestService(bus, 2))

    def get_path(self):
        return dbus.ObjectPath(self.path)

    def add_service(self, service):
        self.services.append(service)

    @dbus.service.method(DBUS_OM_IFACE, out_signature='a{oa{sa{sv}}}')
    def GetManagedObjects(self):
        response = {}
        print('GetManagedObjects')

        for service in self.services:
            response[service.get_path()] = service.get_properties()
            chrcs = service.get_characteristics()
            for chrc in chrcs:
                response[chrc.get_path()] = chrc.get_properties()
                descs = chrc.get_descriptors()
                for desc in descs:
                    response[desc.get_path()] = desc.get_properties()

        return response


class Service(dbus.service.Object):
    """
    org.bluez.GattService1 interface implementation
    """
    PATH_BASE = '/org/bluez/example/service'

    def __init__(self, bus, index, uuid, primary):
        self.path = self.PATH_BASE + str(index)
        self.bus = bus
        self.uuid = uuid
        self.primary = primary
        self.characteristics = []
        dbus.service.Object.__init__(self, bus, self.path)

    def get_properties(self):
        return {
                GATT_SERVICE_IFACE: {
                        'UUID': self.uuid,
                        'Primary': self.primary,
                        'Characteristics': dbus.Array(
                                self.get_characteristic_paths(),
                                signature='o')
                }
        }

    def get_path(self):
        return dbus.ObjectPath(self.path)

    def add_characteristic(self, characteristic):
        self.characteristics.append(characteristic)

    def get_characteristic_paths(self):
        result = []
        for chrc in self.characteristics:
            result.append(chrc.get_path())
        return result

    def get_characteristics(self):
        return self.characteristics

    @dbus.service.method(DBUS_PROP_IFACE,
                         in_signature='s',
                         out_signature='a{sv}')
    def GetAll(self, interface):
        if interface != GATT_SERVICE_IFACE:
            raise InvalidArgsException()

        return self.get_properties()[GATT_SERVICE_IFACE]


class Characteristic(dbus.service.Object):
    """
    org.bluez.GattCharacteristic1 interface implementation
    """
    def __init__(self, bus, index, uuid, flags, service):
        self.path = service.path + '/char' + str(index)
        self.bus = bus
        self.uuid = uuid
        self.service = service
        self.flags = flags
        self.descriptors = []
        dbus.service.Object.__init__(self, bus, self.path)

    def get_properties(self):
        return {
                GATT_CHRC_IFACE: {
                        'Service': self.service.get_path(),
                        'UUID': self.uuid,
                        'Flags': self.flags,
                        'Descriptors': dbus.Array(
                                self.get_descriptor_paths(),
                                signature='o')
                }
        }

    def get_path(self):
        return dbus.ObjectPath(self.path)

    def add_descriptor(self, descriptor):
        self.descriptors.append(descriptor)

    def get_descriptor_paths(self):
        result = []
        for desc in self.descriptors:
            result.append(desc.get_path())
        return result

    def get_descriptors(self):
        return self.descriptors

    @dbus.service.method(DBUS_PROP_IFACE,
                         in_signature='s',
                         out_signature='a{sv}')
    def GetAll(self, interface):
        if interface != GATT_CHRC_IFACE:
            raise InvalidArgsException()

        return self.get_properties()[GATT_CHRC_IFACE]

    @dbus.service.method(GATT_CHRC_IFACE,
                        in_signature='a{sv}',
                        out_signature='ay')
    def ReadValue(self, options):
        print('Default ReadValue called, returning error')
        raise NotSupportedException()

    @dbus.service.method(GATT_CHRC_IFACE, in_signature='aya{sv}')
    def WriteValue(self, value, options):
        print('Default WriteValue called, returning error')
        raise NotSupportedException()

    @dbus.service.method(GATT_CHRC_IFACE)
    def StartNotify(self):
        print('Default StartNotify called, returning error')
        raise NotSupportedException()

    @dbus.service.method(GATT_CHRC_IFACE)
    def StopNotify(self):
        print('Default StopNotify called, returning error')
        raise NotSupportedException()

    @dbus.service.signal(DBUS_PROP_IFACE,
                         signature='sa{sv}as')
    def PropertiesChanged(self, interface, changed, invalidated):
        pass


class Descriptor(dbus.service.Object):
    """
    org.bluez.GattDescriptor1 interface implementation
    """
    def __init__(self, bus, index, uuid, flags, characteristic):
        self.path = characteristic.path + '/desc' + str(index)
        self.bus = bus
        self.uuid = uuid
        self.flags = flags
        self.chrc = characteristic
        dbus.service.Object.__init__(self, bus, self.path)

    def get_properties(self):
        return {
                GATT_DESC_IFACE: {
                        'Characteristic': self.chrc.get_path(),
                        'UUID': self.uuid,
                        'Flags': self.flags,
                }
        }

    def get_path(self):
        return dbus.ObjectPath(self.path)

    @dbus.service.method(DBUS_PROP_IFACE,
                         in_signature='s',
                         out_signature='a{sv}')
    def GetAll(self, interface):
        if interface != GATT_DESC_IFACE:
            raise InvalidArgsException()

        return self.get_properties()[GATT_DESC_IFACE]

    @dbus.service.method(GATT_DESC_IFACE,
                        in_signature='a{sv}',
                        out_signature='ay')
    def ReadValue(self, options):
        print ('Default ReadValue called, returning error')
        raise NotSupportedException()

    @dbus.service.method(GATT_DESC_IFACE, in_signature='aya{sv}')
    def WriteValue(self, value, options):
        print('Default WriteValue called, returning error')
        raise NotSupportedException()

def get_raw_trans(raw):
    get_my_address = subprocess.check_output('/home/pi/testnet/a/bitcoin-cli decoderawtransaction '+raw, shell = True)
    my_address = get_my_address.decode('ascii')
    print(my_address)
    my_address = json.loads(my_address)
    print('decode :',my_address)
    return my_address
def sign(raw, string):
    get_my_address = subprocess.check_output('/home/pi/testnet/a/bitcoin-cli signrawtransaction '+raw+" '"+string.replace(' ','').replace("'",'"')+"'", shell = True)
    my_address = get_my_address.decode('ascii')
    print('signed :',my_address)
    s = json.loads(my_address)
    return s['hex']
class TestCharacteristic(Characteristic):
    '''
    Characteristic

    '''
    TEST_CHRC_UUID = '12345678-1234-5678-1234-56789abcdef1'
    def __init__(self, bus, index, service):
        Characteristic.__init__(self, bus, index, self.TEST_CHRC_UUID, ['read', 'write', 'writable-auxiliaries'], service)
        self.value = array.array('B', b'FLAG{c0n9r47u1a710n}')
        self.flag = 0
        self.address_flag = 0
        self.address = None
        self.get_dict = ''
        self.signed = None
        self.sign_flag = 0
    def ReadValue(self, options):
        if self.address:
            if self.address_flag == 0:
                self.value = self.address[:16].encode()
                self.address_flag+=1
            else:
                self.value = self.address[16:].encode()
                self.address = None
                self.address_flag = 0
        if self.signed:
            length = len(self.signed)
            tmp = None
            if self.sign_flag + 16 > length:
                tmp = self.signed[self.sign_flag:].encode()
                self.sign_flag = 0
                self.signed = None
                self.value = tmp
            else:
                tmp = self.signed[self.sign_flag:self.sign_flag+16].encode()
                self.sign_flag+=16
                self.value = tmp
        print(repr(self.value))
        # return None
        return self.value
    def WriteValue(self, value, options):
        print('TestCharacteristic Write: ' + repr(value))
        print("value =", value)
        lst = [chr(s) for s in value]
        s = "".join(lst)
        print ("s = ", s)
        if "get wallet address" in s:
            print('start')
            my_address = get_address()
            print(my_address)
            # add = binascii.unhexlify(my_address)
            # print(add)
            self.value = my_address
            self.address = my_address
            # print (self.value)
        else:
            if '{' in s:
                self.get_dict = self.get_dict + s
            else:
                self.get_dict = self.get_dict + s[4:]
            print(self.get_dict)
            if '}' in self.get_dict:
                #self.get_dict = self.get_dict[:self.get_dict.find('}')+1]
                print(type(self.get_dict))
                self.get_dict = self.get_dict.replace("'",'"')
                print('get all',self.get_dict)
                try:
                    d = json.loads(self.get_dict)
                except:
                    print(self.get_dict.encode('ascii'))
                print(d)
                print(type(d))
                string = [{"txid":d['txid'], "vout":d['vout'], "scriptPubKey":d['key']}]
                hexi = sign(d['raw_trans'], str(string))
                self.signed = hexi
                print('signed!!!!',self.signed)

        self.flag = 1
        #self.value = value

class TestService(Service):
    """
    Dummy test service that provides characteristics and descriptors that
    exercise various API functionality.
    """
    TEST_SVC_UUID = '12345678-1234-5678-1234-56789abcdef0'

    def __init__(self, bus, index):
        Service.__init__(self, bus, index, self.TEST_SVC_UUID, True)
        self.add_characteristic(TestCharacteristic(bus, 0, self))

def register_app_cb():
    print('GATT application registered')


def register_app_error_cb(error):
    print('Failed to register application: ' + str(error))
    mainloop.quit()


def find_adapter(bus):
    remote_om = dbus.Interface(bus.get_object(BLUEZ_SERVICE_NAME, '/'),
                               DBUS_OM_IFACE)
    objects = remote_om.GetManagedObjects()

    for o, props in objects.items():
        if GATT_MANAGER_IFACE in props.keys():
            return o

    return None

def main():
    # init some service
    os.system("sudo systemctl restart bluetooth.service")
    os.system("pulseaudio --start")
    os.system("sudo hciconfig hci0 piscan")
    os.system("sudo hciconfig hci0 leadv0")

    global mainloop

    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)

    bus = dbus.SystemBus()

    adapter = find_adapter(bus)
    if not adapter:
        print('GattManager1 interface not found')
        return

    service_manager = dbus.Interface(
            bus.get_object(BLUEZ_SERVICE_NAME, adapter),
            GATT_MANAGER_IFACE)

    app = Application(bus)

    mainloop = GObject.MainLoop()

    print('Registering GATT application...')

    service_manager.RegisterApplication(app.get_path(), {},
                                    reply_handler=register_app_cb,
                                    error_handler=register_app_error_cb)

    mainloop.run()

if __name__ == '__main__':
    main()

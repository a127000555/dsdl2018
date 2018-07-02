import os
import time

while(1):
    os.system("sudo hciconfig hci0 leadv0")
    time.sleep(10)


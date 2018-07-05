# report
This report is talking about the contribution of each team member, and the problem we had encountered.
If you want to know the details of each part of our work, please follow the instructions in the README.md.

## Contribution of each team memeber
The following things are the contribution of each team memeber:
- B05902013: Set up raspi3 and build the connection, and build up raspi3 BLE server.
- B05902021: Set up raspi3 and build the connection, and build up raspi3 BLE server.
- B05902031: Set up raspi3 and build the connection, and build the bitcoin server which can communicate with the mobile phone.
- B05902127: Set up raspi3 and build the connection, and design the Android app.

## The problem we had encountered
- We can only connect success in some Android version, and this is not the problem that we can solve. Therefore, the method we use is to find a mobile phone that can connect to raspi3 successfully.
- When sending message to the mobile phone, if the message length is too long, the phone can't receive the message successfully. Our solution is seperate the message into small piece, and then send to the mobile phone respectively.

package com.arvin.dsdl;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

class Webclient {
    static String rawTransaction = "";
    public static String throwMessageToWeb(final int port , final String message ) throws IOException {
        new Thread() {
            @Override
            public void run() {
                String host = "140.112.90.167";
                Socket socket = null;
                try {
                    socket = new Socket(host, port);
                    BufferedInputStream input = null;
                    DataOutputStream output = null;

                    try {
//                        input = new DataInputStream( socket.getInputStream() );
                        input = new java.io.BufferedInputStream(socket.getInputStream());
                        output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF("~~~~~~~~~~~~~~"+ message);
//                        rawTransaction = input.readUTF();

                        byte[] b = new byte[1024];
                        rawTransaction = "";
                        int length;
                        while ((length = input.read(b)) > 0)// <=0的話就是結束了
                        {
                            rawTransaction += new String(b, 0, length);
                        }

                        Log.e("web", "get " + rawTransaction + "from web.");
                    } catch (IOException e) {
                    } finally {
                        if (input != null)
                            input.close();
                        if (output != null)
                            output.close();
                    }
                } catch (IOException e) {
                    Log.e("web", e.toString());

                    e.printStackTrace();
                } finally {
                    try {
                        if (socket != null)
                            socket.close();
                    } catch (IOException e) {
                        Log.e("web", e.toString());
                    }
                }


            }
        }.start();
        while (rawTransaction.length() == 0) {

        }
            return rawTransaction.substring(0, rawTransaction.length() - 1);
    }
}

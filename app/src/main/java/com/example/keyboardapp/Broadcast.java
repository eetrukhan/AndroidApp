package com.example.keyboardapp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Broadcast {
    private static final String LOG_TAG = "BROADCAST";

    static void recieveBroadcast()
    {
        try {
            DatagramSocket socket = new DatagramSocket(9876, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (Constants.HOST == null) {
                Log.i(LOG_TAG,"Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                Log.i(LOG_TAG, "Packet received from: " + packet.getAddress().getHostAddress());

                Constants.HOST = packet.getAddress().getHostAddress();
            }
        } catch (IOException ex) {
            Log.i(LOG_TAG, "" + ex.getMessage());
        }
    }
}

package com.example.keyboardapp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

class Broadcast {
    private final String LOG_TAG = "Broadcast";

    private int mPort;
    private volatile String host = null;

    private volatile boolean packetReceived = false;


    Broadcast(int port) {
        mPort = port;
    }

    void recieveBroadcast() {
        packetReceived = false;
        host = null;

        try {
            DatagramSocket socket = new DatagramSocket(mPort, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            Log.i(LOG_TAG, "Waiting for packets . . .");

            byte[] recvBuf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);

            Log.i(LOG_TAG, "Packet received from: " + packet.getAddress().getHostAddress());
            host = packet.getAddress().getHostAddress();
            packetReceived = true;
        }
        catch (IOException e)
        {
            Log.i(LOG_TAG, e.getMessage() == null ? "Receiving error." : e.getMessage());
        }
    }

    boolean isPacketReceived() {
        return packetReceived;
    }

    String getHost() {
        return host;
    }
}

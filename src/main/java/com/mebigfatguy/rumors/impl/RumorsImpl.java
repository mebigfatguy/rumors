/*
 * rumors - a simple discovery/connection protocol
 * Copyright 2011-2017 MeBigFatGuy.com
 * Copyright 2011-2017 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.rumors.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mebigfatguy.rumors.Endpoint;
import com.mebigfatguy.rumors.Rumors;
import com.mebigfatguy.rumors.RumorsException;
import com.mebigfatguy.rumors.aux.Closer;

public class RumorsImpl implements Rumors {

    private static Logger LOGGER = LoggerFactory.getLogger(RumorsImpl.class);

    private static final String DEFAULT_BROADCAST_IP = "228.229.230.231";
    private static final int DEFAULT_DYNAMIC_PORT = 13531;
    private static final int[] DEFAULT_ANNOUNCE_DELAY = { 100, 5000, 5000, 5000, 60000 };
    private static final int MAX_BUFFERED_ENDPOINTS = 4000 / 40;

    private Endpoint broadcastEndpoint = new Endpoint(DEFAULT_BROADCAST_IP, DEFAULT_DYNAMIC_PORT);
    private int staticPort = 0;
    private List<Endpoint> staticEndpoints = new ArrayList<>();
    private int[] broadcastAnnounce = DEFAULT_ANNOUNCE_DELAY;

    private final Object sync = new Object();
    private Thread dynamicBroadcastThread;
    private Thread dynamicReceiveThread;
    private Thread staticBroadcastThread;
    private Thread staticReceiveThread;
    private boolean running = false;
    private ServerSocket staticDiscoverySocket;
    private MulticastSocket broadcastSocket;
    private ServerSocket messageSocket;

    private final Map<Endpoint, Boolean> knownMessageSockets = new ConcurrentHashMap<>();

    @Override
    public void begin() throws RumorsException {
        synchronized (sync) {
            if (!running) {
                LOGGER.debug("Beginning rumors");
                initializeRumorPorts();
                knownMessageSockets.put(new Endpoint(messageSocket.getInetAddress().getHostAddress(), messageSocket.getLocalPort()), Boolean.TRUE);

                dynamicBroadcastThread = new Thread(new DynamicBroadcastRunnable());
                dynamicBroadcastThread.setName("Rumor Broadcast");
                dynamicBroadcastThread.start();
                dynamicReceiveThread = new Thread(new DynamicReceiveRunnable());
                dynamicReceiveThread.setName("Rumor Receive");
                dynamicReceiveThread.start();

                if (staticPort > 0) {
                    staticReceiveThread = new Thread(new StaticReceiveRunnable());
                    staticReceiveThread.setName("Static Receive");
                    staticReceiveThread.start();
                }

                if (!staticEndpoints.isEmpty()) {
                    staticBroadcastThread = new Thread(new StaticBroadcastRunnable());
                    staticBroadcastThread.setName("Static Discovery");
                    staticBroadcastThread.start();
                }
                running = true;
            }
        }
    }

    @Override
    public void end() {
        synchronized (sync) {
            if (running) {
                try {
                    terminateRumorPorts();

                    dynamicBroadcastThread.interrupt();
                    dynamicReceiveThread.interrupt();
                    if (staticBroadcastThread != null) {
                        staticBroadcastThread.interrupt();
                        staticBroadcastThread.join();
                    }
                    if (staticReceiveThread != null) {
                        staticReceiveThread.interrupt();
                        staticReceiveThread.join();
                    }
                    dynamicBroadcastThread.join();
                    dynamicReceiveThread.join();

                } catch (InterruptedException ie) {
                } finally {
                    dynamicBroadcastThread = null;
                    dynamicReceiveThread = null;
                    staticBroadcastThread = null;
                    staticReceiveThread = null;
                    running = false;
                    LOGGER.debug("Ending Rumors");
                }
            }
        }
    }

    public void setBroadcastEndpoint(Endpoint bcEndpoint) {
        broadcastEndpoint = bcEndpoint;
    }

    public void setStaticPort(int port) {
        staticPort = port;
    }

    public void setPoint2PointEndpoints(List<Endpoint> p2pEndpoints) {
        staticEndpoints = new ArrayList<>(p2pEndpoints);
    }

    public void setBroadcastAnnounceDelay(String value) {
        String[] delays = value.split(",");
        broadcastAnnounce = new int[delays.length];

        int i = 0;
        for (String delay : delays) {
            broadcastAnnounce[i++] = Integer.parseInt(delay);
        }
    }

    private void initializeRumorPorts() throws RumorsException {
        try {
            messageSocket = new ServerSocket();
            messageSocket.bind(null);

            broadcastSocket = new MulticastSocket(broadcastEndpoint.getPort());
            broadcastSocket.joinGroup(InetAddress.getByName(broadcastEndpoint.getIp()));

            if (staticPort > 0) {
                staticDiscoverySocket = new ServerSocket(staticPort);
            }
        } catch (IOException ioe) {
            terminateRumorPorts();
            throw new RumorsException("Failed initializing rumor ports", ioe);
        }

    }

    private void terminateRumorPorts() {
        try {
            Closer.close(messageSocket);

            if (broadcastSocket != null) {
                broadcastSocket.leaveGroup(InetAddress.getByName(broadcastEndpoint.getIp()));
                Closer.close(broadcastSocket);
            }

            if (staticDiscoverySocket != null) {
                Closer.close(staticDiscoverySocket);
            }
        } catch (Exception e) {
        } finally {
            messageSocket = null;
            broadcastSocket = null;
            staticDiscoverySocket = null;
        }
    }

    private byte[] endPointsToBuffer() throws RumorsException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int written = 0;

            for (Endpoint ep : knownMessageSockets.keySet()) {
                if (written >= MAX_BUFFERED_ENDPOINTS) {
                    break;
                }

                dos.writeUTF(ep.getIp());
                dos.writeInt(ep.getPort());
            }

            dos.writeUTF("");
            dos.flush();

            return baos.toByteArray();

        } catch (IOException ioe) {
            throw new RumorsException("Failed converting known endpoints to buffer", ioe);
        }
    }

    private void bufferToEndPoints(InputStream is) throws RumorsException {
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

            String ip = dis.readUTF();
            while (ip.length() > 0) {
                int port = dis.readInt();

                Endpoint ep = new Endpoint(ip, port);
                if (!knownMessageSockets.containsKey(ep)) {
                    knownMessageSockets.put(ep, false);
                }
                ip = dis.readUTF();
            }

        } catch (IOException ioe) {
            throw new RumorsException("Failed converting incoming buffer to endpoints", ioe);
        }
    }

    private class DynamicBroadcastRunnable implements Runnable {
        @Override
        public void run() {
            int delayIndex = 0;
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(broadcastAnnounce[delayIndex++]);
                    if (delayIndex >= broadcastAnnounce.length) {
                        --delayIndex;
                    }

                    byte[] message = endPointsToBuffer();
                    DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(broadcastEndpoint.getIp()),
                            broadcastEndpoint.getPort());
                    LOGGER.info("Sending dynamic broadcast packet {}", knownMessageSockets.keySet());
                    broadcastSocket.send(packet);
                } catch (Exception e) {
                    LOGGER.error("Failed performing broadcast", e);
                }
            }
        }
    }

    private class DynamicReceiveRunnable implements Runnable {
        @Override
        public void run() {
            byte[] buffer = new byte[4096];

            while (!Thread.interrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    broadcastSocket.receive(packet);
                    LOGGER.info("Receiving dynamic broadcast packet");

                    bufferToEndPoints(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                } catch (Exception e) {
                    LOGGER.error("Failed receiving broadcast", e);
                }
            }
        }
    }

    private class StaticBroadcastRunnable implements Runnable {
        @Override
        public void run() {
            int delayIndex = 0;
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(broadcastAnnounce[delayIndex++] + 100);
                    if (delayIndex >= broadcastAnnounce.length) {
                        --delayIndex;
                    }

                    LOGGER.info("Sending static broadcast packets {}", knownMessageSockets.keySet());
                    for (Endpoint ep : staticEndpoints) {
                        try (Socket s = new Socket(ep.getIp(), ep.getPort()); OutputStream os = s.getOutputStream(); InputStream is = s.getInputStream()) {
                            byte[] buffer = endPointsToBuffer();
                            os.write(buffer);
                            os.flush();
                            bufferToEndPoints(is);
                        } catch (IOException ioe) {
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed performing broadcast", e);
                }
            }
        }
    }

    private class StaticReceiveRunnable implements Runnable {

        @Override
        public void run() {
            while (!Thread.interrupted()) {

                try (Socket s = staticDiscoverySocket.accept();
                        BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
                        OutputStream os = s.getOutputStream()) {

                    LOGGER.info("Receiving static broadcast packets");
                    bufferToEndPoints(bis);
                    byte[] buffer = endPointsToBuffer();
                    os.write(buffer);
                    os.flush();
                } catch (Exception e) {
                    LOGGER.error("Failed receiving static discovery request", e);
                }

            }
        }
    }
}

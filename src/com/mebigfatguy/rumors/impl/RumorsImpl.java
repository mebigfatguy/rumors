/*
 * rumors - a simple discovery/connection protocol
 * Copyright 2011 MeBigFatGuy.com
 * Copyright 2011 Dave Brosius
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.mebigfatguy.rumors.Endpoint;
import com.mebigfatguy.rumors.Rumors;
import com.mebigfatguy.rumors.RumorsException;

public class RumorsImpl implements Rumors {

	private static final String DEFAULT_BROADCAST_IP = "228.229.230.231";
	private static final int DEFAULT_DYNAMIC_PORT = 13531;
	private static final int[] DEFAULT_ANNOUNCE_DELAY = { 100,5000,5000,5000,60000 };
	private static final int MAX_BUFFERED_ENDPOINTS = 4000 / 40;
	
	private Endpoint broadcastEndpoint = new Endpoint(DEFAULT_BROADCAST_IP, DEFAULT_DYNAMIC_PORT);
	private List<Endpoint> staticEndpoints = new ArrayList<Endpoint>();
	private int[] broadcastAnnounce = DEFAULT_ANNOUNCE_DELAY;
	
	private final Object sync = new Object();
	private Thread broadcastThread;
	private Thread receiveThread;
	private boolean running = false;
	private ServerSocket messageSocket;
	private MulticastSocket broadcastSocket;
	
	private final List<Endpoint> knownMessageSockets = new ArrayList<Endpoint>();

	
	@Override
	public void begin() throws RumorsException {
		synchronized(sync) {
			if (!running) {
				initializeRumorPorts();
				knownMessageSockets.add(new Endpoint(messageSocket.getLocalSocketAddress().toString(), messageSocket.getLocalPort()));
				
				broadcastThread = new Thread(new BroadcastRunnable());
				broadcastThread.setName("Rumor Broadcast");
				broadcastThread.start();
				receiveThread = new Thread(new ReceiveRunnable());
				receiveThread.setName("Rumor Receive");
				receiveThread.start();
				running = true;
			}
		}
	}

	@Override
	public void end() {
		synchronized(sync) {
			if (running) {
				try {
					terminateRumorPorts();
					
					broadcastThread.interrupt();
					receiveThread.interrupt();
					broadcastThread.join();
					receiveThread.join();
				} catch (InterruptedException ie) {
				} finally {
					broadcastThread = null;
					receiveThread = null;
					running = false;
				}
			}
		}
	}

	public void setBroadcastEndpoint(Endpoint bcEndpoint) {
		broadcastEndpoint = bcEndpoint;
	}

	public void setPoint2PointEndpoints(List<Endpoint> p2pEndpoints) {
		staticEndpoints = new ArrayList<Endpoint>(p2pEndpoints);
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
		} catch (IOException ioe) {
			terminateRumorPorts();
			throw new RumorsException("Failed initializing rumor ports", ioe);
		}
		
	}
	
	private void terminateRumorPorts() {
		try {
			messageSocket.close();

			if (broadcastSocket != null) {
				broadcastSocket.leaveGroup(InetAddress.getByName(broadcastEndpoint.getIp()));
				broadcastSocket.close();
			}
		} catch (Exception e) {
		} finally {
			messageSocket = null;
			broadcastSocket = null;
		}
	}
	
	private byte[] endPointsToBuffer() throws RumorsException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			
			synchronized(knownMessageSockets) {
				for (int i = 0; (i < knownMessageSockets.size()) && (i < MAX_BUFFERED_ENDPOINTS); i++) {
					Endpoint ep = knownMessageSockets.get(i);
					dos.writeUTF(ep.getIp());
					dos.writeInt(ep.getPort());
				}
			}
			dos.writeUTF("");
			dos.flush();
			
			
			return baos.toByteArray();
			
		} catch (IOException ioe) {
			throw new RumorsException("Failed converting known endpoints to buffer", ioe);
		}
	}
	
	private class BroadcastRunnable implements Runnable {
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
					DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(broadcastEndpoint.getIp()), broadcastEndpoint.getPort());
					broadcastSocket.send(packet);	
				} catch (Exception e) {		
				}
			}
		}
	}
	
	private class ReceiveRunnable implements Runnable {
		@Override
		public void run() {
			byte[] buffer = new byte[4196];
			
			while (!Thread.interrupted()) {
				try {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					broadcastSocket.receive(packet);
					
					System.out.println("RECV: " + packet.getLength());
				} catch (IOException ioe) {
					
				}
			}
		}
	}
}

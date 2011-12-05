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

import java.util.ArrayList;
import java.util.List;

import com.mebigfatguy.rumors.Rumors;
import com.mebigfatguy.rumors.TcpEndpoint;

public class RumorsImpl implements Rumors {

	private static final int DEFAULT_DYNAMIC_PORT = 13531;
	
	private int dynamicPort = DEFAULT_DYNAMIC_PORT;
	private List<TcpEndpoint> endpoints = new ArrayList<TcpEndpoint>();
	private int[] broadcastDelays = new int[] { 100,5000,5000,5000,60000 };
	
	private final Object sync = new Object();
	private Thread broadcastThread;
	private Thread receiveThread;
	private boolean running = false;

	
	@Override
	public void begin() {
		synchronized(sync) {
			if (!running) {
				broadcastThread = new Thread(new BroadcastRunnable());
				broadcastThread.start();
				receiveThread = new Thread(new ReceiveRunnable());
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

	public void setDynamicPort(int port) {
		dynamicPort = port;
	}

	public void setStaticEndpoints(List<TcpEndpoint> tcpEndpoints) {
		endpoints = new ArrayList<TcpEndpoint>(tcpEndpoints);
	}
	

	public void setBroadcastDelay(String value) {
		String[] delays = value.split(",");
		broadcastDelays = new int[delays.length];
		
		int i = 0;
		for (String delay : delays) {
			broadcastDelays[i++] = Integer.parseInt(delay);
		}
		
	}
	
	private class BroadcastRunnable implements Runnable {
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				
			}
		}
	}
	
	private class ReceiveRunnable implements Runnable {
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				
			}
		}
	}


}

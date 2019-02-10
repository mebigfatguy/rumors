/*
 * rumors - a simple discovery/connection protocol
 * Copyright 2011-2019 MeBigFatGuy.com
 * Copyright 2011-2019 Dave Brosius
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
package com.mebigfatguy.rumors;

public class Endpoint {
	private final String ip;
	private final int port;
	
	public Endpoint(String serverIP, int serverPort) {
		ip = serverIP;
		port = serverPort;
	}
	
	public String getIp() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	@Override
	public int hashCode() {
		return ip.hashCode() ^ port;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Endpoint)) {
			return false;
		}
		
		Endpoint that = (Endpoint) o;
		return ((port == that.port) && ip.equals(that.ip));
	}
	
	@Override
	public String toString() {
		return ip + ":" + port;
	}
}

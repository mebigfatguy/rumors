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

	private int dynamicPort;
	private List<TcpEndpoint> endpoints;
	
	@Override
	public void begin() {
		// TODO Auto-generated method stub

	}

	@Override
	public void end() {
		// TODO Auto-generated method stub

	}

	public void setDynamicPort(int port) {
		dynamicPort = port;
	}

	public void setStaticEndpoints(List<TcpEndpoint> tcpEndpoints) {
		endpoints = new ArrayList<TcpEndpoint>(tcpEndpoints);
	}

}

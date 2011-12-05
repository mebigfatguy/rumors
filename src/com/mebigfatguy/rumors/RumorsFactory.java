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
package com.mebigfatguy.rumors;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.mebigfatguy.rumors.aux.Closer;
import com.mebigfatguy.rumors.impl.RumorsImpl;

public class RumorsFactory {

	public static final String RUMORS_FILE = "/rumors.xml";
	private static final Rumors rumors;
	
	static {
		rumors = new RumorsImpl();
		initializeFromRumorsFile();
	}
	
	private RumorsFactory() {
	}
	
	public static Rumors getRumors() {
		return rumors;
	}
	
	private static void initializeFromRumorsFile() {
		
		InputStream is = null;
		
		try {
			is = new BufferedInputStream(RumorsFactory.class.getResourceAsStream(RUMORS_FILE));
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document d = db.parse(is);
			
		} catch (Exception e) {
			
		} finally {
			Closer.close(is);
		}
	}
}

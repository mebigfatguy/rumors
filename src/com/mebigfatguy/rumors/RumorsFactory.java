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

	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 
	public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	public static final String RUMORS_SCHEMA_NAME = "http://rumors.mebigfatguy.com/1.0/rumors";
	public static final String RUMORS_FILE = "/rumors.xml";
	public static final String RUMORS_SCHEMA_FILE = "/rumors.xsd";
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
		
		InputStream xmlIs = null;
		InputStream xsdIs = null;
		
		try {
			xmlIs = new BufferedInputStream(RumorsFactory.class.getResourceAsStream(RUMORS_FILE));
			xsdIs = new BufferedInputStream(RumorsFactory.class.getResourceAsStream(RUMORS_SCHEMA_FILE));
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(true);
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA); 
			dbf.setAttribute(JAXP_SCHEMA_SOURCE, xsdIs); 
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document d = db.parse(xmlIs);
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Closer.close(xmlIs);
			Closer.close(xsdIs);
		}
	}
}

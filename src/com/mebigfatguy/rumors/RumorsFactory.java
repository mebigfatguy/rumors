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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mebigfatguy.rumors.aux.Closer;
import com.mebigfatguy.rumors.impl.RumorsImpl;

public class RumorsFactory {

	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; 
	public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	public static final String RUMORS_SCHEMA_NAME = "http://rumors.mebigfatguy.com/1.0/rumors";
	public static final String RUMORS_FILE = "/rumors.xml";
	public static final String RUMORS_SCHEMA_FILE = "/rumors.xsd";
	
	private static Logger LOGGER = LoggerFactory.getLogger(RumorsFactory.class);

	
	private static final RumorsImpl rumors;
	
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
			
			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();
			xp.setNamespaceContext(new RumorsNamespaceContext());
			XPathExpression xpe = xp.compile("/ru:rumors/broadcast");
			
			Element e = (Element)xpe.evaluate(d, XPathConstants.NODE);
			rumors.setBroadcastEndpoint(new Endpoint(e.getAttribute("ip"), Integer.parseInt(e.getAttribute("port"))));
			
			xpe = xp.compile("/ru:rumors/point2point/tcp");
			NodeList tcps = (NodeList)xpe.evaluate(d, XPathConstants.NODESET);
			List<Endpoint> endpoints = new ArrayList<Endpoint>();
			for (int i = 0; i < tcps.getLength(); ++i) {
				Element tcp = (Element) tcps.item(i);
				String ip = tcp.getAttribute("ip");
				int port = Integer.parseInt(tcp.getAttribute("port"));
				Endpoint endpoint = new Endpoint(ip, port);
				endpoints.add(endpoint);
			}
			rumors.setPoint2PointEndpoints(endpoints);
			
			xpe = xp.compile("/ru:rumors/announce/@delay");
			Attr attr = (Attr) xpe.evaluate(d, XPathConstants.NODE);
			
			rumors.setBroadcastAnnounceDelay(attr.getValue());				
		} catch (Exception e) {
			LOGGER.error("Failed initializing rumors from file: " + RUMORS_FILE, e);
		} finally {
			Closer.close(xmlIs);
			Closer.close(xsdIs);
		}
	}
	
	private static class RumorsNamespaceContext implements NamespaceContext {
		@Override
		public String getNamespaceURI(String prefix) {
			return RUMORS_SCHEMA_NAME;
		}

		@Override
		public String getPrefix(String namespaceURI) {
			return "";
		}

		@Override
		public Iterator<String> getPrefixes(String namespaceURI) {
			return null;
		}
	}
}

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
package com.mebigfatguy.rumors;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mebigfatguy.rumors.impl.RumorsImpl;

public final class RumorsFactory {

    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    public static final String RUMORS_SCHEMA_NAME = "http://rumors.mebigfatguy.com/1.0/rumors";
    public static final String RUMORS_FILE = "/rumors.xml";
    public static final String RUMORS_SCHEMA_FILE = "/com.mebigfatguy/rumors/rumors.xsd";

    private static Logger LOGGER = LoggerFactory.getLogger(RumorsFactory.class);

    private RumorsFactory() {
    }

    public static Rumors createRumors() {
        return new RumorsImpl();
    }

    public static Rumors createRumors(Path rumorsPath) throws IOException {

        try (InputStream is = Files.newInputStream(rumorsPath)) {
            return createRumors(is);
        }
    }

    public static Rumors createRumors(InputStream rumorsStream) throws IOException {
        try (InputStream xmlIs = new BufferedInputStream(rumorsStream);
                InputStream xsdIs = new BufferedInputStream(RumorsFactory.class.getResourceAsStream(RUMORS_SCHEMA_FILE))) {

            RumorsImpl rumors = new RumorsImpl();

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

            Element e = (Element) xpe.evaluate(d, XPathConstants.NODE);
            rumors.setBroadcastEndpoint(new Endpoint(e.getAttribute("ip"), Integer.parseInt(e.getAttribute("port"))));

            xpe = xp.compile("/ru:rumors/static/@port");
            Attr staticPort = (Attr) xpe.evaluate(d, XPathConstants.NODE);
            if (staticPort != null) {
                rumors.setStaticPort(Integer.parseInt(staticPort.getValue()));
            }

            xpe = xp.compile("/ru:rumors/point2point/tcp");
            NodeList tcps = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
            List<Endpoint> endpoints = new ArrayList<>();
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

            return rumors;
        } catch (SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new IOException("Failed to parse rumors configuration file", e);
        }
    }

    private static class RumorsNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            return RUMORS_SCHEMA_NAME;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return "ru";
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return new Iterator<String>() {
                boolean returnedRU = false;

                @Override
                public boolean hasNext() {
                    return !returnedRU;
                }

                @Override
                public String next() {
                    if (returnedRU) {
                        throw new IllegalStateException();
                    }

                    returnedRU = true;
                    return "ru";
                }

            };
        }
    }
}

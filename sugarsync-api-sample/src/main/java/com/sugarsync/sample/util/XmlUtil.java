package com.sugarsync.sample.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility class used for different XML operations (formatting,parsing,
 * retrieving node value)
 */
public class XmlUtil {
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static XPathFactory xPathFactory = XPathFactory.newInstance();
    static {
        transformerFactory.setAttribute("indent-number", new Integer(2));
    }

    /**
     * Formats an unformatted xml and returns a pretty format.
     * 
     * @param unformattedXml
     *            the unformatted xml string
     * @return the xml in a pretty format
     * @throws TransformerException
     */
    public static String formatXml(String unformattedXml) throws TransformerException {
        Document doc = parseXml(unformattedXml);

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        String xmlString = result.getWriter().toString();
        return xmlString;
    }

    /**
     * Transforms a xml String in a Document
     * 
     * @param xmlString
     *            the xml Stirng
     * @return The Document resulted from xmlString
     */
    private static Document parseXml(String xmlString) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the value of the nodes described by a xpath expression
     * 
     * @param xml
     *            the xml string
     * @param xpathExpression
     *            the xpath expression
     * @return the text value of the nodes described by the xpath expression
     * @throws XPathExpressionException
     */

    public static List<String> getNodeValues(String xml, String xpathExpression)
            throws XPathExpressionException {
        List<String> nodeValues = new ArrayList<String>();
        Document doc = parseXml(xml);
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile(xpathExpression);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            nodeValues.add(nodes.item(i).getNodeValue());
        }
        return nodeValues;
    }
}

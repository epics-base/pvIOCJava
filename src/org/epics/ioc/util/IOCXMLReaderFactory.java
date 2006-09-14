/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;

import org.xml.sax.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.*;

/**
 * @author mrk
 *
 */
public class IOCXMLReaderFactory {
    static private UserHandler userHandler = new UserHandler();
    static private IOCXMLListener listener = null;
    static private String rootElementName = null;
    private static Map<String,String> substituteMap = new TreeMap<String,String>();
    private static List<String> pathList = new ArrayList<String>();
    private static Pattern separatorPattern = Pattern.compile("[, ]");
    private static Pattern equalPattern = Pattern.compile("[=]");
    
    static public IOCXMLReader getReader() {
        return userHandler;
    }
    
    /**
     * Create an IOCXMLReader.
     * @param rootElementName The root element tag name.
     * The root file and any included files must have the same rootElementName.
     * @param fileName The file.
     * @param listener The callback listener.
     * @throws IllegalStateException
     */
    public static void create(String rootElementName,String fileName, IOCXMLListener listener) 
    throws IllegalStateException
    {
        IOCXMLReaderFactory.rootElementName = rootElementName;
        IOCXMLReaderFactory.listener = listener;
        create(null,fileName);
    }
    
    private static IOCXMLReader create(Handler parent,String fileName) 
    throws IllegalStateException
    {
        String uri = null;
        try {
            uri = new File(fileName).toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                    String.format("%n")
                    + "XMLToDBDFactory.convert terminating with MalformedURLException"
                    + String.format("%n")
                    + e.getMessage());
        }
        XMLReader reader;
        Handler handler = new Handler(parent);
        try {
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(uri);
        } catch (SAXException e) {
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with SAXException"
                + String.format("%n")
                + e.getMessage());
        } catch (IOException e) {
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IOException"
                + String.format("%n")
                + e.getMessage());
        } catch (IllegalStateException e) {
            handler.errorMessage("IllegalStateException " + e.getMessage());
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IllegalStateException"
                + String.format("%n")
                + e.getMessage());
        } catch (IllegalArgumentException e) {
            handler.errorMessage("IllegalArgumentException " + e.getMessage());
            throw new IllegalStateException(
                String.format("%n")
                + "XMLToDBDFactory.convert terminating with IllegalArgumentException"
                + String.format("%n")
                + e.getMessage());
        }
        return handler;
    }
    
    private static class UserHandler implements IOCXMLReader {
        private IOCXMLReader currentReader = null;
        
        private void setCurrentReader(IOCXMLReader reader) {
            currentReader = reader;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCXMLReader#errorMessage(java.lang.String)
         */
        public void errorMessage(String message) {
            currentReader.errorMessage(message);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCXMLReader#fatalMessage(java.lang.String)
         */
        public void fatalMessage(String message) {
            currentReader.fatalMessage(message);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCXMLReader#warningMessage(java.lang.String)
         */
        public void warningMessage(String message) {
            currentReader.warningMessage(message);
        }
        
    }
    private static class Handler implements IOCXMLReader, ContentHandler, ErrorHandler {
        private Handler parent = null;
        private Locator locator;
        private int nWarning = 0;
        private int nError = 0;
        private int nFatal = 0;
        private boolean gotFirstElement = false;
        private StringBuilder charBuilder = new StringBuilder();
        
        private Handler(Handler parent) {
            this.parent = parent;
            userHandler.setCurrentReader(this);
        }
        
        private void printLocation() {
            System.err.printf("line %d column %d in %s%n",
                locator.getLineNumber(),
                locator.getColumnNumber(),
                locator.getSystemId());
            if(parent!=null) parent.printLocation();
        }
        private void printMessage(String message)
        {
            System.out.printf("%n%s%n",message);
            printLocation();
        }
        public void errorMessage(String message) {
            printMessage("error " + message);
            nError++;
        }

        public void fatalMessage(String message) {
            printMessage("fatal error " + message);
            nFatal++;
        }

        public void warningMessage(String message) {
            printMessage("warning " + message);
            nWarning++;
        }

        public void error(SAXParseException e) throws SAXException {
            printMessage("error " + e.toString());
            nError++;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            printMessage("fatal " + e.toString());
            nFatal++;
        }

        public void warning(SAXParseException e) throws SAXException {
            printMessage("warning " + e.toString());
            nWarning++;
        }

        private enum CharState {
            idle,
            got$,
            gotPrefix
        }
        private CharState charState = CharState.idle;
        
        public void characters(char[] ch, int start, int length) throws SAXException {
            switch(charState) {
            case idle:
                for(int i=0; i< length; i++) {
                    if(ch[start+i]=='$') {
                        if(i+1<length) {
                            if(ch[start+i+1]=='{') {
                                if(i>0) listener.characters(ch,start,i-start);
                                charState = CharState.got$;
                                characters(ch,start+i+1,length-(i+1));
                                return;
                            } else {
                                continue;
                            }
                        } else {
                            if(i>0) listener.characters(ch,start,i - start);
                            charState = CharState.got$;
                            return;
                        }
                    }
                }
                listener.characters(ch,start,length);
                return;
            case got$:
                if(ch[start]=='{') {
                    charState = CharState.gotPrefix;
                    charBuilder.setLength(0);
                    start++;
                    if(length>1) characters(ch,start,length-1);
                    return;
                }
                char[] str$ = new char[] {'$'};
                listener.characters(str$,0,1);
                charState = CharState.idle;
                if(length>1) listener.characters(ch,start+1,length-1);
                return;
            case gotPrefix:
                for(int i=0; i<length; i++) {
                    if(ch[start+i]=='}') {
                        if(i>0) charBuilder.append(ch,start,i);
                        String from = charBuilder.toString();
                        String to = substituteMap.get(from);
                        if(to!=null) {
                            char[] charArray = to.toCharArray();
                            listener.characters(charArray,0,charArray.length);
                        }
                        charState = CharState.idle;
                        if(i+1<length) characters(ch,start+i+1,length-(i+1));
                        return;
                    }
                }
                charBuilder.append(ch,start,length);
                return;
            }
        }

        public void endDocument() throws SAXException {
            if(parent==null) listener.endDocument();
            if(nWarning>0 || nError>0 || nFatal>0) {
                System.err.printf("%s endDocument: warning %d severe %d fatal %d%n",
                    locator.getSystemId(),nWarning,nError,nFatal);
            }
            userHandler.setCurrentReader(parent);
            parent = null;
            locator = null;
        }
         
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if(!gotFirstElement) {
                gotFirstElement = true;
                if(!qName.equals(rootElementName)) {
                    errorMessage(
                        "rootElementName is " + qName +
                        " but expected " + rootElementName);
                }
                return;
            }
            if(qName.equals("include")) {
                includeElement(atts);
                return;
            }
            if(qName.equals("substitute")) {
                substituteElement(atts);
                return;
            }
            charBuilder.setLength(0);
            Map<String,String> attributes = new TreeMap<String,String>();
            for(int i=0; i<atts.getLength(); i++) {
                String name = atts.getQName(i);
                String value = atts.getValue(i);
                int prefix = value.indexOf("${");
                int end = 0;
                if(prefix>=0) {
                    end = value.indexOf("}",prefix);
                    if(end<0 || (end-prefix)<3) {
                        errorMessage("attribute " + name + " has bad value");
                    } else {
                        StringBuilder builder = new StringBuilder();
                        if(prefix>0) builder.append(value.substring(0,prefix));
                        String temp = value.substring(prefix+2,end);
                        temp = substituteMap.get(temp);
                        if(temp==null) {
                            errorMessage("attribute " + name + " no substitution found");
                        } else {
                            builder.append(temp);
                        }
                        if(end+1<value.length()) {
                            builder.append(value.substring(end+1));
                        }
                        value = builder.toString();
                     }
                }
                attributes.put(name,value);
            }
            listener.startElement(qName,attributes);
        }
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(qName.equals(rootElementName)) return;
            if(qName.equals("include")) return;
            if(qName.equals("substitute")) return;
            listener.endElement(qName);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void processingInstruction(String target, String data) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void skippedEntity(String name) throws SAXException {
            // TODO Auto-generated method stub
            
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // TODO Auto-generated method stub
            
        }
        
        private void includeElement(Attributes atts) {
            String removePath = atts.getValue("removePath");
            if(removePath!=null) {
                if(!pathList.remove(removePath)) {
                    warningMessage("path " + removePath + " not in pathList");
                }
            }
            String addPath = atts.getValue("addPath");
            if(addPath!=null) {
                pathList.add(0,addPath);
            }
            String href = atts.getValue("href");
            if(href==null) {
                if(removePath==null && addPath==null) {
                    warningMessage("no attribute was recognized");
                }
                return;
            }
            if(pathList.size()>0) {
                href = pathList.get(0) + File.separator + href; 
            }
            create(this,href);
            return;
        }
        
        private void substituteElement(Attributes atts) {
            
            String remove = atts.getValue("remove");
            if(remove!=null) {
                if(substituteMap.remove(remove)==null) {
                    warningMessage(remove + " not found");
                }
            }
            String from = atts.getValue("from");
            if(from!=null) {
                String to = atts.getValue("to");
                if(to==null) {
                    warningMessage("from without corresonding to");
                } else {
                    substituteMap.put(from,to);
                }
            }
            String fromTo = atts.getValue("fromTo");
            if(fromTo==null) {
                if(remove==null && from==null) {
                    warningMessage("no attribute was recognized");
                }
                return;
            }
            String[] items = separatorPattern.split(fromTo);
            for(String item : items) {
                String[] parts = equalPattern.split(item);
                if(parts.length!=2) {
                    errorMessage(item + " is not a valid substitution");
                } else {
                    substituteMap.put(parts[0],parts[1]);
                }
            }
        }
    }
}
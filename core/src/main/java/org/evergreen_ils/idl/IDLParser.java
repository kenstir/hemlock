package org.evergreen_ils.idl;

import org.evergreen_ils.xdata.XOSRFCoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.opensrf.util.OSRFRegistry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class IDLParser {

    public static final String OILS_NS_BASE="http://opensrf.org/spec/IDL/base/v1";
    public static final String OILS_NS_OBJ="http://open-ils.org/spec/opensrf/IDL/objects/v1";
    public static final String OILS_NS_OBJ_PREFIX="oils_obj";
    public static final String OILS_NS_PERSIST="http://open-ils.org/spec/opensrf/IDL/persistence/v1";
    public static final String OILS_NS_PERSIST_PREFIX="oils_persist";
    public static final String OILS_NS_REPORTER="http://open-ils.org/spec/opensrf/IDL/reporter/v1";
    public static final String OILS_NS_REPORTER_PREFIX="reporter";

    /** The source for the IDL XML */
    InputStream inStream;
    IDLObject current;
    private int fieldIndex;
    private int parsedObjectCount;

    public IDLParser() {
        parsedObjectCount = 0;
        fieldIndex = 0;
    }

    public IDLParser(String fileName) throws IOException {
        this(new FileInputStream(fileName));
    }

    public IDLParser(InputStream inStream) {
        this();
        this.inStream = inStream;
    }

    /**
    * Parses the IDL XML
    */
    public void parse() throws IOException, IDLException {
    
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(this.inStream, null);
            int eventType = xpp.getEventType();

            /** cycle through the XML events */
            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        handleStartElement(xpp);
                        break;
                    case XmlPullParser.END_TAG:
                        handleEndElement(xpp);
                        break;
                }
                eventType = xpp.next();
            }
        } catch(XmlPullParserException se) {
            throw new IDLException("Error parsing IDL XML", se);
        }
    }


    public void handleStartElement(XmlPullParser reader) {

        if(!OILS_NS_BASE.equals(reader.getNamespace())) return;
        String localpart = reader.getName();
    
        if( "class".equals(localpart) ) {
            fieldIndex = 0;
            current = new IDLObject();
            current.setIDLClass(reader.getAttributeValue(null, "id"));
            return;
        }
    
        if( "field".equals(localpart) ) {
            IDLField field = new IDLField();
            field.setName(reader.getAttributeValue(null, "name"));
            field.setArrayPos(fieldIndex++);
            field.setIsVirtual("true".equals(reader.getAttributeValue(OILS_NS_PERSIST, "virtual")));
            current.addField(field);
        }
    }

    public void handleEndElement(XmlPullParser reader) throws IDLException {

        if(!OILS_NS_BASE.equals(reader.getNamespace())) return;
        String localpart = reader.getName();

        if("class".equals(localpart)) {
            HashMap fields = current.getFields();
            String[] fieldNames = new String[fields.size()];

            for(Iterator itr = fields.keySet().iterator(); itr.hasNext(); ) {
                String key = (String) itr.next();
                IDLField field = (IDLField) fields.get(key);
                try {
                    fieldNames[ field.getArrayPos() ] = field.getName();
                } catch(ArrayIndexOutOfBoundsException E) {
                    String msg = "class="+current.getIDLClass()+";field="+key+
                        ";fieldcount="+fields.size()+";currentpos="+field.getArrayPos();
                    throw new IDLException(msg, E);
                }
            }

            XOSRFCoder.registerClass(
                current.getIDLClass(), List.of(fieldNames));
            OSRFRegistry.registerObject(
                current.getIDLClass(), OSRFRegistry.WireProtocol.ARRAY, fieldNames);

            parsedObjectCount++;

            current = null;
        }
    }
}

package org.open_ils.idl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.opensrf.util.OSRFRegistry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;


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
    HashMap<String, IDLObject> IDLObjects;
    IDLObject current;
    private int fieldIndex;

    /** If true, we retain the full set of IDL objects in memory.  This is true by default. */
    private boolean keepIDLObjects;

    private int parsedObjectCount;

    public IDLParser() {
        IDLObjects = new HashMap<String, IDLObject>();
        keepIDLObjects = true;
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
            //XMLInputFactory factory = XMLInputFactory.newInstance();
            
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(this.inStream,null );
            int eventType = xpp.getEventType();

            /** disable as many unused features as possible to speed up the parsing */
            /*
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
             */
    
        /** create the stream reader */
    
            while(eventType != XmlPullParser.END_DOCUMENT) {
                /** cycle through the XML events */
    
               // eventType = reader.next();
    
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

    /**
    * Returns the IDLObject with the given IDLClass 
    */
    public IDLObject getObject(String IDLClass) {
      return (IDLObject) IDLObjects.get(IDLClass);
    }

    /**
     * Returns the full set of IDL objects as a hash from classname to object.
     * If keepIDLObjects is false, the map will be empty.
     */
    public HashMap<String, IDLObject> getIDLObjects() {
        return IDLObjects;
    }

    /**
     * Returns the number of parsed objects, regardless of the keepIDLObjects setting.
     */
    public int getObjectCount() {
        return parsedObjectCount;
    }


    public void handleStartElement(XmlPullParser reader) {

        if(!OILS_NS_BASE.equals(reader.getNamespace())) return;
        String localpart = reader.getName();
    
        if( "class".equals(localpart) ) {
            fieldIndex = 0;
            current = new IDLObject();
            current.setIDLClass(reader.getAttributeValue(null, "id"));
            current.setController(reader.getAttributeValue(null, "controller"));
            String persist = reader.getAttributeValue(OILS_NS_PERSIST, "virtual");
            current.setIsVirtual("persist".equals(reader.getAttributeValue(OILS_NS_PERSIST, "virtual")));
            return;
        }
    
        if( "field".equals(localpart) ) {
            IDLField field = new IDLField();
            field.setName(reader.getAttributeValue(null, "name"));
            field.setArrayPos(fieldIndex++);
            field.setIsVirtual("true".equals(reader.getAttributeValue(OILS_NS_PERSIST, "virtual")));
            current.addField(field);
            //Log.d("parser","Field " + localpart + " " + field );
        }

        if( "link".equals(localpart) ) {
            IDLLink link = new IDLLink();
            link.setField(reader.getAttributeValue(null, "field"));
            link.setReltype(reader.getAttributeValue(null, "reltype"));
            link.setKey(reader.getAttributeValue(null, "key"));
            link.setMap(reader.getAttributeValue(null, "map"));
            link.setIDLClass(reader.getAttributeValue(null, "class"));
            current.addLink(link);
        }
    }

    public void handleEndElement(XmlPullParser reader) throws IDLException {

        if(!OILS_NS_BASE.equals(reader.getNamespace())) return;
        String localpart = reader.getName();

        if("class".equals(localpart)) {

            if(keepIDLObjects)
                IDLObjects.put(current.getIDLClass(), current);

            HashMap fields = current.getFields();
            String fieldNames[] = new String[fields.size()];

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

            OSRFRegistry.registerObject(
                current.getIDLClass(), OSRFRegistry.WireProtocol.ARRAY, fieldNames);

            parsedObjectCount++;

            current = null;
        }
    }


    public String toXML() {
        StringBuffer sb = new StringBuffer();
        Set keys = IDLObjects.keySet();
        Iterator itr = IDLObjects.keySet().iterator();
        String IDLClass;
        IDLObject obj;
        while(itr.hasNext()) {
            IDLClass = (String) itr.next();
            obj = IDLObjects.get(IDLClass);
            obj.toXML(sb);
        }
        return sb.toString();
    }
}







package org.evergreen_ils.gateway.idl;
import java.util.HashMap;
import java.util.Iterator;


public class IDLObject {

    private String IDLClass;
    private HashMap<String, IDLField> fields;

    public IDLObject() {
       fields = new HashMap<String, IDLField>();
    }

    public String getIDLClass() {
       return IDLClass;
    }

    public void addField(IDLField field) {
       fields.put(field.getName(), field);
    }

    public IDLField getField(String name) {
        return fields.get(name);
    }

    public HashMap getFields() {
        return fields;
    }

    public void setIDLClass(String IDLClass) {
       this.IDLClass = IDLClass;
    }

    public void toXML(StringBuffer sb) {

        sb.append("\t\t<fields>");
        Iterator itr = fields.keySet().iterator();
        IDLField field;
        while(itr.hasNext()) {
            field = fields.get(itr.next());
            field.toXML(sb);
        }
        sb.append("\t\t</fields>");
    }
}

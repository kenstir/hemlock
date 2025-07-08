/*
 * Copyright (c) 2025 Kenneth H. Cox
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

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

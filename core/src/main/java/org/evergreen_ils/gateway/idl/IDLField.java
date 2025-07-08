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

public class IDLField {

    /** Field name */
    private String name;

    /** Where this field resides in the array when serilized */
    private int arrayPos;

    /** True if this field does not belong in the database */
    private boolean isVirtual;

    public void setName(String name) {
      this.name = name;
    }
    public void setArrayPos(int arrayPos) {
      this.arrayPos = arrayPos;
    }
    public void setIsVirtual(boolean isVirtual) {
      this.isVirtual = isVirtual;
    }
    public String getName() {
      return this.name;
    }
    public int getArrayPos() {
      return this.arrayPos;
    }
    public boolean getIsVirtual() {
      return this.isVirtual;
    }

    public void toXML(StringBuffer sb) {
        sb.append("\t\t\t<field name='");
        sb.append(name);
        sb.append("' ");
        sb.append(IDLParser.OILS_NS_OBJ_PREFIX);
        sb.append(":array_position='");
        sb.append(arrayPos);
        sb.append("' ");
        sb.append(IDLParser.OILS_NS_PERSIST_PREFIX);
        sb.append(":virtual='");
        sb.append(isVirtual);
        sb.append("'/>\n");
    }
}

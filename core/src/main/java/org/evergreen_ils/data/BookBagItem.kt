/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen_ils.data

import org.evergreen_ils.searchCatalog.RecordInfo
import org.opensrf.util.OSRFObject
import java.io.Serializable
import java.util.*

class BookBagItem(cbrebiObj: OSRFObject) : Serializable {
    @JvmField
    var id: Int = cbrebiObj.getInt("id") ?: -1
    @JvmField
    var targetId: Int = cbrebiObj.getInt("target_biblio_record_entry") ?: -1
    var createTime: Date? = cbrebiObj.getDate("create_time")
    @JvmField
    var recordInfo: RecordInfo? = null
}

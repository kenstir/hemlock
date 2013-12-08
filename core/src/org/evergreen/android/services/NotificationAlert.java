/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * or the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be usefull,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 * 
 */
package org.evergreen.android.services;

import java.util.Date;

import org.androwrapee.db.DatabaseClass;
import org.androwrapee.db.DatabaseField;
import org.androwrapee.db.IdField;

@DatabaseClass
public class NotificationAlert {

    public static final String tableName = "notifications";

    public static final int NOTIFICATION_INTENT = 123456;

    @IdField
    public long id;

    @DatabaseField
    public int intent_val;

    @DatabaseField
    public Date triggerDate;

    @DatabaseField
    public String message;

    // required constructor for DAO
    public NotificationAlert() {

    }

    public NotificationAlert(int id, int intent_val, Date triggerDate,
            String message) {

        this.id = id;
        this.intent_val = intent_val;
        this.triggerDate = triggerDate;
        this.message = message;
    }

    @Override
    public String toString() {

        return " Notification:[ id: " + id + "; intent_val: " + intent_val
                + "; triggerDate : " + triggerDate + "; message: " + message
                + "]";
    }
}

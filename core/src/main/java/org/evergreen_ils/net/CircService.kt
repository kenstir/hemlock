/*
 * Copyright (c) 2020 Kenneth H. Cox
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.net

import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.Result
import org.opensrf.util.OSRFObject

interface CircService {
    suspend fun fetchCirc(account: Account, circId: Int): Result<OSRFObject>
    suspend fun fetchHolds(account: Account): Result<List<OSRFObject>>
    suspend fun fetchHoldQueueStats(account: Account, holdId: Int): Result<OSRFObject>
    suspend fun placeHoldAsync(account: Account, holdType: String, targetId: Int, pickupLib: Int, emailNotify: Boolean, phoneNotify: String?, smsNotify: String?, smsCarrierId: Int?, expireTime: String?, suspendHold: Boolean, thawDate: String?, useOverride: Boolean): Result<OSRFObject>
    suspend fun cancelHoldAsync(account: Account, holdId: Int): Result<String?>
    suspend fun fetchTitleHoldIsPossible(account: Account, targetId: Int, pickupLib: Int): Result<OSRFObject>
    suspend fun renewCircAsync(account: Account, targetCopy: Int): Result<OSRFObject>
    suspend fun updateHoldAsync(account: Account, holdId: Int, pickupLib: Int, expireTime: String?, suspendHold: Boolean, thawDate: String?): Result<String>
}

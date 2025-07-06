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

package net.kenstir.hemlock.net

import net.kenstir.hemlock.data.Result
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.model.CircRecord
import net.kenstir.hemlock.data.model.HoldPart
import net.kenstir.hemlock.data.model.HoldRecord

interface CircService {
    /**
     * Fetches the current checkouts.
     *
     * @return list of skeleton circ records.  These records must be fleshed out with details using [loadCheckoutDetails].
     */
    suspend fun fetchCheckouts(account: Account): Result<List<CircRecord>>

    /**
     * Fetches the details for a circ record.
     */
    suspend fun loadCheckoutDetails(account: Account, circRecord: CircRecord): Result<Unit>

    /**
     * Fetches the current holds.
     *
     * @return list of skeleton hold records.  These records must be fleshed out with details using [loadHoldDetails].
     */
    suspend fun fetchHolds(account: Account): Result<List<HoldRecord>>

    /**
    * Fetches the details for a hold record.
    */
    suspend fun loadHoldDetails(account: Account, holdRecord: HoldRecord): Result<Unit>

    /**
     * Fetches the parts available to place a hold.
     */
    suspend fun fetchHoldParts(targetId: Int): Result<List<HoldPart>>

    /**
     * Fetches whether a title hold is possible for the given item with parts for the specified pickup library.
     */
    suspend fun fetchTitleHoldIsPossible(account: Account, targetId: Int, pickupLib: Int): Result<Boolean>

    /**
     * Places a hold on the specified target.
     *
     * @param targetId titleId for Title hold, partId for Part hold
     */
    suspend fun placeHold(account: Account, targetId: Int, options: HoldOptions): Result<Boolean>

    /**
     * Updates an existing hold with new options.
     */
    suspend fun updateHold(account: Account, holdId: Int, options: HoldOptions): Result<Boolean>

    /**
     * Cancels a hold.
     */
    suspend fun cancelHold(account: Account, holdId: Int): Result<Boolean>
}

/**
 * Options for placing or updating a hold.  Properties marked as `var` can be modified for [CircService.updateHold].
 *
 * @property holdType The type of hold.
 * @property emailNotify Whether to notify by email.
 * @property phoneNotify Phone number to notify.
 * @property smsNotify Phone number to notify by SMS.
 * @property smsCarrierId Carrier ID for SMS notification.
 * @property useOverride Whether to use the .override option when placing a hold.
 * @property pickupLib Library ID for pickup location.
 * @property expireTime Expiration time for the hold (ISO 8601 format).
 * @property suspendHold Whether to suspend the hold.
 * @property thawDate Date to thaw the hold (ISO 8601 format).
 */
data class HoldOptions(
    val holdType: String,
    val emailNotify: Boolean,
    val phoneNotify: String? = null,
    val smsNotify: String? = null,
    val smsCarrierId: Int? = null,
    val useOverride: Boolean = false,
    var pickupLib: Int,
    var expireTime: String? = null,
    var suspendHold: Boolean = false,
    var thawDate: String? = null,
)

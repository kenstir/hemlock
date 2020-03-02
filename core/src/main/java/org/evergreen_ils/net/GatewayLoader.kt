/*
 * Copyright (c) 2019 Kenneth H. Cox
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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.data.Organization
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.Account
import org.evergreen_ils.data.BookBag
import org.evergreen_ils.data.Result
import org.opensrf.util.OSRFObject

object GatewayLoader {

    // usage: loadOrgSettingsAsync(...).await()
    suspend fun loadOrgSettingsAsync(org: Organization?) = GlobalScope.async {
        val orgs = if (org != null) listOf(org) else EgOrg.orgs
        for (org in orgs) {
            if (!org.settingsLoaded) {
                async {
                    val result = Gateway.actor.fetchOrgSettings(org.id)
                    if (result is Result.Success) {
                        org.loadSettings(result.data);
                        Log.d(TAG, "[kcxxx] org ${org.id} loaded")
                    }
                }
            }
        }
    }

    suspend fun loadBookBagsAsync(account: Account): Result<List<OSRFObject>?> {
        return if (account.bookBagsLoaded) {
            Log.d(TAG, "[kcxxx] loadBookBagsAsync...noop")
            Result.Success(null)
        } else {
            Log.d(TAG, "[kcxxx] loadBookBagsAsync...")
            Gateway.actor.fetchBookBags(account)
        }
    }
}

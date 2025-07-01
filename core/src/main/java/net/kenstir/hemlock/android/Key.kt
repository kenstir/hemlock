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

package net.kenstir.hemlock.android

/**
 * Keys for intent extras "must" use a package prefix, according to {@link android.content.Intent#putExtra()}.
 * But AccountManager.* keys don't have a prefix, and I haven't seen any problems with omitting prefixes.
 */
object Key {
    const val NUM_RESULTS = "numResults"
    const val ORG_ID = "orgID"
    const val PATRON_LIST = "patronList"
    const val POSITION = "position"
    const val RECORD_INFO = "recordInfo"
    const val RECORD_POSITION = "recordPosition"
    const val SEARCH_BY = "searchBy"
    const val SEARCH_TEXT = "searchText"
    const val TOTAL = "total"
}

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

package net.kenstir.apps.core

import org.evergreen_ils.Api
import org.evergreen_ils.android.App
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.system.Library
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.StdoutLogProvider
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import org.junit.Assert.assertTrue

class GatewayTest {

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Log.d("hey", "setup");
    }

    @Test
    fun test_basic() {
        assertTrue(true)
        val s = Gateway.baseUrl
        assertNotNull(s)
        val url = Gateway.buildUrl(Api.ACTOR, Api.ILS_VERSION, arrayOf())
        assertNotNull(url)
    }

    @Test
    @Throws(Exception::class)
    fun test_suspendfun_1() {

    }

    companion object {

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())
            Log.d("hey", "here");

            App.setLibrary(Library("https://kenstir.ddns.net", "test"))
        }
    }
}
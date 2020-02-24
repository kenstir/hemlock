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

package org.evergreen_ils.test

import org.evergreen_ils.data.FineRecord
import org.evergreen_ils.android.Log
import org.evergreen_ils.android.StdoutLogProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.opensrf.util.GatewayResult
import org.opensrf.util.OSRFRegistry

class FineRecordTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            Log.setProvider(StdoutLogProvider())

            val circFields = arrayOf("checkin_lib","checkin_staff","checkin_time","circ_lib","circ_staff","desk_renewal","due_date","duration","duration_rule","fine_interval","id","max_fine","max_fine_rule","opac_renewal","phone_renewal","recurring_fine","recurring_fine_rule","renewal_remaining","grace_period","stop_fines","stop_fines_time","target_copy","usr","xact_finish","xact_start","create_time","workstation","checkin_workstation","checkin_scan_time","parent_circ","billings","payments","billable_transaction","circ_type","billing_total","payment_total","unrecovered","copy_location","aaactsc_entries","aaasc_entries","auto_renewal","auto_renewal_remaining")
            val mbtsFields = arrayOf("balance_owed","id","last_billing_note","last_billing_ts","last_billing_type","last_payment_note","last_payment_ts","last_payment_type","total_owed","total_paid","usr","xact_finish","xact_start","xact_type")
            val mvrFields = arrayOf("title","author","doc_id","doc_type","pubdate","isbn","publisher","tcn","subject","types_of_resource","call_numbers","edition","online_loc","synopsis","physical_description","toc","copy_count","series","serials","foreign_copy_maps")
            OSRFRegistry.registerObject("circ", OSRFRegistry.WireProtocol.ARRAY, circFields)
            OSRFRegistry.registerObject("mbts", OSRFRegistry.WireProtocol.ARRAY, mbtsFields)
            OSRFRegistry.registerObject("mvr", OSRFRegistry.WireProtocol.ARRAY, mvrFields)
        }
    }

    @Test
    fun test_noTransactions() {
        // open-ils.actor.user.transactions.have_charge.fleshed response if no charges
        val json = """
            {"payload":[[]],"status":200}
            """
        val result = GatewayResult.create(json)

        val fines = FineRecord.makeArray(result.asArray())
        assertNotNull(fines)
        assertEquals(0, fines?.size)
    }

    @Test
    fun test_oneCharge() {
        // open-ils.actor.user.transactions.have_charge.fleshed with one charge
        val json = """
            {"payload":[[{"circ":{"__c":"circ","__p":[null,null,null,69,3788,"f","2019-11-21T23:59:59-0500","21 days","default","1 day",90763841,"1.00","overdue_1","f","f","0.02","02_cent_per_day",1,"00:00:00",null,null,19331811,409071,null,"2019-10-31T13:27:47-0400","2019-10-31T13:27:47-0400",6355,null,null,null,null,null,null,null,null,null,null,614,null,null,"f",1]},"copy":null,"transaction":{"__c":"mbts","__p":["0.42",90763841,"System Generated Overdue Fine","2019-12-14T23:59:59-0500","Overdue materials",null,null,null,"0.42","0.0",409071,null,"2019-10-31T13:27:47-0400","circulation"]},"record":{"__c":"mvr","__p":["The testaments","Atwood, Margaret",4286727,null,"2019","9780385543781",null,"4286727",{"Misogyny":1,"Surrogate mothers":1,"Women":1,"Man-woman relationships":1},["text"],[],"First edition.",[],"yadda yadda yadda.","print x, 419 pages ; 25 cm","Intro -- Finale -- Coda.",null,[]]}}]],"status":200}
            """
        val result = GatewayResult.create(json)

        val fines = FineRecord.makeArray(result.asArray())
        assertEquals(1, fines.size)
        val finesRecord = fines.first()
        assertEquals("The testaments", finesRecord.title)
        assertEquals("Atwood, Margaret", finesRecord.subtitle)
        assertEquals(0.42, finesRecord.balance_owed)
        assertEquals(1.0, finesRecord.max_fine)
        assertEquals("fines accruing", finesRecord.status)
    }

}

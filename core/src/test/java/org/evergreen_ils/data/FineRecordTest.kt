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

package org.evergreen_ils.data

import org.evergreen_ils.xdata.XGatewayResult
import org.evergreen_ils.xdata.XOSRFCoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

class FineRecordTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val circFields = listOf("checkin_lib","checkin_staff","checkin_time","circ_lib","circ_staff","desk_renewal","due_date","duration","duration_rule","fine_interval","id","max_fine","max_fine_rule","opac_renewal","phone_renewal","recurring_fine","recurring_fine_rule","renewal_remaining","grace_period","stop_fines","stop_fines_time","target_copy","usr","xact_finish","xact_start","create_time","workstation","checkin_workstation","checkin_scan_time","parent_circ","billings","payments","billable_transaction","circ_type","billing_total","payment_total","unrecovered","copy_location","aaactsc_entries","aaasc_entries","auto_renewal","auto_renewal_remaining")
            val mbtsFields = listOf("balance_owed","id","last_billing_note","last_billing_ts","last_billing_type","last_payment_note","last_payment_ts","last_payment_type","total_owed","total_paid","usr","xact_finish","xact_start","xact_type")
            val mvrFields = listOf("title","author","doc_id","doc_type","pubdate","isbn","publisher","tcn","subject","types_of_resource","call_numbers","edition","online_loc","synopsis","physical_description","toc","copy_count","series","serials","foreign_copy_maps")
            XOSRFCoder.registerClass("circ", circFields)
            XOSRFCoder.registerClass("mbts", mbtsFields)
            XOSRFCoder.registerClass("mvr", mvrFields)
        }
    }

    @Test
    fun test_noTransactions() {
        // open-ils.actor.user.transactions.have_charge.fleshed response if no charges
        val json = """
            {"payload":[[]],"status":200}
            """
        val result = XGatewayResult.create(json)

        val fines = FineRecord.makeArray(result.payloadFirstAsObjectList())
        assertNotNull(fines)
        assertEquals(0, fines.size)
    }

    @Test
    fun test_oneCircCharge() {
        // open-ils.actor.user.transactions.have_charge.fleshed with one charge
        val json = """
            {"payload":[[{"circ":{"__c":"circ","__p":[null,null,null,69,3788,"f","2019-11-21T23:59:59-0500","21 days","default","1 day",90763841,"1.00","overdue_1","f","f","0.02","02_cent_per_day",1,"00:00:00",null,null,19331811,409071,null,"2019-10-31T13:27:47-0400","2019-10-31T13:27:47-0400",6355,null,null,null,null,null,null,null,null,null,null,614,null,null,"f",1]},"copy":null,"transaction":{"__c":"mbts","__p":["0.42",90763841,"System Generated Overdue Fine","2019-12-14T23:59:59-0500","Overdue materials",null,null,null,"0.42","0.0",409071,null,"2019-10-31T13:27:47-0400","circulation"]},"record":{"__c":"mvr","__p":["The testaments","Atwood, Margaret",4286727,null,"2019","9780385543781",null,"4286727",{"Misogyny":1,"Surrogate mothers":1,"Women":1,"Man-woman relationships":1},["text"],[],"First edition.",[],"yadda yadda yadda.","print x, 419 pages ; 25 cm","Intro -- Finale -- Coda.",null,[]]}}]],"status":200}
            """
        val result = XGatewayResult.create(json)

        val fines = FineRecord.makeArray(result.payloadFirstAsObjectList())
        assertEquals(1, fines.size)
        val fine = fines.first()
        assertEquals("The testaments", fine.title)
        assertEquals("Atwood, Margaret", fine.subtitle)
        assertEquals(0.42, fine.balanceOwed)
        assertEquals(1.0, (fine as FineRecord).maxFine)
        assertEquals("fines accruing", fine.status)
    }

    @Test
    fun test_twoGroceryBills() {
        // two grocery bills, with neither circ nor record
        val json = """
            {"payload":[[
             {"transaction":{"__c":"mbts","__p":["2.00",221301311,null,"2021-03-15T09:49:53-0400","Card: Lost Fee",null,null,null,"2.00","0.0",4212142,null,"2021-03-15T09:49:52-0400","grocery"]}},
             {"transaction":{"__c":"mbts","__p":["3.75",221301316,"Photocopies","2021-03-15T09:50:15-0400","Miscellaneous",null,null,null,"3.75","0.0",4212142,null,"2021-03-15T09:50:15-0400","grocery"]}}
            ]],"status":200}
            """
        val result = XGatewayResult.create(json)

        val fines = FineRecord.makeArray(result.payloadFirstAsObjectList())
        assertEquals(2, fines.size)
        val fine = fines.first()
        assertEquals("Card: Lost Fee", fine.title)
        assertEquals(null, fine.subtitle)
        assertEquals(2.0, fine.balanceOwed)
        assertEquals(null, (fine as FineRecord).maxFine)
        assertEquals("", fine.status)
    }

    /* TODO: waiting to hear from Amy whether this logic is correct
    @Test
    fun test_oneChargeZeroBalance() {
        // open-ils.actor.user.transactions.have_charge.fleshed with one charge
        val json = """
            {"payload":[[{"circ":{"__c":"circ","__p":[null,null,null,69,3788,"f","2019-11-21T23:59:59-0500","21 days","default","1 day",90763841,"1.00","overdue_1","f","f","0.02","02_cent_per_day",1,"00:00:00",null,null,19331811,409071,null,"2019-10-31T13:27:47-0400","2019-10-31T13:27:47-0400",6355,null,null,null,null,null,null,null,null,null,null,614,null,null,"f",1]},"copy":null,"transaction":{"__c":"mbts","__p":["0.0",90763841,"System Generated Overdue Fine","2019-12-14T23:59:59-0500","Overdue materials",null,null,null,"0.0","0.0",409071,null,"2019-10-31T13:27:47-0400","circulation"]},"record":{"__c":"mvr","__p":["The testaments","Atwood, Margaret",4286727,null,"2019","9780385543781",null,"4286727",{"Misogyny":1,"Surrogate mothers":1,"Women":1,"Man-woman relationships":1},["text"],[],"First edition.",[],"yadda yadda yadda.","print x, 419 pages ; 25 cm","Intro -- Finale -- Coda.",null,[]]}}]],"status":200}
            """
        val result = XGatewayResult.create(json)

        val fines = FineRecord.makeArray(result.asArray())
        assertEquals(1, fines.size)
        val fine = fines.first()
        assertEquals(0.0, fine.balance_owed)
        assertEquals("", fine.status)
    }
     */
}

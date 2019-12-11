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
package org.evergreen_ils.accountAccess.fines

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.launch
import org.evergreen_ils.R
import org.evergreen_ils.accountAccess.AccountAccess
import org.evergreen_ils.accountAccess.SessionNotFoundException
import org.evergreen_ils.android.AccountUtils
import org.evergreen_ils.android.App
import org.evergreen_ils.data.EgOrg.findOrg
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayActor
import org.evergreen_ils.searchCatalog.RecordDetails
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.system.Analytics
import org.evergreen_ils.system.Log
import org.evergreen_ils.system.Utils
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.opensrf.util.OSRFObject
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.*

private const val TAG = "FinesActivity"

class FinesActivity : BaseActivity() {
    private var total_owed: TextView? = null
    private var total_paid: TextView? = null
    private var balance_owed: TextView? = null
    private var pay_fines_button: Button? = null
    private var lv: ListView? = null
    private var listAdapter: FinesArrayAdapter? = null
    private var finesRecords: ArrayList<FinesRecord>? = null
    private var haveAnyGroceryBills = false
    private var haveAnyFines = false
    private var getFinesInfo: Runnable? = null
    private var ac: AccountAccess? = null
    private var progress: ProgressDialogSupport? = null
    private var decimalFormater: DecimalFormat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_fines)

        decimalFormater = DecimalFormat("#0.00")
        lv = findViewById(R.id.fines_overdue_materials_list)
        total_owed = findViewById(R.id.fines_total_owed)
        total_paid = findViewById(R.id.fines_total_paid)
        balance_owed = findViewById(R.id.fines_balance_owed)
        pay_fines_button = findViewById(R.id.pay_fines)
        ac = AccountAccess.getInstance()
        progress = ProgressDialogSupport()
        finesRecords = ArrayList()
        listAdapter = FinesArrayAdapter(this,
                R.layout.fines_list_item, finesRecords!!)
        lv?.setAdapter(listAdapter)
        lv?.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
            Analytics.logEvent("Fines: Tap List Item", "have_grocery_bills", haveAnyGroceryBills)
            val records = ArrayList<RecordInfo>()
            if (haveAnyGroceryBills) {
                // If any of the fines are for non-circulation items ("grocery bills"), we
                // start the details flow with only the one record, if a record was selected.
                // The details flow can't handle nulls.
                val record = finesRecords!![position].recordInfo
                if (record != null) {
                    records.add(record)
                    RecordDetails.launchDetailsFlow(this@FinesActivity, records, 0)
                }
            } else {
                for (item in finesRecords!!) records.add(item.recordInfo)
                RecordDetails.launchDetailsFlow(this@FinesActivity, records, position)
            }
        })
        initPayFinesButton()
        initRunnable()
        progress!!.show(this, getString(R.string.msg_retrieving_fines))
        Thread(getFinesInfo).start()
    }

    override fun onDestroy() {
        if (progress != null) progress!!.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        loadData()
    }

    private fun loadData() {
        launch {
            try {
                val start = System.currentTimeMillis()
                Log.d(TAG, "[kcxxx] loadData ...")
                val job = GatewayActor.fetchAllOrgSettings()
                Log.d(TAG, "[kcxxx] loadData ... join")
                job.join()
                Log.d(TAG, "[kcxxx] loadData ... done")
                Log.logElapsedTime(TAG, start, "[kcxxx] loadData")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] loadData ... caught", ex)
            }
        }

    }

    private fun initPayFinesButton() {
        val homeOrgId = App.getAccount().homeOrg
        val homeOrg = findOrg(homeOrgId!!) //TODO: handle null
        if (resources.getBoolean(R.bool.ou_enable_pay_fines)
                && homeOrg != null && Utils.safeBool(homeOrg.isPaymentAllowedSetting)) {
            pay_fines_button!!.isEnabled = false
            pay_fines_button!!.setOnClickListener {
                Analytics.logEvent("Fines: Pay Fines", "num_fines", finesRecords!!.size)
                val username = App.getAccount().username
                val password = AccountUtils.getPassword(this@FinesActivity, username)
                var url = (Gateway.baseUrl
                        + "/eg/opac/login"
                        + "?redirect_to=" + URLEncoder.encode("/eg/opac/myopac/main_payment_form#pay_fines_now")
                        + "?username=" + URLEncoder.encode(username))
                if (!TextUtils.isEmpty(password)) url += "&password=" + URLEncoder.encode(password)
                startActivityForResult(Intent(Intent.ACTION_VIEW, Uri.parse(url)), App.REQUEST_LAUNCH_OPAC_LOGIN_REDIRECT)
            }
        } else {
            pay_fines_button!!.visibility = View.GONE
        }
    }

    private fun getFloat(o: OSRFObject?, field: String): Float {
        var ret = 0.0f
        try {
            if (o != null) ret = o.getString(field).toFloat()
        } catch (e: Exception) {
            Analytics.logException(e)
        }
        return ret
    }

    private fun initRunnable() {
        getFinesInfo = Runnable {
            var summary: OSRFObject? = null
            try {
                summary = ac!!.finesSummary
            } catch (e: SessionNotFoundException) {
                try {
                    if (ac!!.reauthenticate(this@FinesActivity)) summary = ac!!.finesSummary
                } catch (e1: Exception) {
                }
            }
            val finesSummary = summary
            var frecords: ArrayList<FinesRecord>? = null
            try {
                frecords = ac!!.transactions
            } catch (e: SessionNotFoundException) {
                try {
                    if (ac!!.reauthenticate(this@FinesActivity)) frecords = ac!!.transactions
                } catch (e1: Exception) {
                }
            }
            finesRecords = frecords
            runOnUiThread {
                listAdapter!!.clear()
                haveAnyFines = false
                haveAnyGroceryBills = false
                if (finesRecords != null) {
                    for (finesRecord in finesRecords!!) {
                        listAdapter!!.add(finesRecord)
                        haveAnyFines = true
                        if (finesRecord.recordInfo == null) {
                            haveAnyGroceryBills = true
                        }
                    }
                }
                listAdapter!!.notifyDataSetChanged()
                total_owed!!.text = decimalFormater!!.format(getFloat(finesSummary, "total_owed").toDouble())
                total_paid!!.text = decimalFormater!!.format(getFloat(finesSummary, "total_paid").toDouble())
                val balance = getFloat(finesSummary, "balance_owed").toDouble()
                balance_owed!!.text = decimalFormater!!.format(balance)
                pay_fines_button!!.isEnabled = haveAnyFines && balance > 0
                progress!!.dismiss()
            }
        }
    }

    internal inner class FinesArrayAdapter(context: Context, private val resourceId: Int, private val items: List<FinesRecord>) : ArrayAdapter<FinesRecord>(context, resourceId, items) {
        private var fineTitle: TextView? = null
        private var fineAuthor: TextView? = null
        private var fineBalanceOwed: TextView? = null
        private var fineStatus: TextView? = null

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(index: Int): FinesRecord {
            return items[index]
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val row = when(convertView) {
                null -> {
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    inflater.inflate(resourceId, parent, false)
                }
                else -> {
                    convertView
                }
            }

            fineTitle = row.findViewById(R.id.fines_title)
            fineAuthor = row.findViewById(R.id.fines_author)
            fineBalanceOwed = row.findViewById(R.id.fines_balance_owed)
            fineStatus = row.findViewById(R.id.fines_status)

            val record = getItem(position)
            fineTitle?.setText(record.title)
            fineAuthor?.setText(record.author)
            fineBalanceOwed?.setText(decimalFormater!!.format(record.balance_owed))
            fineStatus?.setText(record.status)

            return row
        }
    }

    fun onButtonClick(v: View) {
        val id = v.id
        if (id == R.id.pay_fines) {
            Toast.makeText(this, "payFines", Toast.LENGTH_LONG).show()
        }
    }
}

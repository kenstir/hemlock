/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 * Kotlin conversion by Kenneth H. Cox
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
package org.evergreen_ils.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.R
import org.evergreen_ils.android.AccountUtils
import org.evergreen_ils.android.App
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.data.FineRecord
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.searchCatalog.RecordDetails
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.Log
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import org.opensrf.util.OSRFObject
import java.net.URLEncoder
import java.text.DecimalFormat

private const val TAG = "FinesActivity"

class FinesActivity : BaseActivity() {
    private var total_owed: TextView? = null
    private var total_paid: TextView? = null
    private var balance_owed: TextView? = null
    private var pay_fines_button: Button? = null
    private var lv: ListView? = null
    private var listAdapter: FinesArrayAdapter? = null
    private var fineRecords: ArrayList<FineRecord> = ArrayList()
    private var haveAnyGroceryBills = false
    private var haveAnyFines = false
    private var progress: ProgressDialogSupport? = null
    private var decimalFormatter: DecimalFormat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.activity_fines)

        decimalFormatter = DecimalFormat("#0.00")
        lv = findViewById(R.id.fines_overdue_materials_list)
        total_owed = findViewById(R.id.fines_total_owed)
        total_paid = findViewById(R.id.fines_total_paid)
        balance_owed = findViewById(R.id.fines_balance_owed)
        pay_fines_button = findViewById(R.id.pay_fines)
        progress = ProgressDialogSupport()
        fineRecords = ArrayList()
        listAdapter = FinesArrayAdapter(this, R.layout.fines_list_item, fineRecords)
        lv?.adapter = listAdapter
        lv?.setOnItemClickListener { parent, view, position, id -> onItemClick(position) }
        updatePayFinesButtonState(false)
    }

    override fun onDestroy() {
        progress?.dismiss()
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name)

        fetchData()
    }

    private fun fetchData() {
        async {
            try {
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@FinesActivity, getString(R.string.msg_retrieving_fines))
                Log.d(TAG, "[kcxxx] fetchData ...")

                jobs.add(async {
                    // Need homeOrg's settings to enable/disable fines
                    val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
                    GatewayLoader.loadOrgSettingsAsync(homeOrg).await()
                    updatePayFinesButtonVisibility()
                })

                jobs.add(async {
                    onSummaryResult(Gateway.actor.fetchUserFinesSummary(App.getAccount()))
                })
                jobs.add(async {
                    onTransactionsResult(Gateway.actor.fetchUserTransactionsWithCharges(App.getAccount()))
                })

                jobs.joinAll()
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun updatePayFinesButtonVisibility() {
        val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
        if (resources.getBoolean(R.bool.ou_enable_pay_fines)
                && homeOrg?.isPaymentAllowedSetting ?: false) {
            pay_fines_button?.visibility = View.VISIBLE
            pay_fines_button?.setOnClickListener {
                Analytics.logEvent("Fines: Pay Fines", "num_fines", fineRecords.size)
                val username = App.getAccount().username
                val password = AccountUtils.getPassword(this@FinesActivity, username)
                var url = (Gateway.baseUrl
                        + "/eg/opac/login"
                        + "?redirect_to=" + URLEncoder.encode("/eg/opac/myopac/main_payment_form#pay_fines_now"))
                launchURL(url)
            }
        } else {
            pay_fines_button?.visibility = View.GONE
        }
//        Log.d(TAG, "[kcxxx] updatePayFinesButtonVisibility v:${pay_fines_button?.visibility}")
    }

    private fun updatePayFinesButtonState(enabled: Boolean) {
        pay_fines_button?.isEnabled = enabled
    }

    private fun onSummaryResult(result: Result<OSRFObject?>) {
        when (result) {
            is Result.Success -> loadSummary(result.data)
            is Result.Error -> showAlert(result.exception)
        }
    }

    private fun loadSummary(obj: OSRFObject?) {
//        Log.d(TAG, "[kcxxx] updateSummary: o:$obj")
        total_owed?.text = decimalFormatter?.format(getFloat(obj, "total_owed"))
        total_paid?.text = decimalFormatter?.format(getFloat(obj, "total_paid"))
        val balance = getFloat(obj, "balance_owed")
        balance_owed?.text = decimalFormatter?.format(balance)
        updatePayFinesButtonState(balance > 0)
    }

    private fun onTransactionsResult(result: Result<List<OSRFObject>>) {
        when (result) {
            is Result.Success -> loadTransactions(result.data)
            is Result.Error -> showAlert(result.exception)
        }
    }

    private fun loadTransactions(objects: List<OSRFObject>) {
//        Log.d(TAG, "[kcxxx] loadTransactions o:$objects")

        listAdapter?.clear()
        val fines = FineRecord.makeArray(objects)
        haveAnyFines = fines.isNotEmpty()
        haveAnyGroceryBills = false

        for (fine in fines) {
            listAdapter?.add(fine)
            if (fine.recordInfo == null) haveAnyGroceryBills = true
        }

        listAdapter?.notifyDataSetChanged()
    }

    private fun getFloat(o: OSRFObject?, field: String): Float {
        var ret = 0.0f
        try {
            ret = o?.getString(field)?.toFloat() ?: 0.0f
        } catch (e: Exception) {
            Analytics.logException(e)
        }
        return ret
    }

    private fun onItemClick(position: Int) {
        Analytics.logEvent("Fines: Tap List Item", "have_grocery_bills", haveAnyGroceryBills)
        val records = ArrayList<RecordInfo>()
        if (haveAnyGroceryBills) {
            // If any of the fines are for non-circulation items ("grocery bills"), we
            // start the details flow with only the one record, if a record was selected.
            // The details flow can't handle nulls.
            fineRecords[position].recordInfo?.let {
                records.add(it)
            }
        } else {
            for (item in fineRecords) {
                item.recordInfo?.let {
                    records.add(it)
                }
            }
        }
        if (records.size > 0) {
            val targetPosition = if (position > records.size - 1) records.size - 1 else position
            RecordDetails.launchDetailsFlow(this@FinesActivity, records, targetPosition)
        }
    }

    internal inner class FinesArrayAdapter(context: Context, private val resourceId: Int, private val items: List<FineRecord>) : ArrayAdapter<FineRecord>(context, resourceId, items) {
        private var fineTitle: TextView? = null
        private var fineAuthor: TextView? = null
        private var fineBalanceOwed: TextView? = null
        private var fineStatus: TextView? = null

        override fun getCount(): Int {
            return items.size
        }

        override fun getItem(index: Int): FineRecord {
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
            fineAuthor?.setText(record.subtitle)
            fineBalanceOwed?.setText(decimalFormatter!!.format(record.balance_owed))
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

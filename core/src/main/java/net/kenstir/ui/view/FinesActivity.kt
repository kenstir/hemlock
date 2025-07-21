/*
 * Copyright (C) 2012 Evergreen Open-ILS
 * @author Daniel-Octavian Rizea
 *
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
package net.kenstir.ui.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.kenstir.data.Result
import net.kenstir.data.model.BibRecord
import net.kenstir.data.model.ChargeRecord
import net.kenstir.data.model.PatronCharges
import net.kenstir.hemlock.R
import net.kenstir.logging.Log
import net.kenstir.ui.App
import net.kenstir.ui.BaseActivity
import net.kenstir.ui.account.AccountUtils
import net.kenstir.ui.util.ProgressDialogSupport
import net.kenstir.ui.util.compatEnableEdgeToEdge
import net.kenstir.ui.util.showAlert
import net.kenstir.ui.view.search.RecordDetails
import org.evergreen_ils.gateway.GatewayClient
import org.evergreen_ils.system.EgOrg
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
    private var charges: PatronCharges? = null
    private var haveAnyGroceryBills = false
    private var haveAnyFines = false
    private var progress: ProgressDialogSupport? = null
    private var decimalFormatter: DecimalFormat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        compatEnableEdgeToEdge()
        setContentView(R.layout.activity_fines)
        setupActionBar()
        adjustPaddingForEdgeToEdge()
        setupNavigationDrawer()

        decimalFormatter = DecimalFormat("#0.00")
        lv = findViewById(R.id.list_view)
        total_owed = findViewById(R.id.fines_total_owed)
        total_paid = findViewById(R.id.fines_total_paid)
        balance_owed = findViewById(R.id.fines_balance_owed)
        pay_fines_button = findViewById(R.id.pay_fines)
        progress = ProgressDialogSupport()
        listAdapter = FinesArrayAdapter(this, R.layout.fines_list_item)
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
        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        fetchData()
    }

    private fun fetchData() {
        scope.async {
            try {
                val start = System.currentTimeMillis()
                progress?.show(this@FinesActivity, getString(R.string.msg_retrieving_fines))
                Log.d(TAG, "[kcxxx] fetchData ...")

                val jobs = mutableListOf<Deferred<Any>>()
                val homeOrg = EgOrg.findOrg(App.getAccount().homeOrg)
                homeOrg?.let {
                    jobs.add(scope.async {
                        val result = App.getServiceConfig().orgService.loadOrgSettings(homeOrg.id)
                        if (result is Result.Error) {
                            throw result.exception
                        }
                        updatePayFinesButtonVisibility()
                    })
                }
                jobs.add(scope.async {
                    val result = App.getServiceConfig().userService.fetchPatronCharges(
                        App.getAccount())
                    onChargesResult(result)
                })

                jobs.map { it.await() }
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
                && homeOrg?.isPaymentAllowed == true) {
            pay_fines_button?.visibility = View.VISIBLE
            pay_fines_button?.setOnClickListener {
                //Analytics.logEvent("fines_payfines", "num_fines", fineRecords.size)
                val username = App.getAccount().username
                val password = AccountUtils.getPassword(this@FinesActivity, username)
                var url = (GatewayClient.baseUrl
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

    private fun onChargesResult(result: Result<PatronCharges>) {
        when (result) {
            is Result.Success -> {
                charges = result.data
                loadSummary()
                loadTransactions()
            }
            is Result.Error ->
                showAlert(result.exception)
        }
    }

    private fun loadSummary() {
        if (charges == null) {
            return
        }
        total_owed?.text = decimalFormatter?.format(charges!!.totalCharges)
        total_paid?.text = decimalFormatter?.format(charges!!.totalPaid)
        balance_owed?.text = decimalFormatter?.format(charges!!.balanceOwed)
        updatePayFinesButtonState(charges!!.balanceOwed > 0)
    }

    private fun loadTransactions() {
        listAdapter?.clear()
        haveAnyFines = charges?.transactions?.isNotEmpty() ?: false
        haveAnyGroceryBills = false
        charges?.transactions?.let {
            for (fine in it) {
                listAdapter?.add(fine)
                if (fine.record == null) haveAnyGroceryBills = true
            }
        }
        listAdapter?.notifyDataSetChanged()
    }

    private fun onItemClick(position: Int) {
        //Analytics.logEvent("fines_itemclick", "have_grocery_bills", haveAnyGroceryBills)
        val records = ArrayList<BibRecord>()
        val transactions = charges?.transactions ?: return
        if (haveAnyGroceryBills) {
            // If any of the fines are for non-circulation items ("grocery bills"), we
            // start the details flow with only the one record, if a record was selected.
            // The details flow can't handle nulls.
            val fineRecord = listAdapter?.getItem(position) ?: return
            fineRecord.record?.let {
                records.add(it)
            }
        } else {
            for (item in transactions) {
                item.record?.let {
                    records.add(it)
                }
            }
        }
        if (records.size > 0) {
            val targetPosition = if (position > records.size - 1) records.size - 1 else position
            RecordDetails.launchDetailsFlow(this@FinesActivity, records, targetPosition)
        }
    }

    internal inner class FinesArrayAdapter(context: Context, private val resourceId: Int) : ArrayAdapter<ChargeRecord>(context, resourceId) {
        private var fineTitle: TextView? = null
        private var fineAuthor: TextView? = null
        private var fineBalanceOwed: TextView? = null
        private var fineStatus: TextView? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
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
            fineTitle?.text = record?.title
            fineAuthor?.text = record?.subtitle
            fineBalanceOwed?.text = decimalFormatter!!.format(record?.balanceOwed)
            fineStatus?.text = record?.status

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

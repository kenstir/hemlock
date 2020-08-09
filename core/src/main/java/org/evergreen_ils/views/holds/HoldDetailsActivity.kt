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
package org.evergreen_ils.views.holds

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import kotlinx.coroutines.async
import org.evergreen_ils.Api
import org.evergreen_ils.R
import org.evergreen_ils.android.App
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.data.HoldRecord
import org.evergreen_ils.data.Result
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.Log
import org.evergreen_ils.utils.ui.*
import java.util.*

class HoldDetailsActivity : BaseActivity() {
    private var expirationDate: EditText? = null
    private var datePicker: DatePickerDialog? = null
    private var suspendHold: CheckBox? = null
    private var thawDatePicker: DatePickerDialog? = null
    private var thawDateEdittext: EditText? = null
    private var expireDate: Date? = null
    private var thawDate: Date? = null
    private var selectedOrgPos = 0
    private var progress: ProgressDialogSupport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!App.isStarted()) {
            App.restartApp(this)
            return
        }
        setContentView(R.layout.hold_details)
        ActionBarUtils.initActionBarForActivity(this)
        progress = ProgressDialogSupport()

        val record = intent.getSerializableExtra("holdRecord") as HoldRecord

        val title = findViewById<TextView>(R.id.hold_title)
        val author = findViewById<TextView>(R.id.hold_author)
        val format = findViewById<TextView>(R.id.hold_format)
        val physical_description = findViewById<TextView>(R.id.hold_physical_description)
        val cancelHold = findViewById<Button>(R.id.cancel_hold_button)
        val updateHold = findViewById<Button>(R.id.update_hold_button)
        suspendHold = findViewById(R.id.hold_suspend_hold)
        val orgSelector = findViewById<Spinner>(R.id.hold_pickup_location)
        expirationDate = findViewById(R.id.hold_expiration_date)
        thawDateEdittext = findViewById(R.id.hold_thaw_date)

        title.text = record.title
        author.text = record.author
        if (record.recordInfo != null) {
            format.text = record.recordInfo.iconFormatLabel
            physical_description.text = record.recordInfo.physical_description
        }
        suspendHold?.setChecked(record.isSuspended)
        if (record.isSuspended && record.thawDate != null) {
            thawDate = record.thawDate
            thawDateEdittext?.setText(DateFormat.format("MMMM dd, yyyy", thawDate))
        }
        if (record.expireTime != null) {
            expireDate = record.expireTime
            expirationDate?.setText(DateFormat.format("MMMM dd, yyyy", expireDate))
        }
        thawDateEdittext?.setEnabled(suspendHold?.isChecked() ?: false)
        cancelHold.setOnClickListener {
            val builder = AlertDialog.Builder(this@HoldDetailsActivity)
            builder.setMessage(R.string.cancel_hold_dialog_message)
            builder.setNegativeButton(R.string.cancel_hold_negative_button, null)
            builder.setPositiveButton(R.string.cancel_hold_positive_button) { dialog, which ->
                Analytics.logEvent("Holds: Cancel Hold")
                cancelHold(record)
            }
            builder.create().show()
        }
        updateHold.setOnClickListener {
            Analytics.logEvent("Holds: Update Hold")
            updateHold(record)
        }
        suspendHold?.setOnCheckedChangeListener { buttonView, isChecked ->
            thawDateEdittext?.setEnabled(isChecked)
        }
        val cal = Calendar.getInstance()
        datePicker = DatePickerDialog(this,
                OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val chosenDate = Date(year - 1900, monthOfYear, dayOfMonth)
                    expireDate = chosenDate
                    val strDate = DateFormat.format("MMMM dd, yyyy", chosenDate)
                    expirationDate?.setText(strDate)
                }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH])
        expirationDate?.setOnClickListener {
            datePicker?.show()
        }
        thawDatePicker = DatePickerDialog(this,
                OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val chosenDate = Date(year - 1900, monthOfYear, dayOfMonth)
                    thawDate = chosenDate
                    val strDate = DateFormat.format("MMMM dd, yyyy", chosenDate)
                    thawDateEdittext?.setText(strDate)
                }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH])
        thawDateEdittext?.setOnClickListener {
            thawDatePicker?.show()
        }
        var l = mutableListOf<String>()
        for (i in EgOrg.visibleOrgs.indices) {
            val org = EgOrg.visibleOrgs[i]
            l.add(org.spinnerLabel)
            if (org.id == record.pickupLib) {
                selectedOrgPos = i
            }
        }
        val adapter: ArrayAdapter<String> = OrgArrayAdapter(this, R.layout.org_item_layout, l, true)
        orgSelector.adapter = adapter
        orgSelector.setSelection(selectedOrgPos)
        orgSelector.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View, ID: Int, arg3: Long) {
                selectedOrgPos = ID
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    override fun onDestroy() {
        if (progress != null) progress!!.dismiss()
        super.onDestroy()
    }

    private fun cancelHold(record: HoldRecord) {
        async {
//            Log.d(TAG, "[kcxxx] cancelHold " + record.ahr.getInt("id"))
            progress?.show(this@HoldDetailsActivity, getString(R.string.msg_canceling_hold))

            val holdId = record.ahr.getInt("id") ?: 0
            val result = Gateway.circ.cancelHoldAsync(App.getAccount(), holdId)
            progress?.dismiss()
            when (result) {
                is Result.Success -> {
                    setResult(RESULT_CODE_DELETE_HOLD)
                    finish()
                }
                is Result.Error -> {
                    showAlert(result.exception)
                }
            }
        }
    }

    private fun updateHold(record: HoldRecord) {
        async {
            progress?.show(this@HoldDetailsActivity, getString(R.string.msg_updating_hold))
            var expireDateApi: String? = null
            var thawDateApi: String? = null
            if (expireDate != null) expireDateApi = Api.formatDate(expireDate)
            if (thawDate != null) thawDateApi = Api.formatDate(thawDate)

            val holdId = record.ahr.getInt("id") ?: 0
            val orgId = EgOrg.visibleOrgs[selectedOrgPos].id
            val result = Gateway.circ.updateHoldAsync(App.getAccount(), holdId,
                    orgId, expireDateApi, suspendHold!!.isChecked, thawDateApi)
            progress?.dismiss()
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this@HoldDetailsActivity,
                            getString(R.string.msg_updated_hold), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CODE_UPDATE_HOLD)
                    finish()
                }
                is Result.Error -> {
                    showAlert(result.exception)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = HoldDetailsActivity::class.java.simpleName
        const val RESULT_CODE_DELETE_HOLD = 5
        const val RESULT_CODE_UPDATE_HOLD = 6
        const val RESULT_CODE_CANCEL = 7
    }
}

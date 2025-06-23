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
 * This program is distributed in the hope that it will be useful,
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

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.Analytics
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.android.Log
import net.kenstir.hemlock.data.model.Account
import net.kenstir.hemlock.data.Result
import org.evergreen_ils.HOLD_TYPE_PART
import org.evergreen_ils.HOLD_TYPE_TITLE
import org.evergreen_ils.data.OSRFUtils
import org.evergreen_ils.data.SMSCarrier
import org.evergreen_ils.net.Gateway
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.system.EgSms
import org.evergreen_ils.utils.getCustomMessage
import org.evergreen_ils.utils.ui.BaseActivity
import org.evergreen_ils.utils.ui.OrgArrayAdapter
import org.evergreen_ils.utils.ui.ProgressDialogSupport
import org.evergreen_ils.utils.ui.showAlert
import org.opensrf.util.OSRFObject
import java.util.*

class PlaceHoldActivity : BaseActivity() {
    private var title: TextView? = null
    private var author: TextView? = null
    private var format: TextView? = null
    private var account: Account? = null
    private var expirationDate: EditText? = null
    private var smsNotify: EditText? = null
    private var phoneNotify: EditText? = null
    private var phoneNotification: CheckBox? = null
    private var emailNotification: CheckBox? = null
    private var smsNotification: CheckBox? = null
    private var smsSpinner: Spinner? = null
    private var placeHold: Button? = null
    private var suspendHold: CheckBox? = null
    private var partRow: View? = null
    private var partSpinner: Spinner? = null
    private var orgSpinner: Spinner? = null
    private var phoneNotificationLabel: TextView? = null
    private var smsNotificationLabel: TextView? = null
    private var smsSpinnerLabel: TextView? = null
    private var datePicker: DatePickerDialog? = null
    private var thawDatePicker: DatePickerDialog? = null
    private var thawDateEdittext: EditText? = null
    private var expireDate: Date? = null
    private var thawDate: Date? = null
    private var selectedOrgPos = 0
    private var selectedSMSPos = 0
    private var progress: ProgressDialogSupport? = null
    private var parts: List<OSRFObject>? = null
    private var titleHoldIsPossible: Boolean? = null
    private lateinit var record: MBRecord

    private val hasParts: Boolean
        get() = !(parts.isNullOrEmpty())
    private val partRequired: Boolean
        get() = hasParts && titleHoldIsPossible != true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRestarting) return

        setContentView(R.layout.place_hold)

        record = intent.getSerializableExtra("recordInfo") as MBRecord
        account = App.getAccount()
        progress = ProgressDialogSupport()
        title = findViewById(R.id.hold_title)
        author = findViewById(R.id.hold_author)
        format = findViewById(R.id.hold_format)
        placeHold = findViewById(R.id.place_hold)
        expirationDate = findViewById(R.id.hold_expiration_date)
        emailNotification = findViewById(R.id.hold_enable_email_notification)
        phoneNotificationLabel = findViewById(R.id.hold_phone_notification_label)
        smsNotificationLabel = findViewById(R.id.hold_sms_notification_label)
        smsSpinnerLabel = findViewById(R.id.hold_sms_spinner_label)
        phoneNotification = findViewById(R.id.hold_enable_phone_notification)
        phoneNotify = findViewById(R.id.hold_phone_notify)
        smsNotify = findViewById(R.id.hold_sms_notify)
        smsNotification = findViewById(R.id.hold_enable_sms_notification)
        smsSpinner = findViewById(R.id.hold_sms_carrier)
        suspendHold = findViewById(R.id.hold_suspend_hold)
        partRow = findViewById(R.id.hold_part_row)
        partSpinner = findViewById(R.id.hold_part_spinner)
        orgSpinner = findViewById(R.id.hold_pickup_location)
        thawDateEdittext = findViewById(R.id.hold_thaw_date)

        title?.text = record.title
        author?.text = record.author
        format?.text = record.iconFormatLabel
        emailNotification?.isChecked = account?.notifyByEmail ?: false

        initPhoneControls(resources.getBoolean(R.bool.ou_enable_phone_notification))
        initPlaceHoldButton()
        initSuspendHoldButton()
        initDatePickers()
        initOrgSpinner()

        // Prevent unnecessary UI flash by not calling initSMSControls yet.
        // Most consortia allow SMS notification, so disabling it here would
        // cause a flash when the orgs are loaded in fetchData.
        // By contrast, we do allow part controls to be disabled here,
        // because most items do not have parts.
        //initSMSControls()
        initPartControls()
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
                Log.d(TAG, "[kcxxx] fetchData ...")
                val start = System.currentTimeMillis()
                var jobs = mutableListOf<Job>()
                progress?.show(this@PlaceHoldActivity, getString(R.string.msg_loading_place_hold))
                placeHold?.isEnabled = false

                jobs.add(scope.async {
                    GatewayLoader.loadOrgSettingsAsync(null).await()
                    logOrgStats()
                })

                jobs.add(scope.async {
                    GatewayLoader.loadSMSCarriersAsync()
                })

                if (resources.getBoolean(R.bool.ou_enable_part_holds)) {
                    Log.d(TAG, "${record.title}: fetching parts")
                    jobs.add(scope.async {
                        val result = Gateway.search.fetchHoldParts(record.id)
                        onPartsResult(result)
                        if (hasParts && resources.getBoolean(R.bool.ou_enable_title_hold_on_item_with_parts)) {
                            Log.d(TAG, "${record.title}: checking titleHoldIsPossible")
                            val isPossibleResult = Gateway.circ.fetchTitleHoldIsPossible(App.getAccount(), record.id, App.getAccount().pickupOrg ?: 1)
                            onTitleHoldIsPossibleResult(isPossibleResult)
                        }
                    })
                }

                jobs.joinAll()
                initPartControls()
                initSMSControls()
                placeHold?.isEnabled = true
                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                Log.d(TAG, "[kcxxx] fetchData ... caught", ex)
                showAlert(ex)
            } finally {
                progress?.dismiss()
            }
        }
    }

    private fun logOrgStats() {
        if (Analytics.isDebuggable(this)) {
            EgOrg.dumpOrgStats()
        }
    }

    private fun onPartsResult(result: Result<List<OSRFObject>>) {
        when (result) {
            is Result.Success -> {
                parts = result.data
                Log.d(TAG, "${record.title}: ${parts?.size} parts found")
            }
            is Result.Error -> {
                showAlert(result.exception)
            }
        }
    }

    private fun onTitleHoldIsPossibleResult(result: Result<OSRFObject>) {
        titleHoldIsPossible = when (result) {
            is Result.Success -> true
            is Result.Error -> false
        }
        Log.d(TAG, "${record.title}: titleHoldIsPossible=$titleHoldIsPossible")
    }

    private fun logPlaceHoldResult(succeeded: Boolean, result: String) {
        val notify = ArrayList<String?>()
        if (emailNotification?.isChecked == true) notify.add("email")
        if (phoneNotification?.isChecked == true) notify.add("phone")
        if (smsNotification?.isChecked == true) notify.add("sms")

        //TODO: change HOLD_NOTIFY to dimensional value {default | +-email:+-phone:+-sms:

        val notifyTypes = TextUtils.join("|", notify)
        try {
            Analytics.logEvent(Analytics.Event.HOLD_PLACE_HOLD, bundleOf(
                    Analytics.Param.RESULT to result,
                    Analytics.Param.HOLD_NOTIFY to notifyTypes,
                    Analytics.Param.HOLD_EXPIRES_KEY to (expireDate != null),
                    Analytics.Param.HOLD_PICKUP_KEY to Analytics.orgDimensionKey(EgOrg.visibleOrgs[selectedOrgPos],
                            EgOrg.findOrg(App.getAccount().pickupOrg),
                            EgOrg.findOrg(App.getAccount().homeOrg)),
            ))
        } catch (e: Exception) {
            Analytics.logException(e)
        }
    }

    private fun initPlaceHoldButton() {
        placeHold?.setOnClickListener { placeHold() }
    }

    private fun getPhoneNotify(): String? {
        return if (phoneNotification?.isChecked == true) phoneNotify?.text.toString() else null
    }

    private fun getSMSNotify(): String? {
        return if (smsNotification?.isChecked == true) smsNotify?.text.toString() else null
    }

    private fun getSMSNotifyCarrier(id: Int): Int? {
        return if (smsNotification?.isChecked == true) id else null
    }

    private fun getExpireDate(): String? {
        return if (expireDate != null) OSRFUtils.formatDate(expireDate) else null
    }

    private fun getThawDate(): String? {
        return if (thawDate != null) OSRFUtils.formatDate(thawDate) else null
    }

    private fun placeHoldPreFlightCheck(): Boolean {
        val selectedOrg = EgOrg.visibleOrgs[selectedOrgPos]
        if (!selectedOrg.isPickupLocation) {
            val builder = AlertDialog.Builder(this@PlaceHoldActivity)
            builder.setTitle(getString(R.string.title_not_pickup_location))
                    .setMessage(getString(R.string.msg_not_pickup_location, selectedOrg.name))
                    .setPositiveButton(android.R.string.ok, null)
            builder.create().show()
            return false
        }
        if (partRequired && partSpinner?.selectedItem.toString().isEmpty()) {
            val builder = AlertDialog.Builder(this@PlaceHoldActivity)
            builder.setTitle(getString(R.string.title_no_part_selected))
                    .setMessage(getString(R.string.msg_no_part_selected))
                    .setPositiveButton(android.R.string.ok, null)
            builder.create().show()
            return false
        }
        if (phoneNotification?.isChecked == true && TextUtils.isEmpty(phoneNotify?.text.toString())) {
            phoneNotify?.error = getString(R.string.error_phone_notify_empty)
            return false
        }
        if (smsNotification?.isChecked == true && TextUtils.isEmpty(smsNotify?.text.toString())) {
            smsNotify?.error = getString(R.string.error_sms_notify_empty)
            return false
        }
        return true
    }

    private fun placeHold() {
        if (!placeHoldPreFlightCheck())
            return

        scope.async {
            Log.d(TAG, "[kcxxx] placeHold: ${record.id}")
            val selectedOrgID = if (EgOrg.visibleOrgs.size > selectedOrgPos) EgOrg.visibleOrgs[selectedOrgPos].id else -1
            val selectedSMSCarrierID = if (EgSms.carriers.size > selectedSMSPos) EgSms.carriers[selectedSMSPos].id else -1
            val holdType: String
            val itemId: Int
            when {
                partRequired || getPartId() > 0 -> { holdType = HOLD_TYPE_PART; itemId = getPartId() }
                else -> { holdType = HOLD_TYPE_TITLE; itemId = record.id }
            }
            progress?.show(this@PlaceHoldActivity, "Placing hold")
            val result = Gateway.circ.placeHoldAsync(App.getAccount(), holdType, itemId,
                    selectedOrgID, emailNotification?.isChecked == true, getPhoneNotify(), getSMSNotify(),
                    getSMSNotifyCarrier(selectedSMSCarrierID), getExpireDate(),
                    suspendHold?.isChecked == true, getThawDate(),
                    resources.getBoolean(R.bool.ou_enable_hold_use_override))
            Log.d(TAG, "[kcxxx] placeHold: $result")
            progress?.dismiss()
            when (result) {
                is Result.Success -> {
                    logPlaceHoldResult(true, Analytics.Value.OK)
                    Toast.makeText(this@PlaceHoldActivity, "Hold successfully placed", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@PlaceHoldActivity, HoldsActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    logPlaceHoldResult(false, result.exception.getCustomMessage())
                    showAlert(result.exception)
                }
            }
        }
    }

    private fun initSuspendHoldButton() {
        suspendHold?.setOnCheckedChangeListener { buttonView, isChecked -> thawDateEdittext?.isEnabled = isChecked }
    }

    private fun initPhoneControls(isPhoneNotifyVisible: Boolean) {
        // Allow phone_notify to be set even if UX is not visible
        val notifyPhoneNumber = account?.phoneNumber
        phoneNotify?.setText(notifyPhoneNumber)
        if (account?.notifyByPhone == true && !notifyPhoneNumber.isNullOrEmpty()) {
            phoneNotification?.isChecked = true
        }

        if (isPhoneNotifyVisible) {
            phoneNotification?.setOnCheckedChangeListener { buttonView, isChecked ->
                phoneNotify?.isEnabled = isChecked
            }
            phoneNotify?.isEnabled = (phoneNotification?.isChecked == true)
        } else {
            phoneNotificationLabel?.visibility = View.GONE
            phoneNotification?.visibility = View.GONE
            phoneNotify?.visibility = View.GONE
        }
    }

    private fun initSMSControls() {
        val notifySmsNumber = account?.smsNumber
        smsNotify?.setText(notifySmsNumber)
        if (account?.notifyBySMS == true && !notifySmsNumber.isNullOrEmpty()) {
            smsNotification?.isChecked = true
        }

        val enabled = EgOrg.smsEnabled
        if (enabled) {
            smsNotification?.setOnCheckedChangeListener { buttonView, isChecked ->
                smsSpinner?.isEnabled = isChecked
                smsNotify?.isEnabled = isChecked
            }
            smsNotify?.isEnabled = (smsNotification?.isChecked == true)
            smsSpinner?.isEnabled = (smsNotification?.isChecked == true)
            initSMSSpinner(account?.smsCarrier)
        }

        val visibility = if (enabled) View.VISIBLE else View.GONE
        smsNotificationLabel?.visibility = visibility
        smsSpinnerLabel?.visibility = visibility
        smsNotification?.visibility = visibility
        smsSpinner?.visibility = visibility
        smsNotify?.visibility = visibility
    }

    private fun initSMSSpinner(defaultCarrierID: Int?) {
        val entries = ArrayList<String>()
        val carriers: List<SMSCarrier> = EgSms.carriers
        for (i in carriers.indices) {
            val (id, name) = carriers[i]
            entries.add(name)
            if (id == defaultCarrierID) {
                selectedSMSPos = i
            }
        }
        val adapter = ArrayAdapter(this, R.layout.org_item_layout, entries)
        smsSpinner?.adapter = adapter
        smsSpinner?.setSelection(selectedSMSPos)
        smsSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSMSPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initDatePickers() {
        val cal = Calendar.getInstance()
        datePicker = DatePickerDialog(this,
                OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val chosenDate = Date(year - 1900, monthOfYear, dayOfMonth)
                    expireDate = chosenDate
                    val strDate = DateFormat.format("MMMM dd, yyyy", chosenDate)
                    expirationDate?.setText(strDate)
                }, cal[Calendar.YEAR], cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH])
        expirationDate?.setOnClickListener { datePicker?.show() }
        thawDatePicker = DatePickerDialog(this,
                OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    val chosenDate = Date(year - 1900, monthOfYear, dayOfMonth)
                    thawDate = chosenDate
                    val strDate = DateFormat.format("MMMM dd, yyyy", chosenDate)
                    thawDateEdittext?.setText(strDate)
                }, cal[Calendar.YEAR], cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH])
        thawDateEdittext?.setOnClickListener { thawDatePicker?.show() }
    }

    private fun initPartControls() {
        if (!hasParts) {
            partRow?.visibility = View.GONE
        } else {
            partRow?.visibility = View.VISIBLE
            initPartSpinner()
        }
    }

    private fun initPartSpinner() {
        val sentinelLabel = if (partRequired) "" else "- ${resources.getString(R.string.label_hold_any_part)} -"
        val labels = mutableListOf(sentinelLabel)
        parts?.let {
            for (elem in it) {
                elem.getString("label")?.let { label ->
                    labels.add(label)
                }
            }
        }
        val adapter = ArrayAdapter(this, R.layout.org_item_layout, labels)
        partSpinner?.adapter = adapter
    }

    private fun getPartId(): Int {
        // partSpinner[1] is parts[0] because we added a blank first entry
        //val index = partSpinner?.selectedItemPosition
        val label = partSpinner?.selectedItem.toString()
        val partObj = parts?.find { it.getString("label") == label }
        val partId = partObj?.getInt("id")
        return partId ?: -1
    }

    private fun initOrgSpinner() {
        val defaultOrgId = account?.pickupOrg
        val list = ArrayList<String>()
        for ((index, org) in EgOrg.visibleOrgs.withIndex()) {
            list.add(org.spinnerLabel)
            if (org.id == defaultOrgId) {
                selectedOrgPos = index
            }
        }
        val adapter: ArrayAdapter<String> = OrgArrayAdapter(this, R.layout.org_item_layout, list, true)
        orgSpinner?.adapter = adapter
        orgSpinner?.setSelection(selectedOrgPos)
        orgSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedOrgPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
        private val TAG = PlaceHoldActivity::class.java.simpleName
    }
}

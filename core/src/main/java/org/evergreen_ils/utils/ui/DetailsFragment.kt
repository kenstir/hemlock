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
package org.evergreen_ils.utils.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.NetworkImageView
import org.evergreen_ils.R
import org.evergreen_ils.android.Analytics
import org.evergreen_ils.android.App
import org.evergreen_ils.android.Log
import org.evergreen_ils.net.Gateway.getUrl
import org.evergreen_ils.net.Volley
import org.evergreen_ils.searchCatalog.CopyInformationActivity
import org.evergreen_ils.searchCatalog.RecordInfo
import org.evergreen_ils.searchCatalog.RecordLoader
import org.evergreen_ils.searchCatalog.RecordLoader.ResponseListener
import org.evergreen_ils.system.EgOrg.findOrg
import org.evergreen_ils.views.bookbags.BookBagUtils.showAddToListDialog
import org.evergreen_ils.views.holds.PlaceHoldActivity

class DetailsFragment : Fragment() {
    private var record: RecordInfo? = null
    private var orgID: Int? = null
    private var position: Int? = null
    private var total: Int? = null

    private var titleTextView: TextView? = null
    private var formatTextView: TextView? = null
    private var authorTextView: TextView? = null
    private var publisherTextView: TextView? = null
    private var seriesTextView: TextView? = null
    private var subjectTextView: TextView? = null
    private var synopsisTextView: TextView? = null
    private var isbnTextView: TextView? = null
    private var descriptionTextView: TextView? = null
    private var synopsisTableRow: View? = null
    private var subjectTableRow: View? = null
    private var seriesTableRow: View? = null
    private var isbnTableRow: View? = null
    private var placeHoldButton: Button? = null
    private var showCopiesButton: Button? = null
    private var onlineAccessButton: Button? = null
    private var addToBookbagButton: Button? = null
    private var recordImage: NetworkImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            record = savedInstanceState.getSerializable("recordInfo") as RecordInfo
            orgID = savedInstanceState.getInt("orgID")
            position = savedInstanceState.getInt("position")
            total = savedInstanceState.getInt("total")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("recordInfo", record)
        outState.putInt("orgID", orgID!!)
        outState.putInt("position", position!!)
        outState.putInt("total", total!!)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(
                R.layout.record_details_fragment, null) as LinearLayout

        val recordHeader = layout.findViewById<TextView>(R.id.record_header_text)
        titleTextView = layout.findViewById(R.id.record_details_simple_title)
        formatTextView = layout.findViewById(R.id.record_details_format)
        authorTextView = layout.findViewById(R.id.record_details_simple_author)
        publisherTextView = layout.findViewById(R.id.record_details_simple_publisher)
        seriesTextView = layout.findViewById(R.id.record_details_series_text)
        subjectTextView = layout.findViewById(R.id.record_details_subject_text)
        synopsisTextView = layout.findViewById(R.id.record_details_synopsis_text)
        isbnTextView = layout.findViewById(R.id.record_details_isbn_text)
        recordImage = layout.findViewById(R.id.record_details_simple_image)
        descriptionTextView = layout.findViewById(R.id.record_details_brief_description)
        placeHoldButton = layout.findViewById(R.id.simple_place_hold_button)
        showCopiesButton = layout.findViewById(R.id.show_copy_information_button)
        onlineAccessButton = layout.findViewById(R.id.record_details_online_button)
        addToBookbagButton = layout.findViewById(R.id.add_to_bookbag_button)
        synopsisTableRow = layout.findViewById(R.id.record_details_synopsis_row)
        seriesTableRow = layout.findViewById(R.id.record_details_series_row)
        subjectTableRow = layout.findViewById(R.id.record_details_subject_row)
        isbnTableRow = layout.findViewById(R.id.record_details_isbn_row)

        recordHeader.text = String.format(getString(R.string.record_of), position!! + 1, total)
        descriptionTextView?.text = ""
        initButtons()

        // Start async image load
        val imageHref = getUrl("/opac/extras/ac/jacket/medium/r/" + record!!.doc_id)
        val imageLoader = Volley.getInstance(activity).imageLoader
        recordImage?.setImageUrl(imageHref, imageLoader)
        //recordImage?.setDefaultImageResId(R.drawable.missing_art);//for screenshots

        // Start async record load
        fetchRecordInfo(record)

        return layout
    }

    private fun initButtons() {
        // disable most buttons until the record is loaded
        placeHoldButton!!.isEnabled = false
        showCopiesButton!!.isEnabled = false
        onlineAccessButton!!.isEnabled = false
        updateButtonViews()
        placeHoldButton!!.setOnClickListener {
//            Analytics.logEvent("placehold_view", "via", "details_button")
            val intent = Intent(activity?.applicationContext, PlaceHoldActivity::class.java)
            intent.putExtra("recordInfo", record)
            startActivity(intent)
        }
        showCopiesButton!!.setOnClickListener {
//            Analytics.logEvent("copyinfo_view", "via", "details_button")
            val intent = Intent(activity?.applicationContext, CopyInformationActivity::class.java)
            intent.putExtra("recordInfo", record)
            intent.putExtra("orgID", orgID)
            startActivity(intent)
        }
        onlineAccessButton!!.setOnClickListener {
//            Analytics.logEvent("onlineaccess_view", "via", "details_button")
            launchOnlineAccess()
        }
        addToBookbagButton!!.setOnClickListener {
            Analytics.logEvent("lists_addtolist", "via", "details_button")
            (activity as? BaseActivity)?.let {
                showAddToListDialog(it, App.getAccount().bookBags, record!!)
            }
        }
    }

    private fun launchURL(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        activity?.packageManager?.let {
            if (intent.resolveActivity(it) != null) {
                startActivity(intent)
            }
        }
    }

    private fun launchOnlineAccess() {
        val org = findOrg(orgID)
        val links = App.getBehavior().getOnlineLocations(record, org!!.shortname)
        if (links.isEmpty()) return // TODO: alert

        // if there's only one link, launch it without ceremony
        if (links.size == 1 && !resources.getBoolean(R.bool.ou_always_popup_online_links)) {
            launchURL(links[0].href)
            return
        }

        // show an alert dialog to choose between links
        val titles = arrayOfNulls<String>(links.size)
        for (i in titles.indices) titles[i] = links[i].text
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.record_online_access)
        builder.setItems(titles) { dialog, which -> launchURL(links[which].href) }
        builder.create().show()
    }

    private fun updateButtonViews() {
        val is_online_resource = App.getBehavior().isOnlineResource(record)
        Log.d(TAG, "yyy: updateButtonViews: title:" + record!!.title + " is_online_resource:" + is_online_resource)
        if (is_online_resource == null) return  // not ready yet
        placeHoldButton!!.isEnabled = true
        showCopiesButton!!.isEnabled = true
        onlineAccessButton!!.isEnabled = true
        if (is_online_resource) {
            val org = findOrg(orgID)
            val links = App.getBehavior().getOnlineLocations(record, org!!.shortname)
            Log.d(TAG, "yyy: updateButtonViews: title:" + record?.title + " links:" + links.size)
            if (links.isEmpty()) {
                onlineAccessButton?.isEnabled = false
            } else if (resources.getBoolean(R.bool.ou_show_online_access_hostname)) {
                val uri = Uri.parse(links[0].href)
                descriptionTextView?.text = uri.host
            }
        }
        onlineAccessButton?.visibility = if (is_online_resource) View.VISIBLE else View.GONE
        placeHoldButton?.visibility = if (is_online_resource) View.GONE else View.VISIBLE
        showCopiesButton?.visibility = if (is_online_resource) View.GONE else View.VISIBLE
    }

    private fun updateIconFormatView() {
        if (!isAdded) return  // discard late results
        formatTextView?.text = RecordInfo.getIconFormatLabel(record)
        updateButtonViews()
    }

    private fun updateBasicMetadataViews() {
        if (!isAdded) return  // discard late results
        titleTextView?.text = record?.title
        authorTextView?.text = record?.author
        publisherTextView?.text = record?.publishingInfo
        synopsisTextView?.text = record?.synopsis
        seriesTextView?.text = record?.series
        seriesTableRow?.visibility = if (TextUtils.isEmpty(record?.series)) View.GONE else View.VISIBLE
        subjectTextView?.text = record?.subject
        subjectTableRow?.visibility = if (TextUtils.isEmpty(record?.subject)) View.GONE else View.VISIBLE
        isbnTextView?.text = record?.isbn
        isbnTableRow?.visibility = if (TextUtils.isEmpty(record?.isbn)) View.GONE else View.VISIBLE
        updateButtonViews()
    }

    private fun fetchRecordInfo(record: RecordInfo?) {
        RecordLoader.fetchDetailsMetadata(record, activity,
                object : ResponseListener {
                    override fun onMetadataLoaded() {
                        Log.d(TAG, "yyyyy: onMetadataLoaded()")
                        updateBasicMetadataViews()
                        fetchCopyCountInfo(record)
                    }

                    override fun onIconFormatLoaded() {
                        Log.d(TAG, "yyyyy: onIconFormatLoaded()")
                        updateIconFormatView()
                        fetchCopyCountInfo(record)
                    }
                })
    }

    private fun fetchCopyCountInfo(record: RecordInfo?) {
        // Check for copy counts only after we know it is not an online_resource.
        val isOnlineResource = App.getBehavior().isOnlineResource(record)
        Log.d(TAG, "fetchCopyCountInfo id=" + record?.doc_id
                + " is_online_resource=" + isOnlineResource)
        if (isOnlineResource == null) return  // not ready yet
        if (!isOnlineResource && record != null
                && !record.copy_summary_loaded) {
            RecordLoader.fetchCopySummary(record, orgID!!, context) { updateCopyCountView() }
        } else {
            Log.d(TAG, "not updating copy count view for some reason, STOPHERE")
        }
    }

    private fun updateCopyCountView() {
        if (!isAdded) return  // discard late results
        descriptionTextView?.text = RecordLoader.getCopySummary(record, orgID!!, context)
    }

    companion object {
        private val TAG = DetailsFragment::class.java.simpleName
        @JvmStatic
        fun newInstance(record: RecordInfo?, position: Int?, total: Int?, orgID: Int?): DetailsFragment {
            val fragment = DetailsFragment()
            fragment.record = record
            fragment.orgID = orgID
            fragment.position = position
            fragment.total = total
            return fragment
        }
    }
}

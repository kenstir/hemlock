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
package org.evergreen_ils.utils.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.NetworkImageView
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import org.evergreen_ils.KEY_SEARCH_BY
import org.evergreen_ils.KEY_SEARCH_TEXT
import net.kenstir.hemlock.R
import net.kenstir.hemlock.android.App
import net.kenstir.hemlock.logging.Log
import net.kenstir.hemlock.logging.Log.TAG_ASYNC
import net.kenstir.hemlock.android.ui.showAlert
import org.evergreen_ils.net.Gateway.getUrl
import org.evergreen_ils.net.GatewayLoader
import org.evergreen_ils.net.Volley
import org.evergreen_ils.views.search.CopyInformationActivity
import org.evergreen_ils.data.MBRecord
import org.evergreen_ils.system.EgOrg
import org.evergreen_ils.views.bookbags.BookBagUtils.showAddToListDialog
import org.evergreen_ils.views.holds.PlaceHoldActivity
import org.evergreen_ils.views.search.SearchActivity
import org.evergreen_ils.views.search.SearchActivity.Companion.RESULT_CODE_SEARCH_BY_AUTHOR

class DetailsFragment : Fragment() {
    private var record: MBRecord? = null
    private var orgID: Int = EgOrg.consortiumID
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
    private var subjectTableRow: View? = null
    private var seriesTableRow: View? = null
    private var isbnTableRow: View? = null
    private var placeHoldButton: Button? = null
    private var showCopiesButton: Button? = null
    private var onlineAccessButton: Button? = null
    private var addToBookbagButton: Button? = null
    private var extrasButton: Button? = null
    private var recordImage: NetworkImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            record = savedInstanceState.getSerializable("recordInfo") as MBRecord
            orgID = savedInstanceState.getInt("orgID")
            position = savedInstanceState.getInt("position")
            total = savedInstanceState.getInt("total")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("recordInfo", record)
        outState.putInt("orgID", orgID)
        outState.putInt("position", position!!)
        outState.putInt("total", total!!)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG_ASYNC, "${record?.id}: oncreateview")
        val layout = inflater.inflate(
                R.layout.record_details_fragment, container, false) as LinearLayout

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
        extrasButton = layout.findViewById(R.id.extras_button)
        seriesTableRow = layout.findViewById(R.id.record_details_series_row)
        subjectTableRow = layout.findViewById(R.id.record_details_subject_row)
        isbnTableRow = layout.findViewById(R.id.record_details_isbn_row)

        recordHeader.text = String.format(getString(R.string.record_of), position!! + 1, total)
        descriptionTextView?.text = ""
        initButtons()

        // Start async image load
        val url = getUrl("/opac/extras/ac/jacket/medium/r/" + record?.id)
        Log.d(TAG_ASYNC, "${record?.id}: setimageurl $url")
        recordImage?.setImageUrl(url, Volley.getInstance(activity).imageLoader)
        //recordImage?.setDefaultImageResId(R.drawable.missing_art);//for screenshots

        record?.let { fetchData(it) }

        return layout
    }

    private fun initButtons() {
        // disable most buttons until the record is loaded
        placeHoldButton?.isEnabled = false
        showCopiesButton?.isEnabled = false
        onlineAccessButton?.isEnabled = false
        updateButtonViews()
        placeHoldButton?.setOnClickListener {
            val intent = Intent(activity?.applicationContext, PlaceHoldActivity::class.java)
            intent.putExtra("recordInfo", record)
            startActivity(intent)
        }
        showCopiesButton?.setOnClickListener {
            val intent = Intent(activity?.applicationContext, CopyInformationActivity::class.java)
            intent.putExtra("recordInfo", record)
            intent.putExtra("orgID", orgID)
            startActivity(intent)
        }
        onlineAccessButton?.setOnClickListener {
            launchOnlineAccess()
        }
        addToBookbagButton?.setOnClickListener {
            //Analytics.logEvent("lists_addtolist", "via", "details_button")
            (activity as? BaseActivity)?.let {
                record?.let { record ->
                    showAddToListDialog(it, App.getAccount().patronLists, record)
                }
            }
        }
        val extrasLinkText = resources.getString(R.string.ou_details_link_text)
        if (extrasLinkText.isEmpty()) {
            extrasButton?.visibility = View.GONE
        } else {
            extrasButton?.text = extrasLinkText
            extrasButton?.setOnClickListener {
                val url = StringBuilder(resources.getString(R.string.ou_library_url))
                url.append("/eg/opac/record/").append(record?.id)
                val q = resources.getString(R.string.ou_details_link_query)
                if (q.isNotEmpty()) {
                    url.append("?").append(q)
                }
                val frag = resources.getString(R.string.ou_details_link_fragment)
                if (frag.isNotEmpty()) {
                    url.append("#").append(frag)
                }
                launchURL(url.toString())
            }
        }
    }

    private fun launchURL(url: String) {
        (activity as? BaseActivity)?.launchURL(url)
    }

    private fun launchOnlineAccess() {
        val org = EgOrg.findOrg(orgID)
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
        builder.setItems(titles) { _, which -> launchURL(links[which].href) }
        builder.create().show()
    }

    private fun updateButtonViews() {
        Log.d(TAG, "${record?.id}: updateButtonViews: title:${record?.title}")

        if (record?.isDeleted == true) {
            placeHoldButton?.isEnabled = false
            showCopiesButton?.isEnabled = false
            onlineAccessButton?.isEnabled = false
            addToBookbagButton?.isEnabled = false
            return
        }

        val org = EgOrg.findOrg(orgID)
        val links = App.getBehavior().getOnlineLocations(record, org!!.shortname)
        val numCopies = record?.totalCopies(orgID) ?: 0
        placeHoldButton?.isEnabled = (numCopies > 0)
        showCopiesButton?.isEnabled = (numCopies > 0)
        Log.d(TAG, "${record?.id}: updateButtonViews: title:${record?.title} links:${links.size} copies:${numCopies}")
        if (links.isEmpty()) {
            onlineAccessButton?.isEnabled = false
            onlineAccessButton?.visibility = View.GONE
        } else {
            onlineAccessButton?.isEnabled = true
            onlineAccessButton?.visibility = View.VISIBLE

            if (resources.getBoolean(R.bool.ou_show_online_access_hostname)) {
                val uri = Uri.parse(links[0].href)
                descriptionTextView?.text = uri.host
            }
        }
    }

    private fun loadFormat() {
        if (!isAdded) return  // discard late results
        formatTextView?.text = record?.iconFormatLabel
    }

    private fun loadMetadata() {
        if (!isAdded) return  // discard late results
        titleTextView?.text = record?.title
        val ss = SpannableString(record?.author)
        ss.setSpan(URLSpan(""), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        authorTextView?.setText(ss, TextView.BufferType.SPANNABLE)
        authorTextView?.setOnClickListener { searchByAuthor() }
        publisherTextView?.text = record?.publishingInfo
        synopsisTextView?.text = record?.synopsis
        seriesTextView?.text = record?.series
        seriesTableRow?.visibility = if (TextUtils.isEmpty(record?.series)) View.GONE else View.VISIBLE
        subjectTextView?.text = record?.subject
        subjectTableRow?.visibility = if (TextUtils.isEmpty(record?.subject)) View.GONE else View.VISIBLE
        isbnTextView?.text = record?.isbn
        isbnTableRow?.visibility = if (TextUtils.isEmpty(record?.isbn)) View.GONE else View.VISIBLE
        //updateButtonViews()
    }

    private fun searchByAuthor() {
        val author = record?.author ?: return
        // Instead of setResult and finish, we clear the activity stack and push SearchActivity.
        // A finish would work only from Search Details, not from List Details.
        val intent = Intent(activity, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(KEY_SEARCH_TEXT, author)
        intent.putExtra(KEY_SEARCH_BY, RESULT_CODE_SEARCH_BY_AUTHOR)
        startActivity(intent)
    }

    private fun loadCopySummary() {
        if (!isAdded) return  // discard late results
        val record = this.record ?: return
        descriptionTextView?.text = when {
            record.isDeleted -> getString(R.string.item_marked_deleted_msg)
            App.getBehavior().isOnlineResource(record) ?: false -> {
                val onlineLocation = record.getFirstOnlineLocation()
                if (resources.getBoolean(R.bool.ou_show_online_access_hostname) && !onlineLocation.isNullOrEmpty()) {
                    val uri = Uri.parse(onlineLocation)
                    uri.host
                } else {
                    ""
                }
            }
            else -> record.getCopySummary(resources, orgID)
        }
    }

    private fun fetchData(record: MBRecord) {
        val scope = viewLifecycleOwner.lifecycleScope
        scope.async {
            try {
                //Log.d(TAG_ASYNC, "fetchData ...")
                Log.d(TAG_ASYNC, "${record.id}: fetchData")
                val start = System.currentTimeMillis()
                val jobs = mutableListOf<Job>()

                jobs.add(scope.async {
                    GatewayLoader.loadRecordMetadataAsync(record)
                    loadMetadata()
                    Log.d(TAG_ASYNC, "${record.id}: loadRecordMetadataAsync done")
                })

                jobs.add(scope.async {
                    GatewayLoader.loadRecordAttributesAsync(record)
                    loadFormat()
                    Log.d(TAG_ASYNC, "${record.id}: loadRecordAttributesAsync done")
                })

                jobs.add(scope.async {
                    GatewayLoader.loadRecordCopyCountAsync(record, orgID)
                    // do not loadCopySummary yet, we need the MARC
                    Log.d(TAG_ASYNC, "${record.id}: loadRecordCopyCountAsync done")
                })

                if (resources.getBoolean(R.bool.ou_need_marc_record)) {
                    jobs.add(scope.async {
                        GatewayLoader.loadRecordMarcAsync(record)
                        Log.d(TAG_ASYNC, "${record.id}: loadRecordMarcAsync done")
                    })
                }

                jobs.joinAll()

                loadCopySummary()
                updateButtonViews()

                Log.logElapsedTime(TAG, start, "[kcxxx] fetchData ... done")
            } catch (ex: Exception) {
                activity?.showAlert(ex)
            }
        }
    }

    companion object {
        private val TAG = DetailsFragment::class.java.simpleName

        fun create(record: MBRecord?, orgID: Int, position: Int, total: Int): DetailsFragment {
            val fragment = DetailsFragment()
            fragment.record = record
            fragment.orgID = orgID
            fragment.position = position
            fragment.total = total
            return fragment
        }
    }
}

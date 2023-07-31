/*
 * Copyright (c) 2023 Kenneth H. Cox
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package net.kenstir.apps.acorn

import android.os.Bundle
import androidx.annotation.Keep
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.evergreen_ils.R
import org.evergreen_ils.android.Log
import org.evergreen_ils.data.GridButton
import org.evergreen_ils.utils.ui.BaseActivity

const val SPAN_COUNT = 2
x = y

@Keep
class GridButtonsActivity : BaseActivity() {
    private val TAG = javaClass.simpleName

    private var rv: RecyclerView? = null
    private var adapter: GridButtonViewAdapter? = null
    private var items = ArrayList<GridButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // not super
        if (isRestarting) return

        Log.d(TAG, object{}.javaClass.enclosingMethod?.name ?: "")

        setContentView(R.layout.activity_grid_buttons)

        rv = findViewById(R.id.recycler_view)
        rv?.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        adapter = GridButtonViewAdapter(items)
        rv?.adapter = adapter
    }

}

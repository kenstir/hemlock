package net.kenstir.ui.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import net.kenstir.hemlock.R
import org.evergreen_ils.system.EgOrg

class OrgArrayAdapter(
    context: Context,
    resourceId: Int,
    items: List<String>,
    private val forPickup: Boolean
) : ArrayAdapter<String>(context, resourceId, items) {
    override fun isEnabled(pos: Int): Boolean {
        if (!forPickup) return true
        val org = EgOrg.visibleOrgs[pos]
        return org.isPickupLocation
    }

    override fun getDropDownView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val v = super.getDropDownView(pos, convertView, parent)
        val org = EgOrg.visibleOrgs[pos]
        if (v is TextView) {
            v.setTextAppearance(
                context,
                if (org.canHaveUsers) R.style.HemlockText_SpinnerSecondary else R.style.HemlockText_SpinnerPrimary
            )
        }
        v.isEnabled = isEnabled(pos)
        return v
    }
}

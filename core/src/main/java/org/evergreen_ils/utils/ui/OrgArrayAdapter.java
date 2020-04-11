package org.evergreen_ils.utils.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.evergreen_ils.R;
import org.evergreen_ils.system.EgOrg;
import org.evergreen_ils.data.Organization;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OrgArrayAdapter extends ArrayAdapter<String> {
    protected boolean forPickup;

    public OrgArrayAdapter(@NonNull Context context, int resource, @NonNull List objects, boolean forPickup) {
        super(context, resource, objects);
        this.forPickup = forPickup;
    }

    @Override
    public boolean isEnabled(int pos) {
        if (!forPickup)
            return true;
        Organization org = EgOrg.getVisibleOrgs().get(pos);
        return org.isPickupLocation();
    }

    @Override
    public View getDropDownView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = super.getDropDownView(pos, convertView, parent);
        Organization org = EgOrg.getVisibleOrgs().get(pos);
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            //tv.setTypeface(org.orgType.can_have_users ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
            tv.setTextAppearance(getContext(), org.getOrgType().getCanHaveUsers() ? R.style.HemlockText_SpinnerSecondary : R.style.HemlockText_SpinnerPrimary);
        }
        v.setEnabled(isEnabled(pos));
        return v;
    }
}

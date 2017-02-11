package org.evergreen_ils.system;

/**
 * Created by kenstir on 2/11/2017.
 */

public class OrgType {
    public String name = null;
    public Integer id = null;
//    public Integer parent = null;
//    public Integer depth = null;
    public String opac_label = null;
    public boolean can_have_users = false;
    public boolean can_have_vols = false;

    public OrgType() {
    }
}

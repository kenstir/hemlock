//
//  Copyright (C) 2019 Kenneth H. Cox
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.evergreen_ils.utils;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

// TODO: remove Serializable, that will make the TransactionTooLargeException issue worse
public class MARCRecord implements Serializable {
    public static boolean isOnlineLocation(String tag, String ind1, String ind2) {
        return (TextUtils.equals(tag, "856")
                && TextUtils.equals(ind1, "4")
                && (TextUtils.equals(ind2, "0")
                || TextUtils.equals(ind2, "1")
                || TextUtils.equals(ind2, "2")));
    }

    public static boolean isTitleStatement(String tag) {
        return (TextUtils.equals(tag, "245"));
    }

    public static boolean isDatafieldUseful(String tag, String ind1, String ind2) {
        return isOnlineLocation(tag, ind1, ind2) || isTitleStatement(tag);
    }

    public static class MARCSubfield implements Serializable {
        public String code;
        public String text = null;

        public MARCSubfield(String code) {
            this.code = code;
        }
    }

    public static class MARCDatafield implements Serializable {
        public String tag;
        public String ind1;
        public String ind2;
        public List<MARCSubfield> subfields = new ArrayList<>();

        public MARCDatafield(String tag, String ind1, String ind2) {
            this.tag = tag;
            this.ind1 = ind1;
            this.ind2 = ind2;
        }

        public boolean isOnlineLocation() {
            return MARCRecord.isOnlineLocation(tag, ind1, ind2);
        }

        public boolean isTitleStatement() {
            return MARCRecord.isTitleStatement(tag);
        }

        // only valid if isTitleStatement
        @Nullable
        public Integer getNonFilingCharacters() {
            try {
                Integer numNonFilingCharacters = Integer.valueOf(ind2);
                return numNonFilingCharacters;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public String getUri() {
            for (MARCSubfield sf: subfields) {
                if (TextUtils.equals(sf.code, "u"))
                    return sf.text;
            }
            return null;
        }

        public String getLinkText() {
            for (MARCSubfield sf: subfields) {
                if (TextUtils.equals(sf.code, "y"))
                    return sf.text;
            }
            for (MARCSubfield sf: subfields) {
                if (TextUtils.equals(sf.code, "z") || TextUtils.equals(sf.code, "3"))
                    return sf.text;
            }
            return "Tap to access";
        }
    }

    public List<MARCDatafield> datafields = new ArrayList<>();

    public MARCRecord() {
    }

    public List<Link> getLinks() {
        ArrayList<Link> links = new ArrayList<>();
        for (MARCDatafield df: datafields) {
            if (df.isOnlineLocation()) {
                String href = null;
                String text = null;
                for (MARCSubfield sf: df.subfields) {
                    if (TextUtils.equals(sf.code, "u") && href == null) href = sf.text;
                    if ((TextUtils.equals(sf.code, "3") || TextUtils.equals(sf.code, "y")) && text == null) text = sf.text;
                }
                if (href != null && text != null) {
                    links.add(new Link(href, text));
                }
            }
        }
        return links;
    }
}
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MARCXMLParser {

    MARCRecord currentRecord = new MARCRecord();
    MARCRecord.MARCDatafield currentDatafield = null;
    MARCRecord.MARCSubfield currentSubfield = null;
    InputStream inStream;

    public MARCXMLParser() {
    }

    public MARCXMLParser(String fileName) throws IOException {
        this(new FileInputStream(fileName));
    }

    public MARCXMLParser(InputStream inStream) {
        this();
        this.inStream = inStream;
    }

    public MARCRecord parse() throws Exception {

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(this.inStream, null);
            int eventType = parser.getEventType();

            // cycle through the XML events
            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch(eventType) {
                    case XmlPullParser.START_TAG:
                        handleStartElement(parser);
                        break;
                    case XmlPullParser.END_TAG:
                        handleEndElement(parser);
                        break;
                    case XmlPullParser.TEXT:
                        handleText(parser);
                        break;
                }
                eventType = parser.next();
            }
        } catch(XmlPullParserException se) {
            throw new Exception("Error parsing MARCXML", se);
        }

        return currentRecord;
    }

    private void handleStartElement(XmlPullParser parser) throws IOException, XmlPullParserException {
        final String ns = null;
        String name = parser.getName();
//        System.out.println("kcxxx: start="+name);
        if ("datafield".equals(name)) {
            String tag = parser.getAttributeValue(ns, "tag");
            String ind1 = parser.getAttributeValue(ns, "ind1");
            String ind2 = parser.getAttributeValue(ns, "ind2");
            // We only care about certain 856 tags
            // See also templates/opac/parts/misc_util.tt2
            // See also https://www.loc.gov/marc/bibliographic/bd856.html
            if (TextUtils.equals(tag, "856")
                && TextUtils.equals(ind1, "4")
                && (TextUtils.equals(ind2, "0") || TextUtils.equals(ind2, "1"))) {
                currentDatafield = new MARCRecord.MARCDatafield(tag, ind1, ind2);
            }
        } else if ("subfield".equals(name)) {
            String code = parser.getAttributeValue(ns, "code");
            if (currentDatafield != null) {
                currentSubfield = new MARCRecord.MARCSubfield(code);
            }
        }
    }

    private void handleEndElement(XmlPullParser parser) throws Exception {
        String name = parser.getName();
//        System.out.println("kcxxx: end="+name);
        if ("datafield".equals(name)) {
            if (currentDatafield != null) {
                currentRecord.datafields.add(currentDatafield);
                currentDatafield = null;
            }
        } else if ("subfield".equals(name)) {
            if (currentDatafield != null && currentSubfield != null) {
                currentDatafield.subfields.add(currentSubfield);
                currentSubfield = null;
            }
        }
    }

    public void handleText(XmlPullParser parser) {
        String text = parser.getText();
        if (currentSubfield != null) {
//            System.out.println("kcxxx: text="+text);
            currentSubfield.text = text;
        }
    }
}

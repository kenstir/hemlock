/*
 * Copyright (C) 2018 Kenneth H. Cox
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.evergreen_ils.accountAccess.barcode;

import android.os.Bundle;
import android.widget.TextView;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.utils.ui.BaseActivity;

public class BarcodeActivity extends BaseActivity {

    private TextView barcode_text = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mRestarting) return;

        setContentView(R.layout.activity_barcode);

        barcode_text = findViewById(R.id.barcode_text);
        String barcode = AccountAccess.getInstance().getBarcode();
        barcode_text.setText(barcode);
    }

    /*
Bitmap createBarcode(String data) throws WriterException {
    int size = 500;
    MultiFormatWriter barcodeWriter = new MultiFormatWriter();

    BitMatrix barcodeBitMatrix = barcodeWriter.encode(data, BarcodeFormat.AZTEC, size, size);
    Bitmap barcodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    for (int x = 0; x < size; x++) {
        for (int y = 0; y < size; y++) {
            barcodeBitmap.setPixel(x, y, barcodeBitMatrix.get(x, y) ?
                    Color.BLACK : Color.TRANSPARENT);
        }
    }
    return barcodeBitmap;
}
     */
}

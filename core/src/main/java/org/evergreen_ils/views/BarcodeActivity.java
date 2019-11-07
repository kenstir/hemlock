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

package org.evergreen_ils.views;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;

import org.evergreen_ils.R;
import org.evergreen_ils.accountAccess.AccountAccess;
import org.evergreen_ils.utils.BarcodeUtils;
import org.evergreen_ils.utils.ui.BaseActivity;

public class BarcodeActivity extends BaseActivity {

    private final static String TAG = BarcodeActivity.class.getSimpleName();

    private TextView barcode_text = null;
    private ImageView image_view = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isRestarting()) return;

        setContentView(R.layout.activity_barcode);

        barcode_text = findViewById(R.id.barcode_text);
        String barcode = AccountAccess.getInstance().getBarcode();

        image_view = findViewById(R.id.barcode_image);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int image_width = Math.min(metrics.widthPixels, metrics.heightPixels) * 8 / 10;
        int image_height = image_width * 4 / 10;
        Bitmap bitmap = createBitmap(barcode, image_width, image_height);
        if (bitmap != null) {
            barcode_text.setText(barcode);
            image_view.setImageBitmap(bitmap);
        } else {
            barcode_text.setText(getString(R.string.invalid_barcode, barcode));
            image_view.setImageResource(R.drawable.invalid_barcode);
        }
    }

    private Bitmap createBitmap(String data, int image_width, int image_height) {
        if (image_width <= 0 || image_height <= 0)
            return null;

        // Try formats until we successfully encode the data
        BitMatrix bitMatrix = null;
        if (bitMatrix == null)
            bitMatrix = BarcodeUtils.tryEncode(data, image_width, image_height, BarcodeFormat.CODABAR);
        if (bitMatrix == null)
            bitMatrix = BarcodeUtils.tryEncode(data, image_width, image_height, BarcodeFormat.CODE_39);
        if (bitMatrix == null)
            return null;

        // Create a Bitmap from the BitMatrix
        Bitmap bitmap = Bitmap.createBitmap(image_width, image_height, Bitmap.Config.ARGB_8888);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }
}

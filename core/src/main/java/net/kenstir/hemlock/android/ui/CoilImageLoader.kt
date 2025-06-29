/*
 * Copyright (c) 2025 Kenneth H. Cox
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
 * along with this program; if not, see <https://www.gnu.org/licenses/>.
 */

package net.kenstir.hemlock.android.ui

// Not ready to use yet, but it looks like Coil is the right library to replace Volley's NetworkImageView.
// 1. Add Coil dependency in build.gradle
//    implementation 'io.coil-kt:coil:2.5.0'
// 2. Create a custom ImageLoader
// 3. Use Coil's ImageView extension to load images
object CoilImageLoader {
    // Example of a custom ImageLoader
//    val imageLoader = ImageLoader.Builder(context)
//        .okHttpClient { okHttpClient }
//        .crossfade(true)
//        .build()
//    Coil.setImageLoader(imageLoader)

    // Example of loading an image into an ImageView
    // imageView.load("https://example.com/image.jpg") {
    //     placeholder(R.drawable.placeholder)
    //     error(R.drawable.error)
    // }

}

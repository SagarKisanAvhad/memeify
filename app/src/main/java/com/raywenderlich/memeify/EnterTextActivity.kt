/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.memeify

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import kotlinx.android.synthetic.main.activity_enter_text.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EnterTextActivity : Activity(), View.OnClickListener {

  private var viewBitmap: Bitmap? = null
  private var pictureUri: Uri? = null
  private var originalImage = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_enter_text)

    writeTextToImageButton.setOnClickListener(this)
    saveImageButton.setOnClickListener(this)

    originalImage = true
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.writeTextToImageButton -> {}
      R.id.saveImageButton -> {}
      else -> println("No case satisfied")
    }
  }

  private fun askForPermissions() {
    @PermissionChecker.PermissionResult val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE)
    } else {
      saveImageToGallery(viewBitmap)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE) {
      // If request is cancelled, the result arrays are empty.
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        // Permission was granted, yay!
        saveImageToGallery(viewBitmap)
      } else {
        Toaster.show(this, R.string.permissions_please)
      }
    }
  }

  private fun createMeme() {
    // Get strings to place into image
    val topText = top_text_edittext.text.toString()
    val bottomText = bottom_text_edittext.text.toString()

    if (!originalImage) {
      pictureUri?.let {
        val bm = BitmapResizer.shrinkBitmap(this, it, selectedPictureImageview.width, selectedPictureImageview.height)
        selectedPictureImageview.setImageBitmap(bm)
      }
    }

    // get bitmap from imageView and copy to make mutable
    val imageDrawable = selectedPictureImageview.drawable as BitmapDrawable
    viewBitmap = imageDrawable.bitmap
    viewBitmap = viewBitmap!!.copy(viewBitmap!!.config, true)

    // add the text you want to your bitmap
    viewBitmap?.let {
      addTextToBitmap(it, topText, bottomText)
    }

    // set your imageview to show your newly edited bitmap to
    selectedPictureImageview.setImageBitmap(viewBitmap)
    originalImage = false
  }

  private fun addTextToBitmap(viewBitmap: Bitmap, topText: String, bottomText: String) {
    // get dimensions of image
    val bitmapWidth = viewBitmap.width
    val bitmapHeight = viewBitmap.height

    // create a canvas that uses the bitmap as its base
    val pictureCanvas = Canvas(viewBitmap)

    // create paint object with font parameters
    val tf = Typeface.create(HELVETICA_FONT, Typeface.BOLD)

    val textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f,
        resources.displayMetrics).toInt()

    val textPaint = Paint()
    textPaint.textSize = textSize.toFloat()
    textPaint.color = Color.WHITE
    textPaint.typeface = tf
    textPaint.textAlign = Paint.Align.CENTER

    val textPaintOutline = Paint()
    textPaintOutline.isAntiAlias = true
    textPaintOutline.textSize = textSize.toFloat()
    textPaintOutline.color = Color.BLACK
    textPaintOutline.typeface = tf
    textPaintOutline.style = Paint.Style.STROKE
    textPaintOutline.textAlign = Paint.Align.CENTER
    textPaintOutline.strokeWidth = 8f

    val xPos = (bitmapWidth / 2).toFloat()
    var yPos = (bitmapHeight / 7).toFloat()

    pictureCanvas.drawText(topText, xPos, yPos, textPaintOutline)
    pictureCanvas.drawText(topText, xPos, yPos, textPaint)

    yPos = (bitmapHeight - bitmapHeight / 14).toFloat()

    pictureCanvas.drawText(bottomText, xPos, yPos, textPaintOutline)
    pictureCanvas.drawText(bottomText, xPos, yPos, textPaint)
  }

  private fun saveImageToGallery(memeBitmap: Bitmap?) {
    if (!originalImage) {
      // save bitmap to file
      memeBitmap?.let {
        val imageFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), it.toString() + FILE_SUFFIX_JPG)

        try {
          // create output stream, compress image and write to file, flush and close outputstream
          val fos = FileOutputStream(imageFile)
          it.compress(Bitmap.CompressFormat.JPEG, 85, fos)
          fos.flush()
          fos.close()
        } catch (e: IOException) {
          Toaster.show(this, R.string.save_image_failed)
        }

        Toaster.show(this, R.string.save_image_succeeded)
      }
    } else {
      Toaster.show(this, R.string.add_meme_message)
    }
  }

  companion object {
    const private val FILE_SUFFIX_JPG = ".jpg"
    const private val HELVETICA_FONT = "Helvetica"
    const private val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 42
  }
}

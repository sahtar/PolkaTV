package ru.polkatv.launcher.ui.settings

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCode {
    /** Black-on-white QR bitmap of [sizePx] pixels for [text]. */
    fun render(text: String, sizePx: Int): Bitmap {
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, mapOf(EncodeHintType.MARGIN to 1))
        val px = IntArray(sizePx * sizePx) { i -> if (m[i % sizePx, i / sizePx]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(px, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}

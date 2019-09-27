package io.github.cnaos.blescanner.ui.devicelist

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter


@BindingAdapter("app:tint")
fun ImageView.setTint(color: String) {
    val color = Color.parseColor(color)
    this.setColorFilter(color)
}

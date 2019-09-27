package io.github.cnaos.blescanner.ui.devicelist

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.lelloman.identicon.view.ClassicIdenticonView


@BindingAdapter("app:tint")
fun ImageView.setTint(color: String) {
    val color = Color.parseColor(color)
    this.setColorFilter(color)
}

@BindingAdapter("app:hash")
fun ClassicIdenticonView.setHash(hash: String) {
    this.hash = hash.hashCode()
}

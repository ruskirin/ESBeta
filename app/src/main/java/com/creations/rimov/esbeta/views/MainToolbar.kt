package com.creations.rimov.esbeta.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Toolbar
import androidx.core.view.isVisible
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.visible

class MainToolbar(context: Context, attrs: AttributeSet) : Toolbar(context, attrs) {

    private val bookBtnPrev by lazy { findViewById<View>(R.id.bookPrev)}
    private val bookBtnNext by lazy { findViewById<View>(R.id.bookNext)}

    fun vanishPrev() {

        bookBtnPrev?.apply {
            if(isVisible) gone()
            else visible()
        }
    }

    fun vanishNext() {

        bookBtnNext?.apply {
            if(isVisible) gone()
            else visible()
        }
    }

    fun visibleBookNav() {
        if(bookBtnPrev == null || bookBtnNext == null) return

        if(bookBtnPrev.isVisible && bookBtnNext.isVisible) return

        bookBtnPrev.visible()
        bookBtnNext.visible()
    }
}
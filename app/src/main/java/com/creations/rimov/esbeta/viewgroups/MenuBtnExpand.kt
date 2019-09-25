package com.creations.rimov.esbeta.viewgroups

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.visible
import kotlinx.android.synthetic.main.menu_button_expandable.view.*

class MenuBtnExpand(context: Context, attrs: AttributeSet?)
    : RelativeLayout(context, attrs) {

    private val btnExpand: Button by lazy {menuBtnExpand}
    private val textDetail: TextView by lazy {menuDetailText}

    init {
        View.inflate(context, R.layout.menu_button_expandable, this)

        attrs?.let { set ->
            context.obtainStyledAttributes(set, R.styleable.MenuBtnExpand)
                .let {
                    btnExpand.text = it.getString(
                        R.styleable.MenuBtnExpand_button_text) ?: "def name"
                    textDetail.text = it.getString(
                        R.styleable.MenuBtnExpand_detail_text) ?: "def text"

                    it.recycle()
                }
        }

        btnExpand.setOnClickListener {
            toggleDetail()
        }
    }

    fun setGoListener(listener: OnClickListener) {
        menuBtnGo.setOnClickListener(listener)
    }

    private fun toggleDetail() {

        if(textDetail.isVisible) textDetail.gone()
        else textDetail.visible()
    }
}
package com.creations.rimov.esbeta.fragments

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.creations.rimov.esbeta.R
import com.creations.rimov.esbeta.extensions.gone
import com.creations.rimov.esbeta.extensions.visible
import kotlinx.android.synthetic.main.menu_button_expandable.view.*
import kotlinx.android.synthetic.main.menu_main.view.*

class MainMenuFragment : Fragment(), View.OnClickListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.menu_main, container, false)

        (view.menuA as MenuBtnExpand).setGoListener(this)
        (view.menuB as MenuBtnExpand).setGoListener(this)
        (view.menuC as MenuBtnExpand).setGoListener(this)
        (view.menuD as MenuBtnExpand).setGoListener(this)

        return view
    }

    override fun onClick(v: View?) {

        v?.let {
            val parent = it.parent as View

            //btnGo has been triggered, find which item it belongs to
            when(parent.id) {
                R.id.menuA -> {
                    findNavController().navigate(R.id.action_mainMenuFragment_to_videoFragment)
                }
                R.id.menuB -> {
                    findNavController().navigate(R.id.action_mainMenuFragment_to_bookFragment)
                }
                R.id.menuC -> {

                }
                R.id.menuD -> {

                }
            }
        }
    }

    class MenuBtnExpand(context: Context, attrs: AttributeSet?)
        : RelativeLayout(context, attrs) {

        private val btnExpand: Button by lazy {menuBtnExpand}
        private val textDetail: TextView by lazy {menuDetailText}
        private val btnGo: ImageButton by lazy {menuBtnGo}

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
            btnGo.setOnClickListener(listener)
        }

        private fun toggleDetail() {

            if(textDetail.isVisible) {
                textDetail.gone()
                btnGo.gone()

            } else {
                textDetail.visible()
                btnGo.visible()
            }
        }
    }
}
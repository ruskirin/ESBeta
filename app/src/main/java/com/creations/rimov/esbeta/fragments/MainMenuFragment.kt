package com.creations.rimov.esbeta.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.creations.rimov.esbeta.R
import kotlinx.android.synthetic.main.main_menu.view.*

class MainMenuFragment : Fragment(), View.OnClickListener {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.main_menu, container, false)

        view.menuA.setGoListener(this)
        view.menuB.setGoListener(this)
        view.menuC.setGoListener(this)
        view.menuD.setGoListener(this)

        return view
    }

    override fun onClick(v: View?) {

        v?.let {
            when(it.id) {
                R.id.menuA -> {
                    findNavController().navigate(R.id.action_mainMenuFragment_to_videoFragment)
                }
                R.id.menuB -> {

                }
                R.id.menuC -> {

                }
                R.id.menuD -> {

                }
            }
        }
    }
}
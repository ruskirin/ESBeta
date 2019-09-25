package com.creations.rimov.esbeta.extensions

import android.view.View

fun View?.gone() {

    this?.let {
        if(it.visibility == View.VISIBLE || it.visibility == View.INVISIBLE) it.visibility = View.GONE
    }
}

fun View?.visible() {

    this?.let {
        if(it.visibility == View.GONE || it.visibility == View.INVISIBLE) it.visibility = View.VISIBLE
    }
}

fun View?.invisible() {

    this?.let {
        if(it.visibility == View.VISIBLE) it.visibility = View.INVISIBLE
    }
}
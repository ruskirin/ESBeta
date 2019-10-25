package com.creations.rimov.esbeta.extensions

import android.util.Log
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

fun View?.logPress(tag: String, viewName: String? = "Null view") {
    Log.i(tag, "$viewName pressed!")
}
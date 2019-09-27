package com.creations.rimov.esbeta.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Date.stdPattern() = SimpleDateFormat("yyyyMMdd_HHmmss").format(this)
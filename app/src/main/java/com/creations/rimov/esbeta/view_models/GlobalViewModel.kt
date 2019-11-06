package com.creations.rimov.esbeta.view_models

import android.util.Log
import android.view.ViewManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GlobalViewModel : ViewModel() {

    private var totalPageNum: Int = 0
    private var pageNum: MutableLiveData<Int> = MutableLiveData(0)

    private var isRecording: MutableLiveData<Boolean> = MutableLiveData(false)

    fun getPageNum() = pageNum

    fun setPageNum(num: Int) {
        pageNum.postValue(num)
    }

    fun setPrevPage() {

        pageNum.value?.let { num ->
            if(num > 0) pageNum.postValue(pageNum.value?.minus(1))
        }
    }

    fun setNextPage() {

        pageNum.value?.let { num ->
            if(num < totalPageNum - 1) pageNum.postValue(pageNum.value?.plus(1))
        }
    }

    fun getTotalPageNum() = totalPageNum

    fun setTotalPageNum(num: Int) {
        totalPageNum = num
    }
}
package com.hanto.dragndrop.util


import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.hanto.dragndrop.R

@BindingAdapter("isSelected")
fun View.setSelectedBackground(isSelected: Boolean) {
    if (isSelected) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.selected_color))
        Log.d("BindingAdapter", "배경색 변경: 선택됨")
    } else {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        Log.d("BindingAdapter", "배경색 변경: 선택 해제")
    }
}
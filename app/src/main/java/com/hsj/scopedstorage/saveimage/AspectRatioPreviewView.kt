package com.hsj.scopedstorage.saveimage

import android.content.Context
import android.util.AttributeSet
import androidx.camera.view.PreviewView

class AspectRatioPreviewView(context: Context, attrs: AttributeSet?) : PreviewView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthSize < heightSize) {
            val x = widthSize.div(3f)
            heightSize = x.times(4).toInt()
        } else if (widthSize > heightSize) {
            val x = heightSize.div(3f)
            widthSize = x.times(4).toInt()
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY))
    }
}
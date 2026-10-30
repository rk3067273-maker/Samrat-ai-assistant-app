package com.samrat.aiassistant.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.samrat.aiassistant.R

class FloatingSearchPopup(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var popupView: View? = null
    private var isShowing = false

    var onUserQuerySubmitted: ((String) -> Unit)? = null

    fun show(initialTitle: String = "SAMRAT NEURAL SEARCH", initialContent: String = "") {
        if (isShowing) {
            updateContent(initialTitle, initialContent)
            return
        }

        popupView = LayoutInflater.from(context).inflate(R.layout.popup_search_layout, null)
        val tvTitle = popupView?.findViewById<TextView>(R.id.tvPopupTitle)
        val tvResult = popupView?.findViewById<TextView>(R.id.tvSearchResult)
        val ivClose = popupView?.findViewById<ImageView>(R.id.ivClosePopup)
        val etInput = popupView?.findViewById<EditText>(R.id.etPopupChatInput)
        val ivSend = popupView?.findViewById<ImageView>(R.id.ivSendChat)

        tvTitle?.text = initialTitle
        tvResult?.text = initialContent

        ivClose?.setOnClickListener {
            dismiss()
        }

        ivSend?.setOnClickListener {
            val query = etInput?.text?.toString()?.trim()
            if (!query.isNullOrEmpty()) {
                onUserQuerySubmitted?.invoke(query)
                etInput.text?.clear()
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(popupView, params)
        isShowing = true
    }

    fun updateContent(title: String? = null, content: String? = null) {
        popupView?.post {
            title?.let { popupView?.findViewById<TextView>(R.id.tvPopupTitle)?.text = it }
            content?.let { popupView?.findViewById<TextView>(R.id.tvSearchResult)?.text = it }
        }
    }

    fun dismiss() {
        if (isShowing && popupView != null) {
            windowManager.removeView(popupView)
            popupView = null
            isShowing = false
        }
    }
}

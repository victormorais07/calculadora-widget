package com.victor.calcwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Tela simples que explica como adicionar o widget e oferece um atalho para fixa-lo. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = AppWidgetManager.getInstance(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        layout.addView(TextView(this).apply {
            text = getString(R.string.instrucoes)
            textSize = 18f
            gravity = Gravity.CENTER
        })
        if (manager.isRequestPinAppWidgetSupported) {
            layout.addView(Button(this).apply {
                text = getString(R.string.adicionar)
                setOnClickListener {
                    manager.requestPinAppWidget(ComponentName(this@MainActivity, CalcWidgetProvider::class.java), null, null)
                }
            })
        }
        setContentView(layout)
    }
}

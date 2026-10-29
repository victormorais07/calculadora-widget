package com.victor.calcwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.math.BigDecimal

class CalcWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_KEY) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val state = CalcState.load(context, id)
        state.press(key)
        state.save(context, id)
        render(context, AppWidgetManager.getInstance(context), id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { CalcState.clear(context, it) }
    }

    companion object {
        private const val ACTION_KEY = "com.victor.calcwidget.KEY"
        private const val EXTRA_KEY = "key"

        private val KEYS = listOf(
            R.id.b0 to "0", R.id.b1 to "1", R.id.b2 to "2", R.id.b3 to "3", R.id.b4 to "4",
            R.id.b5 to "5", R.id.b6 to "6", R.id.b7 to "7", R.id.b8 to "8", R.id.b9 to "9",
            R.id.bDot to ".", R.id.bAdd to "+", R.id.bSub to "-", R.id.bMul to "*", R.id.bDiv to "/",
            R.id.bPct to "%", R.id.bClr to "C", R.id.bDel to "DEL", R.id.bEq to "=",
        )

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val state = CalcState.load(context, id)
            val views = RemoteViews(context.packageName, R.layout.widget_calc)
            views.setTextViewText(R.id.tvMain, state.mainText())
            views.setTextViewText(R.id.tvExpr, state.subText())

            KEYS.forEachIndexed { index, (viewId, key) ->
                val intent = Intent(context, CalcWidgetProvider::class.java).apply {
                    action = ACTION_KEY
                    // Uri unica por widget/botao para o Android nao reaproveitar o mesmo PendingIntent
                    data = Uri.parse("calc://$id/$index")
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(EXTRA_KEY, key)
                }
                val pending = PendingIntent.getBroadcast(
                    context, id * 100 + index, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(viewId, pending)
            }
            manager.updateAppWidget(id, views)
        }
    }
}

/**
 * Estado de cada widget, salvo em SharedPreferences.
 * expr: expressao em formato interno ("12.5*3"); history: conta anterior ja exibida;
 * done: true logo apos "=" (expr vazia nesse caso significa erro).
 */
private class CalcState(var expr: String, var history: String, var done: Boolean) {

    fun press(key: String) {
        when (key) {
            "C" -> reset()
            "DEL" -> {
                if (done) reset() else expr = expr.dropLast(1)
            }
            "=" -> {
                if (expr.isEmpty() || done) return
                val v = Calculator.evaluate(expr)
                history = pretty(expr.trimEnd(*OPS))
                expr = v?.let { Calculator.raw(it) } ?: ""
                done = true
            }
            "." -> {
                if (done) reset()
                val current = currentNumber()
                if ('.' in current || expr.length >= MAX_LEN) return
                if (current.isEmpty()) expr += "0"
                expr += "."
            }
            "%" -> {
                if (done && expr.isEmpty()) return
                done = false
                history = ""
                val last = expr.lastOrNull() ?: return
                if (last.isDigit() || last == '.') expr += "%"
            }
            "+", "-", "*", "/" -> {
                if (done && expr.isEmpty()) reset()
                done = false
                history = ""
                val last = expr.lastOrNull()
                when {
                    last == null -> if (key == "-") expr = "-"
                    // Permite numero negativo depois de x ou ÷ (ex.: 5×−3)
                    key == "-" && (last == '*' || last == '/') -> expr += key
                    last in OPS -> {
                        val base = expr.trimEnd(*OPS)
                        expr = if (base.isEmpty()) (if (key == "-") "-" else "") else base + key
                    }
                    expr.length < MAX_LEN -> expr += key
                }
            }
            else -> { // digitos
                if (done) reset()
                if (expr.length >= MAX_LEN) return
                if (currentNumber() == "0") expr = expr.dropLast(1)
                expr += key
            }
        }
    }

    fun mainText(): String = when {
        done && expr.isEmpty() -> "Erro"
        done -> Calculator.format(BigDecimal(expr))
        expr.isEmpty() -> "0"
        else -> pretty(expr)
    }

    fun subText(): String {
        if (done) return "$history ="
        // So mostra a previa quando ha alguma operacao de fato
        if (expr.drop(1).none { it in OPS || it == '%' }) return ""
        return Calculator.evaluate(expr)?.let { "= " + Calculator.format(it) } ?: ""
    }

    private fun reset() {
        expr = ""
        history = ""
        done = false
    }

    private fun currentNumber() = expr.takeLastWhile { it.isDigit() || it == '.' }

    fun save(context: Context, id: Int) {
        prefs(context).edit()
            .putString("expr_$id", expr)
            .putString("hist_$id", history)
            .putBoolean("done_$id", done)
            .apply()
    }

    companion object {
        private val OPS = charArrayOf('+', '-', '*', '/')
        private const val MAX_LEN = 40

        private fun prefs(context: Context) = context.getSharedPreferences("calc", Context.MODE_PRIVATE)

        fun load(context: Context, id: Int): CalcState {
            val p = prefs(context)
            return CalcState(
                p.getString("expr_$id", "") ?: "",
                p.getString("hist_$id", "") ?: "",
                p.getBoolean("done_$id", false),
            )
        }

        fun clear(context: Context, id: Int) {
            prefs(context).edit().remove("expr_$id").remove("hist_$id").remove("done_$id").apply()
        }

        private fun pretty(e: String) = buildString {
            for (c in e) {
                append(
                    when (c) {
                        '*' -> '×'
                        '/' -> '÷'
                        '-' -> '−'
                        '.' -> ','
                        else -> c
                    }
                )
            }
        }
    }
}

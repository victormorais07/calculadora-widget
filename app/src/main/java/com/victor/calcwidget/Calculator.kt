package com.victor.calcwidget

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/** Avalia expressoes como "12.5*3-4%" usando BigDecimal (evita 0,1 + 0,2 = 0,30000000000000004). */
object Calculator {
    private val MC = MathContext.DECIMAL64
    private val LIMITE_GRANDE = BigDecimal("1e12")
    private val LIMITE_PEQUENO = BigDecimal("1e-9")

    /** Ignora operadores pendurados no fim. Retorna null se a expressao for invalida ou dividir por zero. */
    fun evaluate(expr: String): BigDecimal? {
        val e = expr.trimEnd('+', '-', '*', '/')
        if (e.isEmpty()) return null
        return try {
            Parser(e).parse()
        } catch (_: ArithmeticException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Valor bruto para continuar calculando a partir do resultado. */
    fun raw(v: BigDecimal): String =
        v.round(MathContext(15, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()

    /** Valor para exibir, com virgula decimal. */
    fun format(v: BigDecimal): String {
        val r = v.round(MathContext(12, RoundingMode.HALF_UP)).stripTrailingZeros()
        val a = r.abs()
        val s = if (a >= LIMITE_GRANDE || (r.signum() != 0 && a < LIMITE_PEQUENO)) {
            String.format(Locale.US, "%.6e", r.toDouble())
        } else {
            r.toPlainString()
        }
        return s.replace('.', ',')
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun parse(): BigDecimal {
            val v = expr()
            require(i == s.length)
            return v
        }

        private fun expr(): BigDecimal {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val r = term()
                v = if (op == '+') v.add(r, MC) else v.subtract(r, MC)
            }
            return v
        }

        private fun term(): BigDecimal {
            var v = factor()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val r = factor()
                v = if (op == '*') v.multiply(r, MC) else v.divide(r, MC)
            }
            return v
        }

        private fun factor(): BigDecimal {
            if (i < s.length && s[i] == '-') {
                i++
                return factor().negate()
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start)
            var v = BigDecimal(s.substring(start, i))
            while (i < s.length && s[i] == '%') {
                i++
                v = v.divide(BigDecimal(100), MC)
            }
            return v
        }
    }
}

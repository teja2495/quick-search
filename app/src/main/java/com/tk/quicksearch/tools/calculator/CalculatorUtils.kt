package com.tk.quicksearch.tools.calculator

import java.util.Locale

/**
 * Utility functions for calculator operations.
 * Safely evaluates basic math expressions (addition, subtraction, multiplication, division, and brackets).
 */
object CalculatorUtils {
    private val percentPhraseRegex =
        Regex(
            pattern = """^\s*([+-]?\d+(?:\.\d+)?)\s*%\s*(off|of)\s*([+-]?\d+(?:\.\d+)?)\s*$""",
            option = RegexOption.IGNORE_CASE,
        )

    // 'x' counts as multiplication only between operands (e.g. "3x4", "(2) X 5"), so words stay text.
    private val letterMultiplyRegex = Regex("""(?<=[\d.)])\s*[xX]\s*(?=[\d.(])""")

    /**
     * Checks if a string looks like a math expression.
     * A math expression should contain at least one operator (+, -, *, /) or brackets.
     */
    fun isMathExpression(query: String): Boolean {
        val trimmed = normalizeLetterMultiply(query.trim())
        if (trimmed.isEmpty()) return false
        if (isPercentPhrase(trimmed)) return true

        // Check if it contains math operators or brackets
        val hasOperator =
            trimmed.contains('+') ||
                trimmed.contains('-') ||
                trimmed.contains('*') ||
                trimmed.contains('/') ||
                trimmed.contains('×') ||
                trimmed.contains('÷') ||
                trimmed.contains('·') ||
                trimmed.contains('(') ||
                trimmed.contains(')') ||
                trimmed.contains('%')

        if (!hasOperator) return false

        // A lone percent ("50%") is not a calculation.
        if (trimmed.none { it in "+-*/×÷·()" }) return false

        // Check if it contains mostly valid math characters
        // Allow: digits, spaces, operators, brackets, decimal points
        val validChars =
            trimmed.all { char ->
                char.isDigit() ||
                    char == '+' ||
                    char == '-' ||
                    char == '*' ||
                    char == '/' ||
                    char == '(' ||
                    char == ')' ||
                    char == '.' ||
                    char == ' ' ||
                    char == '×' || // multiplication symbol
                    char == '÷' || // division symbol
                    char == '·' || // middle dot for multiplication
                    char == '%' // percent
            }

        return validChars && trimmed.length >= 2
    }

    /**
     * Safely evaluates a math expression.
     * Returns the result as a string, or null if evaluation fails.
     */
    fun evaluateExpression(expression: String): String? =
        try {
            evaluatePercentPhrase(expression)?.let { percentResult ->
                return formatResult(percentResult)
            }
            val cleaned = cleanExpression(expression)
            val result = evaluate(cleaned)
            formatResult(result)
        } catch (e: Exception) {
            null
        }

    private fun normalizeLetterMultiply(expression: String): String = letterMultiplyRegex.replace(expression, "*")

    private fun isPercentPhrase(query: String): Boolean = percentPhraseRegex.matches(query)

    private fun evaluatePercentPhrase(expression: String): Double? {
        val match = percentPhraseRegex.matchEntire(expression) ?: return null
        val percent = match.groupValues[1].toDoubleOrNull() ?: return null
        val operation = match.groupValues[2].lowercase(Locale.US)
        val baseValue = match.groupValues[3].toDoubleOrNull() ?: return null

        val percentValue = baseValue * (percent / 100.0)
        return when (operation) {
            "off" -> baseValue - percentValue
            "of" -> percentValue
            else -> null
        }
    }

    /**
     * Cleans the expression by normalizing operators and removing extra spaces.
     */
    private fun cleanExpression(expression: String): String =
        normalizeLetterMultiply(expression)
            .replace("×", "*")
            .replace("÷", "/")
            .replace("·", "*")
            .replace(" ", "")
            .trim()

    /**
     * Evaluates a cleaned math expression using recursive descent parsing.
     * Handles: +, -, *, /, %, (), and decimal numbers.
     */
    private fun evaluate(expression: String): Double {
        var index = 0

        fun parseNumber(): Double {
            var numStr = ""
            while (index < expression.length &&
                (expression[index].isDigit() || expression[index] == '.')
            ) {
                numStr += expression[index]
                index++
            }
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number")
        }

        // Forward declaration for recursive parsing
        lateinit var parseExpression: () -> Double

        fun parseFactor(): Double {
            if (index >= expression.length) {
                throw IllegalArgumentException("Unexpected end of expression")
            }

            return when (expression[index]) {
                '(' -> {
                    index++ // consume '('
                    val result = parseExpression()
                    if (index >= expression.length || expression[index] != ')') {
                        throw IllegalArgumentException("Missing closing parenthesis")
                    }
                    index++ // consume ')'
                    result
                }

                '-' -> {
                    index++ // consume '-'
                    -parseFactor()
                }

                '+' -> {
                    index++ // consume '+'
                    parseFactor()
                }

                else -> {
                    parseNumber()
                }
            }
        }

        // True when the last parsed term was a bare percent literal (e.g. the "10%" in "100 - 10%").
        var lastTermWasPercent = false

        fun parsePercentFactor(): Double {
            var value = parseFactor()
            var hasPercent = false
            while (index < expression.length && expression[index] == '%') {
                index++
                value /= 100.0
                hasPercent = true
            }
            lastTermWasPercent = hasPercent
            return value
        }

        fun parseTerm(): Double {
            var result = parsePercentFactor()
            while (index < expression.length) {
                when (expression[index]) {
                    '*' -> {
                        index++
                        result *= parsePercentFactor()
                        lastTermWasPercent = false
                    }

                    '/' -> {
                        index++
                        val divisor = parsePercentFactor()
                        lastTermWasPercent = false
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }

                    else -> {
                        break
                    }
                }
            }
            return result
        }

        parseExpression = {
            var result = parseTerm()
            lastTermWasPercent = false
            while (index < expression.length) {
                when (expression[index]) {
                    '+' -> {
                        index++
                        val term = parseTerm()
                        result += if (lastTermWasPercent) result * term else term
                    }

                    '-' -> {
                        index++
                        val term = parseTerm()
                        result -= if (lastTermWasPercent) result * term else term
                    }

                    else -> {
                        break
                    }
                }
            }
            result
        }

        val result = parseExpression()
        if (index < expression.length) {
            throw IllegalArgumentException("Unexpected character at position $index")
        }
        return result
    }

    /**
     * Formats the result with max 2 decimal places, trimming trailing zeros.
     */
    private fun formatResult(result: Double): String {
        val rounded = String.format(Locale.US, "%.2f", result)
        return rounded.trimEnd('0').trimEnd('.')
    }
}

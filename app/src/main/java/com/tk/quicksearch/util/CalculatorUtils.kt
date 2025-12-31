package com.tk.quicksearch.util

/**
 * Utility functions for calculator operations.
 * Safely evaluates basic math expressions (addition, subtraction, multiplication, division, and brackets).
 */
object CalculatorUtils {
    
    /**
     * Checks if a string looks like a math expression.
     * A math expression should contain at least one operator (+, -, *, /) or brackets.
     */
    fun isMathExpression(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return false
        
        // Check if it contains math operators or brackets
        val hasOperator = trimmed.contains('+') || 
                         trimmed.contains('-') || 
                         trimmed.contains('*') || 
                         trimmed.contains('/') ||
                         trimmed.contains('(') ||
                         trimmed.contains(')')
        
        if (!hasOperator) return false
        
        // Check if it contains mostly valid math characters
        // Allow: digits, spaces, operators, brackets, decimal points
        val validChars = trimmed.all { char ->
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
            char == '·'    // middle dot for multiplication
        }
        
        return validChars && trimmed.length >= 2
    }
    
    /**
     * Safely evaluates a math expression.
     * Returns the result as a string, or null if evaluation fails.
     */
    fun evaluateExpression(expression: String): String? {
        return try {
            val cleaned = cleanExpression(expression)
            val result = evaluate(cleaned)
            formatResult(result)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cleans the expression by normalizing operators and removing extra spaces.
     */
    private fun cleanExpression(expression: String): String {
        return expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("·", "*")
            .replace(" ", "")
            .trim()
    }
    
    /**
     * Evaluates a cleaned math expression using recursive descent parsing.
     * Handles: +, -, *, /, (), and decimal numbers.
     */
    private fun evaluate(expression: String): Double {
        var index = 0

        fun parseNumber(): Double {
            var numStr = ""
            while (index < expression.length &&
                   (expression[index].isDigit() || expression[index] == '.')) {
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
                else -> parseNumber()
            }
        }

        fun parseTerm(): Double {
            var result = parseFactor()
            while (index < expression.length) {
                when (expression[index]) {
                    '*' -> {
                        index++
                        result *= parseFactor()
                    }
                    '/' -> {
                        index++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        result /= divisor
                    }
                    else -> break
                }
            }
            return result
        }

        parseExpression = {
            var result = parseTerm()
            while (index < expression.length) {
                when (expression[index]) {
                    '+' -> {
                        index++
                        result += parseTerm()
                    }
                    '-' -> {
                        index++
                        result -= parseTerm()
                    }
                    else -> break
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
     * Formats the result to remove unnecessary decimal places.
     */
    private fun formatResult(result: Double): String {
        // If it's a whole number, return without decimal point
        if (result % 1.0 == 0.0) {
            return result.toLong().toString()
        }
        // Otherwise, format to remove trailing zeros
        return result.toString().trimEnd('0').trimEnd('.')
    }
}


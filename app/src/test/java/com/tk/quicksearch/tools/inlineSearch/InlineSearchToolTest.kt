package com.tk.quicksearch.tools.inlineSearch

import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.toolsSettings.ToolSettingsRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineSearchToolTest {
    @Test
    fun aliasesAndSettingsAreDerivedFromToolDefinitions() {
        val aliases = AliasHandler.FEATURE_ALIAS_DEFINITIONS.filter { it.toolType != null }
            .associate { it.featureId to it.toolType }
        assertEquals(SearchToolType.CALCULATOR, aliases["calculator_mode"])
        assertEquals(SearchToolType.UNIT_CONVERTER, aliases["unit_converter_mode"])
        assertEquals(SearchToolType.DATE_CALCULATOR, aliases["date_calculator_mode"])
        assertEquals(
            SearchToolType.entries.mapNotNull { tool -> tool.aliasFeatureId?.let { it to tool } }.toMap(),
            aliases,
        )
        for (tool in SearchToolType.entries) {
            val row = ToolSettingsRegistry.definitionFor(tool.setting.preferenceKey)
            assertEquals(tool.setting, row?.booleanSetting)
            assertEquals(tool.aliasFeatureId, row?.aliasFeatureId)
            assertEquals(tool.infoDestination, row?.infoDestination)
            assertEquals(tool.searchDestination, row?.searchDestination)
        }
    }

    @Test
    fun disablingAutomaticDetectionStillAllowsExistingExplicitAliases() {
        val calculator = SearchToolType.CALCULATOR
        val automatic = detectInlineSearchTool("2 + 3") { true }
        assertEquals(calculator, automatic?.toolType)
        assertEquals("5", automatic?.result)
        assertFalse(checkNotNull(automatic).isToolMode)
        assertNull(detectInlineSearchTool("2 + 3") { it != calculator.setting })

        val explicit = calculator.evaluateInMode("2 + 3")
        assertEquals("5", explicit.result)
        assertTrue(explicit.isCalculatorMode)
        assertTrue(calculator.evaluateInMode("invalid").showInvalidExpression)
        assertFalse(calculator.evaluateInMode("").showInvalidExpression)
    }

    @Test
    fun recognizedColorStillReplacesOtherResults() {
        val result = checkNotNull(detectInlineSearchTool("#abcdef") { true })
        assertEquals(SearchToolType.COLOR_VISUALIZER, result.toolType)
        assertEquals("#ABCDEF", result.result)
        assertTrue(result.hasExclusiveResult)
    }

    @Test
    fun dateResultsCanCoexistWithSearchAndTrailingExpressionsAreAutomaticOnly() {
        val result = checkNotNull(detectInlineSearchTool("Laundry in 1 hr") { true })
        assertEquals(SearchToolType.DATE_CALCULATOR, result.toolType)
        assertNotNull(result.timeResultLabel)
        assertTrue(result.isVisible)
        assertFalse(result.hasExclusiveResult)
        assertTrue(SearchToolType.DATE_CALCULATOR.evaluateInMode("Laundry in 1 hr").showInvalidExpression)
    }
}

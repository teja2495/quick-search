package com.tk.quicksearch.tools.unitConverter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs

object UnitConverterUtils {
    private enum class UnitCategory {
        LENGTH,
        MASS,
        TEMPERATURE,
        AREA,
        VOLUME,
        TIME,
        SPEED,
        DATA,
        ENERGY,
        POWER,
        PRESSURE,
        ANGLE,
        FREQUENCY,
    }

    private sealed interface UnitDefinition {
        val category: UnitCategory
        val displaySymbol: String

        fun toBase(value: Double): Double

        fun fromBase(value: Double): Double
    }

    private data class LinearUnitDefinition(
        override val category: UnitCategory,
        override val displaySymbol: String,
        val factorToBase: Double,
    ) : UnitDefinition {
        override fun toBase(value: Double): Double = value * factorToBase

        override fun fromBase(value: Double): Double = value / factorToBase
    }

    private data class TemperatureUnitDefinition(
        override val displaySymbol: String,
        val toCelsius: (Double) -> Double,
        val fromCelsius: (Double) -> Double,
    ) : UnitDefinition {
        override val category: UnitCategory = UnitCategory.TEMPERATURE

        override fun toBase(value: Double): Double = toCelsius(value)

        override fun fromBase(value: Double): Double = fromCelsius(value)
    }

    private data class UnitEntry(
        val aliases: Set<String>,
        val unit: UnitDefinition,
    )

    private data class ParsedQuery(
        val amount: Double,
        val fromUnitRaw: String,
        val toUnitRaw: String,
    )

    private val separatorRegex = Regex("\\b(?:in|to)\\b", RegexOption.IGNORE_CASE)
    private val leftQueryRegex =
        Regex(
            pattern = "^([+-]?(?:\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.\\d+)?|[+-]?\\.\\d+)\\s*([\\p{L}0-9\\s./^°-]+)$",
            options = setOf(RegexOption.IGNORE_CASE),
        )

    private val normalDecimalFormat by lazy {
        DecimalFormat("0.############", DecimalFormatSymbols(Locale.US)).apply {
            isGroupingUsed = false
        }
    }
    private val scientificDecimalFormat by lazy {
        DecimalFormat("0.######E0", DecimalFormatSymbols(Locale.US)).apply {
            isGroupingUsed = false
        }
    }

    private val unitsByAlias: Map<String, List<UnitDefinition>> by lazy {
        val entries = mutableListOf<UnitEntry>()

        fun registerLinear(
            category: UnitCategory,
            symbol: String,
            factorToBase: Double,
            aliases: Set<String>,
        ) {
            entries +=
                UnitEntry(
                    aliases = aliases.map(::normalizeUnit).toSet(),
                    unit =
                        LinearUnitDefinition(
                            category = category,
                            displaySymbol = symbol,
                            factorToBase = factorToBase,
                        ),
                )
        }

        fun registerTemperature(
            symbol: String,
            aliases: Set<String>,
            toCelsius: (Double) -> Double,
            fromCelsius: (Double) -> Double,
        ) {
            entries +=
                UnitEntry(
                    aliases = aliases.map(::normalizeUnit).toSet(),
                    unit =
                        TemperatureUnitDefinition(
                            displaySymbol = symbol,
                            toCelsius = toCelsius,
                            fromCelsius = fromCelsius,
                        ),
                )
        }

        // Length (base: meter)
        registerLinear(UnitCategory.LENGTH, "m", 1.0, setOf("m", "meter", "meters", "metre", "metres"))
        registerLinear(UnitCategory.LENGTH, "km", 1000.0, setOf("km", "kms", "kilometer", "kilometers", "kilometre", "kilometres"))
        registerLinear(UnitCategory.LENGTH, "cm", 0.01, setOf("cm", "cms", "centimeter", "centimeters", "centimetre", "centimetres"))
        registerLinear(UnitCategory.LENGTH, "mm", 0.001, setOf("mm", "millimeter", "millimeters", "millimetre", "millimetres"))
        registerLinear(UnitCategory.LENGTH, "um", 1e-6, setOf("um", "micrometer", "micrometers", "micrometre", "micrometres", "micron", "microns"))
        registerLinear(UnitCategory.LENGTH, "nm", 1e-9, setOf("nm", "nanometer", "nanometers", "nanometre", "nanometres"))
        registerLinear(UnitCategory.LENGTH, "mi", 1609.344, setOf("mi", "mile", "miles"))
        registerLinear(UnitCategory.LENGTH, "yd", 0.9144, setOf("yd", "yds", "yard", "yards"))
        registerLinear(UnitCategory.LENGTH, "ft", 0.3048, setOf("ft", "foot", "feet"))
        registerLinear(UnitCategory.LENGTH, "in", 0.0254, setOf("in", "inch", "inches"))
        registerLinear(UnitCategory.LENGTH, "nmi", 1852.0, setOf("nmi", "nautical mile", "nautical miles"))

        // Mass (base: kilogram)
        registerLinear(UnitCategory.MASS, "kg", 1.0, setOf("kg", "kgs", "kilogram", "kilograms"))
        registerLinear(UnitCategory.MASS, "g", 0.001, setOf("g", "gram", "grams"))
        registerLinear(UnitCategory.MASS, "mg", 1e-6, setOf("mg", "milligram", "milligrams"))
        registerLinear(UnitCategory.MASS, "ug", 1e-9, setOf("ug", "microgram", "micrograms"))
        registerLinear(UnitCategory.MASS, "lb", 0.45359237, setOf("lb", "lbs", "pound", "pounds"))
        registerLinear(UnitCategory.MASS, "oz", 0.028349523125, setOf("oz", "ounce", "ounces"))
        registerLinear(UnitCategory.MASS, "st", 6.35029318, setOf("st", "stone", "stones"))
        registerLinear(UnitCategory.MASS, "tonne", 1000.0, setOf("tonne", "tonnes", "metric ton", "metric tons", "ton", "tons"))
        registerLinear(UnitCategory.MASS, "us ton", 907.18474, setOf("us ton", "us tons", "short ton", "short tons"))

        // Area (base: square meter)
        registerLinear(UnitCategory.AREA, "m2", 1.0, setOf("m2", "sqm", "sq m", "square meter", "square meters", "square metre", "square metres"))
        registerLinear(UnitCategory.AREA, "km2", 1_000_000.0, setOf("km2", "sqkm", "sq km", "square kilometer", "square kilometers", "square kilometre", "square kilometres"))
        registerLinear(UnitCategory.AREA, "cm2", 0.0001, setOf("cm2", "sqcm", "sq cm", "square centimeter", "square centimeters", "square centimetre", "square centimetres"))
        registerLinear(UnitCategory.AREA, "mm2", 1e-6, setOf("mm2", "sqmm", "sq mm", "square millimeter", "square millimeters", "square millimetre", "square millimetres"))
        registerLinear(UnitCategory.AREA, "ft2", 0.09290304, setOf("ft2", "sqft", "sq ft", "square foot", "square feet"))
        registerLinear(UnitCategory.AREA, "in2", 0.00064516, setOf("in2", "sqin", "sq in", "square inch", "square inches"))
        registerLinear(UnitCategory.AREA, "yd2", 0.83612736, setOf("yd2", "sqyd", "sq yd", "square yard", "square yards"))
        registerLinear(UnitCategory.AREA, "mi2", 2_589_988.110336, setOf("mi2", "sqmi", "sq mi", "square mile", "square miles"))
        registerLinear(UnitCategory.AREA, "acre", 4046.8564224, setOf("acre", "acres"))
        registerLinear(UnitCategory.AREA, "ha", 10_000.0, setOf("ha", "hectare", "hectares"))

        // Volume (base: liter)
        registerLinear(UnitCategory.VOLUME, "l", 1.0, setOf("l", "liter", "liters", "litre", "litres"))
        registerLinear(UnitCategory.VOLUME, "ml", 0.001, setOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"))
        registerLinear(UnitCategory.VOLUME, "m3", 1000.0, setOf("m3", "cu m", "cubic meter", "cubic meters", "cubic metre", "cubic metres"))
        registerLinear(UnitCategory.VOLUME, "cm3", 0.001, setOf("cm3", "cc", "cu cm", "cubic centimeter", "cubic centimeters", "cubic centimetre", "cubic centimetres"))
        registerLinear(UnitCategory.VOLUME, "mm3", 1e-6, setOf("mm3", "cu mm", "cubic millimeter", "cubic millimeters", "cubic millimetre", "cubic millimetres"))
        registerLinear(UnitCategory.VOLUME, "ft3", 28.316846592, setOf("ft3", "cu ft", "cubic foot", "cubic feet"))
        registerLinear(UnitCategory.VOLUME, "in3", 0.016387064, setOf("in3", "cu in", "cubic inch", "cubic inches"))
        registerLinear(UnitCategory.VOLUME, "gal", 3.785411784, setOf("gal", "gallon", "gallons", "us gallon", "us gallons"))
        registerLinear(UnitCategory.VOLUME, "qt", 0.946352946, setOf("qt", "quart", "quarts"))
        registerLinear(UnitCategory.VOLUME, "pt", 0.473176473, setOf("pt", "pint", "pints"))
        registerLinear(UnitCategory.VOLUME, "cup", 0.2365882365, setOf("cup", "cups"))
        registerLinear(UnitCategory.VOLUME, "fl oz", 0.0295735295625, setOf("floz", "fl oz", "fluid ounce", "fluid ounces"))
        registerLinear(UnitCategory.VOLUME, "tbsp", 0.01478676478125, setOf("tbsp", "tablespoon", "tablespoons"))
        registerLinear(UnitCategory.VOLUME, "tsp", 0.00492892159375, setOf("tsp", "teaspoon", "teaspoons"))

        // Time (base: second)
        registerLinear(UnitCategory.TIME, "s", 1.0, setOf("s", "sec", "secs", "second", "seconds"))
        registerLinear(UnitCategory.TIME, "ms", 0.001, setOf("ms", "millisecond", "milliseconds"))
        registerLinear(UnitCategory.TIME, "us", 1e-6, setOf("us", "microsecond", "microseconds"))
        registerLinear(UnitCategory.TIME, "ns", 1e-9, setOf("ns", "nanosecond", "nanoseconds"))
        registerLinear(UnitCategory.TIME, "min", 60.0, setOf("min", "mins", "minute", "minutes"))
        registerLinear(UnitCategory.TIME, "h", 3600.0, setOf("h", "hr", "hrs", "hour", "hours"))
        registerLinear(UnitCategory.TIME, "day", 86_400.0, setOf("d", "day", "days"))
        registerLinear(UnitCategory.TIME, "week", 604_800.0, setOf("w", "week", "weeks"))
        registerLinear(UnitCategory.TIME, "month", 2_629_746.0, setOf("month", "months"))
        registerLinear(UnitCategory.TIME, "year", 31_557_600.0, setOf("yr", "yrs", "year", "years"))

        // Speed (base: meter per second)
        registerLinear(UnitCategory.SPEED, "m/s", 1.0, setOf("m/s", "meter per second", "meters per second", "metre per second", "metres per second", "mps"))
        registerLinear(UnitCategory.SPEED, "km/h", 0.2777777777777778, setOf("km/h", "kph", "kmh", "kilometer per hour", "kilometers per hour", "kilometre per hour", "kilometres per hour"))
        registerLinear(UnitCategory.SPEED, "mph", 0.44704, setOf("mph", "mile per hour", "miles per hour"))
        registerLinear(UnitCategory.SPEED, "kt", 0.5144444444444445, setOf("kt", "kts", "knot", "knots"))
        registerLinear(UnitCategory.SPEED, "ft/s", 0.3048, setOf("ft/s", "fps", "foot per second", "feet per second"))
        registerLinear(UnitCategory.SPEED, "in/s", 0.0254, setOf("in/s", "inch per second", "inches per second"))

        // Data (base: byte)
        registerLinear(UnitCategory.DATA, "byte", 1.0, setOf("byte", "bytes", "b"))
        registerLinear(UnitCategory.DATA, "bit", 0.125, setOf("bit", "bits"))
        registerLinear(UnitCategory.DATA, "KB", 1_000.0, setOf("kb", "kilobyte", "kilobytes"))
        registerLinear(UnitCategory.DATA, "MB", 1_000_000.0, setOf("mb", "megabyte", "megabytes"))
        registerLinear(UnitCategory.DATA, "GB", 1_000_000_000.0, setOf("gb", "gigabyte", "gigabytes"))
        registerLinear(UnitCategory.DATA, "TB", 1_000_000_000_000.0, setOf("tb", "terabyte", "terabytes"))
        registerLinear(UnitCategory.DATA, "KiB", 1024.0, setOf("kib", "kibibyte", "kibibytes"))
        registerLinear(UnitCategory.DATA, "MiB", 1_048_576.0, setOf("mib", "mebibyte", "mebibytes"))
        registerLinear(UnitCategory.DATA, "GiB", 1_073_741_824.0, setOf("gib", "gibibyte", "gibibytes"))
        registerLinear(UnitCategory.DATA, "TiB", 1_099_511_627_776.0, setOf("tib", "tebibyte", "tebibytes"))

        // Energy (base: joule)
        registerLinear(UnitCategory.ENERGY, "J", 1.0, setOf("j", "joule", "joules"))
        registerLinear(UnitCategory.ENERGY, "kJ", 1000.0, setOf("kj", "kilojoule", "kilojoules"))
        registerLinear(UnitCategory.ENERGY, "cal", 4.184, setOf("cal", "calorie", "calories"))
        registerLinear(UnitCategory.ENERGY, "kcal", 4184.0, setOf("kcal", "kilocalorie", "kilocalories"))
        registerLinear(UnitCategory.ENERGY, "Wh", 3600.0, setOf("wh", "watt hour", "watt hours"))
        registerLinear(UnitCategory.ENERGY, "kWh", 3_600_000.0, setOf("kwh", "kilowatt hour", "kilowatt hours"))
        registerLinear(UnitCategory.ENERGY, "eV", 1.602176634e-19, setOf("ev", "electronvolt", "electronvolts"))
        registerLinear(UnitCategory.ENERGY, "BTU", 1055.05585262, setOf("btu", "btus"))

        // Power (base: watt)
        registerLinear(UnitCategory.POWER, "W", 1.0, setOf("w", "watt", "watts"))
        registerLinear(UnitCategory.POWER, "kW", 1000.0, setOf("kw", "kilowatt", "kilowatts"))
        registerLinear(UnitCategory.POWER, "mW", 0.001, setOf("mw", "milliwatt", "milliwatts"))
        registerLinear(UnitCategory.POWER, "hp", 745.6998715822702, setOf("hp", "horsepower"))

        // Pressure (base: pascal)
        registerLinear(UnitCategory.PRESSURE, "Pa", 1.0, setOf("pa", "pascal", "pascals"))
        registerLinear(UnitCategory.PRESSURE, "kPa", 1000.0, setOf("kpa", "kilopascal", "kilopascals"))
        registerLinear(UnitCategory.PRESSURE, "MPa", 1_000_000.0, setOf("mpa", "megapascal", "megapascals"))
        registerLinear(UnitCategory.PRESSURE, "bar", 100_000.0, setOf("bar", "bars"))
        registerLinear(UnitCategory.PRESSURE, "mbar", 100.0, setOf("mbar", "millibar", "millibars"))
        registerLinear(UnitCategory.PRESSURE, "psi", 6894.757293168, setOf("psi"))
        registerLinear(UnitCategory.PRESSURE, "atm", 101_325.0, setOf("atm", "atmosphere", "atmospheres"))
        registerLinear(UnitCategory.PRESSURE, "torr", 133.3223684211, setOf("torr"))
        registerLinear(UnitCategory.PRESSURE, "mmHg", 133.322387415, setOf("mmhg"))

        // Angle (base: radian)
        registerLinear(UnitCategory.ANGLE, "rad", 1.0, setOf("rad", "radian", "radians"))
        registerLinear(UnitCategory.ANGLE, "deg", PI / 180.0, setOf("deg", "degree", "degrees"))
        registerLinear(UnitCategory.ANGLE, "grad", PI / 200.0, setOf("grad", "gradian", "gradians"))
        registerLinear(UnitCategory.ANGLE, "rev", 2.0 * PI, setOf("rev", "revolution", "revolutions", "turn", "turns"))

        // Frequency (base: hertz)
        registerLinear(UnitCategory.FREQUENCY, "Hz", 1.0, setOf("hz", "hertz"))
        registerLinear(UnitCategory.FREQUENCY, "kHz", 1000.0, setOf("khz", "kilohertz"))
        registerLinear(UnitCategory.FREQUENCY, "MHz", 1_000_000.0, setOf("mhz", "megahertz"))
        registerLinear(UnitCategory.FREQUENCY, "GHz", 1_000_000_000.0, setOf("ghz", "gigahertz"))
        registerLinear(UnitCategory.FREQUENCY, "rpm", 1.0 / 60.0, setOf("rpm", "revolutions per minute"))

        // Temperature (base: celsius)
        registerTemperature(
            symbol = "C",
            aliases = setOf("c", "deg c", "celsius"),
            toCelsius = { value -> value },
            fromCelsius = { value -> value },
        )
        registerTemperature(
            symbol = "F",
            aliases = setOf("f", "deg f", "fahrenheit"),
            toCelsius = { value -> (value - 32.0) * (5.0 / 9.0) },
            fromCelsius = { value -> (value * (9.0 / 5.0)) + 32.0 },
        )
        registerTemperature(
            symbol = "K",
            aliases = setOf("k", "kelvin"),
            toCelsius = { value -> value - 273.15 },
            fromCelsius = { value -> value + 273.15 },
        )

        val map = linkedMapOf<String, MutableList<UnitDefinition>>()
        entries.forEach { entry ->
            entry.aliases.forEach { alias ->
                if (alias.isBlank()) return@forEach
                map.getOrPut(alias) { mutableListOf() }.add(entry.unit)
            }
        }
        map.mapValues { (_, units) -> units.toList() }
    }

    fun convertQuery(query: String): String? {
        val parsed = parseQuery(query) ?: return null

        val fromAlias = normalizeUnit(parsed.fromUnitRaw)
        val toAlias = normalizeUnit(parsed.toUnitRaw)

        val fromCandidates = unitsByAlias[fromAlias].orEmpty()
        val toCandidates = unitsByAlias[toAlias].orEmpty()
        if (fromCandidates.isEmpty() || toCandidates.isEmpty()) return null

        val matchedPair = matchCompatibleUnits(fromCandidates, toCandidates) ?: return null
        val (fromUnit, toUnit) = matchedPair

        val baseValue = fromUnit.toBase(parsed.amount)
        val converted = toUnit.fromBase(baseValue)
        if (!converted.isFinite()) return null

        val formattedValue = formatValue(converted)
        return "$formattedValue ${toUnit.displaySymbol}"
    }

    private fun parseQuery(query: String): ParsedQuery? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null

        val separatorMatch = separatorRegex.find(trimmed) ?: return null
        val left = trimmed.substring(0, separatorMatch.range.first).trim()
        val right = trimmed.substring(separatorMatch.range.last + 1).trim()
        if (left.isEmpty() || right.isEmpty()) return null

        val leftMatch = leftQueryRegex.matchEntire(left) ?: return null
        val amount =
            leftMatch.groupValues[1]
                .replace(",", "")
                .toDoubleOrNull() ?: return null
        val fromUnit = leftMatch.groupValues[2].trim()
        if (fromUnit.isEmpty()) return null

        return ParsedQuery(amount = amount, fromUnitRaw = fromUnit, toUnitRaw = right)
    }

    private fun matchCompatibleUnits(
        fromCandidates: List<UnitDefinition>,
        toCandidates: List<UnitDefinition>,
    ): Pair<UnitDefinition, UnitDefinition>? {
        fromCandidates.forEach { from ->
            toCandidates.firstOrNull { it.category == from.category }?.let { to ->
                return from to to
            }
        }
        return null
    }

    private fun formatValue(value: Double): String {
        val normalizedValue = if (abs(value) < 1e-12) 0.0 else value
        val absolute = abs(normalizedValue)
        val formatter =
            if (absolute != 0.0 && (absolute < 1e-6 || absolute >= 1e9)) {
                scientificDecimalFormat
            } else {
                normalDecimalFormat
            }
        return formatter.format(normalizedValue)
    }

    private fun normalizeUnit(rawUnit: String): String {
        var normalized = rawUnit.trim().lowercase(Locale.getDefault())
        normalized = normalized.replace("\u00B5", "u") // micro sign
        normalized = normalized.replace("\u03BC", "u") // greek mu
        normalized = normalized.replace("\u00B0", "") // degree sign
        normalized = normalized.replace("\u00B2", "2")
        normalized = normalized.replace("\u00B3", "3")
        normalized = normalized.replace("^", "")
        normalized = normalized.replace(".", "")
        normalized = normalized.replace(Regex("\\s+"), " ")
        normalized = normalized.replace(" per ", "/")
        normalized = normalized.replace(" / ", "/")
        normalized = normalized.replace(" /", "/")
        normalized = normalized.replace("/ ", "/")
        normalized = normalized.replace("square ", "sq ")
        normalized = normalized.replace("squared", "2")
        normalized = normalized.replace("cubic ", "cu ")
        normalized = normalized.replace("cubed", "3")
        normalized = normalized.replace("-", " ")
        normalized = normalized.replace(Regex("\\s+"), " ")
        return normalized.trim()
    }
}

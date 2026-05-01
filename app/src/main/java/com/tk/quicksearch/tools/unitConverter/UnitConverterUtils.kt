package com.tk.quicksearch.tools.unitConverter

import java.math.BigDecimal
import java.math.RoundingMode
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
            pattern = "^([+-]?(?:(?:\\d{1,3}(?:,\\d{3})+)|\\d+)(?:\\.\\d+)?|[+-]?\\.\\d+)\\s*([\\p{L}0-9\\s./^°-]+)$",
            options = setOf(RegexOption.IGNORE_CASE),
        )
    private val defaultToAliasByFromAlias: Map<String, String> by lazy {
        mapOf(
            // Temperature
            "c" to "f",
            "f" to "c",
            // Weight
            "lb" to "kg",
            "lbs" to "kg",
            "pound" to "kg",
            "pounds" to "kg",
            "kg" to "lb",
            "kgs" to "lb",
            "kilogram" to "lb",
            "kilograms" to "lb",
            "oz" to "g",
            "ounce" to "g",
            "ounces" to "g",
            "g" to "oz",
            "gram" to "oz",
            "grams" to "oz",
            // Length
            "mi" to "km",
            "mile" to "km",
            "miles" to "km",
            "km" to "mi",
            "kilometer" to "mi",
            "kilometers" to "mi",
            "kilometre" to "mi",
            "kilometres" to "mi",
            "ft" to "m",
            "foot" to "m",
            "feet" to "m",
            "m" to "ft",
            "meter" to "ft",
            "meters" to "ft",
            "metre" to "ft",
            "metres" to "ft",
            "in" to "cm",
            "inch" to "cm",
            "inches" to "cm",
            "cm" to "in",
            "centimeter" to "in",
            "centimeters" to "in",
            "centimetre" to "in",
            "centimetres" to "in",
            // Volume
            "gal" to "l",
            "gallon" to "l",
            "gallons" to "l",
            "l" to "gal",
            "liter" to "gal",
            "liters" to "gal",
            "litre" to "gal",
            "litres" to "gal",
            "ml" to "fl oz",
            "milliliter" to "fl oz",
            "milliliters" to "fl oz",
            "millilitre" to "fl oz",
            "millilitres" to "fl oz",
            "floz" to "ml",
            "fl oz" to "ml",
            "fluid ounce" to "ml",
            "fluid ounces" to "ml",
            // Speed
            "mph" to "km/h",
            "km/h" to "mph",
            "kph" to "mph",
            "kmh" to "mph",
            "kmph" to "mph",
        ).mapKeys { normalizeUnit(it.key) }
            .mapValues { normalizeUnit(it.value) }
    }

    private val unitDisplayNames =
        mapOf(
            "m" to "metres",
            "km" to "kilometres",
            "cm" to "centimetres",
            "mm" to "millimetres",
            "um" to "micrometres",
            "nm" to "nanometres",
            "mi" to "miles",
            "yd" to "yards",
            "ft" to "feet",
            "in" to "inches",
            "nmi" to "nautical miles",
            "kg" to "kilograms",
            "g" to "grams",
            "mg" to "milligrams",
            "ug" to "micrograms",
            "lb" to "pounds",
            "oz" to "ounces",
            "st" to "stones",
            "tonne" to "metric tonnes",
            "us ton" to "US tons",
            "m2" to "square metres",
            "km2" to "square kilometres",
            "cm2" to "square centimetres",
            "mm2" to "square millimetres",
            "ft2" to "square feet",
            "in2" to "square inches",
            "yd2" to "square yards",
            "mi2" to "square miles",
            "acre" to "acres",
            "ha" to "hectares",
            "l" to "litres",
            "ml" to "millilitres",
            "m3" to "cubic metres",
            "cm3" to "cubic centimetres",
            "mm3" to "cubic millimetres",
            "ft3" to "cubic feet",
            "in3" to "cubic inches",
            "gal" to "gallons",
            "qt" to "quarts",
            "pt" to "pints",
            "cup" to "cups",
            "fl oz" to "fluid ounces",
            "tbsp" to "tablespoons",
            "tsp" to "teaspoons",
            "s" to "seconds",
            "ms" to "milliseconds",
            "us" to "microseconds",
            "ns" to "nanoseconds",
            "min" to "minutes",
            "h" to "hours",
            "day" to "days",
            "week" to "weeks",
            "month" to "months",
            "year" to "years",
            "m/s" to "metres per second",
            "km/h" to "kilometres per hour",
            "mph" to "miles per hour",
            "kt" to "knots",
            "ft/s" to "feet per second",
            "in/s" to "inches per second",
            "byte" to "bytes",
            "bit" to "bits",
            "KB" to "kilobytes",
            "MB" to "megabytes",
            "GB" to "gigabytes",
            "TB" to "terabytes",
            "KiB" to "kibibytes",
            "MiB" to "mebibytes",
            "GiB" to "gibibytes",
            "TiB" to "tebibytes",
            "J" to "joules",
            "kJ" to "kilojoules",
            "cal" to "calories",
            "kcal" to "kilocalories",
            "Wh" to "watt-hours",
            "kWh" to "kilowatt-hours",
            "eV" to "electronvolts",
            "BTU" to "BTUs",
            "W" to "watts",
            "kW" to "kilowatts",
            "mW" to "milliwatts",
            "hp" to "horsepower",
            "Pa" to "pascals",
            "kPa" to "kilopascals",
            "MPa" to "megapascals",
            "bar" to "bar",
            "mbar" to "millibars",
            "psi" to "psi",
            "atm" to "atmospheres",
            "torr" to "torr",
            "mmHg" to "millimetres of mercury",
            "rad" to "radians",
            "deg" to "degrees",
            "grad" to "gradians",
            "rev" to "revolutions",
            "Hz" to "hertz",
            "kHz" to "kilohertz",
            "MHz" to "megahertz",
            "GHz" to "gigahertz",
            "rpm" to "revolutions per minute",
            "C" to "degrees Celsius",
            "F" to "degrees Fahrenheit",
            "K" to "kelvin",
        )
    private val unitShortLabels =
        mapOf(
            "m" to "m",
            "km" to "km",
            "cm" to "cm",
            "mm" to "mm",
            "um" to "um",
            "nm" to "nm",
            "mi" to "mi",
            "yd" to "yd",
            "ft" to "ft",
            "in" to "in",
            "nmi" to "nmi",
            "kg" to "kg",
            "g" to "g",
            "mg" to "mg",
            "ug" to "ug",
            "lb" to "lbs",
            "oz" to "oz",
            "st" to "st",
            "tonne" to "t",
            "us ton" to "ton",
            "m2" to "m2",
            "km2" to "km2",
            "cm2" to "cm2",
            "mm2" to "mm2",
            "ft2" to "ft2",
            "in2" to "in2",
            "yd2" to "yd2",
            "mi2" to "mi2",
            "acre" to "ac",
            "ha" to "ha",
            "l" to "L",
            "ml" to "mL",
            "m3" to "m3",
            "cm3" to "cm3",
            "mm3" to "mm3",
            "ft3" to "ft3",
            "in3" to "in3",
            "gal" to "gal",
            "qt" to "qt",
            "pt" to "pt",
            "cup" to "cup",
            "fl oz" to "fl oz",
            "tbsp" to "tbsp",
            "tsp" to "tsp",
            "s" to "s",
            "ms" to "ms",
            "us" to "us",
            "ns" to "ns",
            "min" to "min",
            "h" to "h",
            "day" to "day",
            "week" to "wk",
            "month" to "mo",
            "year" to "yr",
            "m/s" to "m/s",
            "km/h" to "km/h",
            "mph" to "mph",
            "kt" to "kt",
            "ft/s" to "ft/s",
            "in/s" to "in/s",
            "byte" to "B",
            "bit" to "bit",
            "KB" to "KB",
            "MB" to "MB",
            "GB" to "GB",
            "TB" to "TB",
            "KiB" to "KiB",
            "MiB" to "MiB",
            "GiB" to "GiB",
            "TiB" to "TiB",
            "J" to "J",
            "kJ" to "kJ",
            "cal" to "cal",
            "kcal" to "kcal",
            "Wh" to "Wh",
            "kWh" to "kWh",
            "eV" to "eV",
            "BTU" to "BTU",
            "W" to "W",
            "kW" to "kW",
            "mW" to "mW",
            "hp" to "hp",
            "Pa" to "Pa",
            "kPa" to "kPa",
            "MPa" to "MPa",
            "bar" to "bar",
            "mbar" to "mbar",
            "psi" to "psi",
            "atm" to "atm",
            "torr" to "torr",
            "mmHg" to "mmHg",
            "rad" to "rad",
            "deg" to "deg",
            "grad" to "grad",
            "rev" to "rev",
            "Hz" to "Hz",
            "kHz" to "kHz",
            "MHz" to "MHz",
            "GHz" to "GHz",
            "rpm" to "rpm",
            "C" to "C",
            "F" to "F",
            "K" to "K",
        )

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
        registerLinear(UnitCategory.SPEED, "km/h", 0.2777777777777778, setOf("km/h", "kph", "kmh", "kmph", "kilometer per hour", "kilometers per hour", "kilometre per hour", "kilometres per hour"))
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
            aliases = setOf("c", "deg c", "degree c", "degrees c", "celsius", "centigrade", "celcius"),
            toCelsius = { value -> value },
            fromCelsius = { value -> value },
        )
        registerTemperature(
            symbol = "F",
            aliases = setOf("f", "deg f", "degree f", "degrees f", "fahrenheit"),
            toCelsius = { value -> (value - 32.0) * (5.0 / 9.0) },
            fromCelsius = { value -> (value * (9.0 / 5.0)) + 32.0 },
        )
        registerTemperature(
            symbol = "K",
            aliases = setOf("k", "kelvin", "kelvins"),
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
        val unitLabel = formatDisplayUnit(toUnit)
        return "$formattedValue $unitLabel"
    }

    private fun parseQuery(query: String): ParsedQuery? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null

        parseExplicitQuery(trimmed)?.let { return it }
        return parseCommonShorthandQuery(trimmed)
    }

    private fun parseExplicitQuery(query: String): ParsedQuery? {
        val separatorMatch = separatorRegex.find(query) ?: return null
        val left = query.substring(0, separatorMatch.range.first).trim()
        val right = query.substring(separatorMatch.range.last + 1).trim()
        if (left.isEmpty() || right.isEmpty()) return null

        val parsedLeft = parseAmountAndUnit(left) ?: return null
        return ParsedQuery(
            amount = parsedLeft.first,
            fromUnitRaw = parsedLeft.second,
            toUnitRaw = right,
        )
    }

    private fun parseCommonShorthandQuery(query: String): ParsedQuery? {
        val parsedInput = parseAmountAndUnit(query) ?: return null
        val fromUnitRaw = parsedInput.second
        val fromUnitAlias = normalizeUnit(fromUnitRaw)
        val defaultToAlias = defaultToAliasByFromAlias[fromUnitAlias] ?: return null

        return ParsedQuery(
            amount = parsedInput.first,
            fromUnitRaw = fromUnitRaw,
            toUnitRaw = defaultToAlias,
        )
    }

    private fun parseAmountAndUnit(input: String): Pair<Double, String>? {
        val leftMatch = leftQueryRegex.matchEntire(input.trim()) ?: return null
        val amount =
            leftMatch.groupValues[1]
                .replace(",", "")
                .toDoubleOrNull() ?: return null
        val fromUnit = leftMatch.groupValues[2].trim()
        if (fromUnit.isEmpty()) return null

        return amount to fromUnit
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
        val rounded =
            BigDecimal.valueOf(normalizedValue)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        return if (rounded == "-0") "0" else rounded
    }

    private fun formatDisplayUnit(unit: UnitDefinition): String {
        if (unit.category == UnitCategory.TEMPERATURE) {
            return when (unit.displaySymbol) {
                "C" -> "\u00B0C"
                "F" -> "\u00B0F"
                else -> "K"
            }
        }
        val unitLabel = unitDisplayNames[unit.displaySymbol] ?: unit.displaySymbol
        val unitShort = unitShortLabels[unit.displaySymbol] ?: unit.displaySymbol
        return "$unitLabel ($unitShort)"
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

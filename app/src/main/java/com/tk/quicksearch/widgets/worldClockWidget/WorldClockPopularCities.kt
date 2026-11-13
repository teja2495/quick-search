package com.tk.quicksearch.widgets.worldClockWidget

import android.icu.util.TimeZone

/**
 * Capitals and well-known cities that the time zone database doesn't name zones after, so they
 * can be searched for: "Mumbai" finds India's clock, "San Francisco" the US Pacific one.
 */
internal object WorldClockPopularCities {
    /** The city a zone is shown as when its namesake isn't the one people know it by. */
    private val headlineCities =
        mapOf(
            "Asia/Kolkata" to "New Delhi",
            "Asia/Shanghai" to "Beijing",
            "Asia/Karachi" to "Islamabad",
            "Asia/Ho_Chi_Minh" to "Hanoi",
        )

    private val citiesByZone =
        mapOf(
            // North America
            "America/New_York" to
                listOf(
                    "Washington", "Washington DC", "Boston", "Philadelphia", "Miami", "Atlanta", "Orlando",
                    "Charlotte", "Pittsburgh", "Baltimore", "Tampa", "Raleigh", "Cleveland", "Columbus",
                    "Jacksonville", "Newark", "Brooklyn", "Manhattan",
                ),
            "America/Chicago" to
                listOf(
                    "Dallas", "Houston", "Austin", "San Antonio", "Minneapolis", "St Louis", "Kansas City",
                    "Nashville", "New Orleans", "Milwaukee", "Oklahoma City", "Memphis",
                ),
            "America/Denver" to listOf("Salt Lake City", "Albuquerque", "Colorado Springs", "El Paso"),
            "America/Phoenix" to listOf("Tucson", "Scottsdale", "Arizona"),
            "America/Los_Angeles" to
                listOf(
                    "San Francisco", "Seattle", "San Diego", "San Jose", "Portland", "Las Vegas", "Sacramento",
                    "Oakland", "Silicon Valley", "Hollywood",
                ),
            "America/Anchorage" to listOf("Alaska", "Juneau", "Fairbanks"),
            "Pacific/Honolulu" to listOf("Hawaii", "Maui"),
            "America/Toronto" to listOf("Ottawa", "Montreal", "Quebec City", "Ontario"),
            "America/Vancouver" to listOf("Victoria", "British Columbia"),
            "America/Edmonton" to listOf("Calgary", "Alberta"),
            "America/Winnipeg" to listOf("Manitoba"),
            "America/Halifax" to listOf("Nova Scotia"),
            "America/Mexico_City" to listOf("Guadalajara", "Monterrey", "Puebla"),
            "America/Cancun" to listOf("Quintana Roo"),
            "America/Tijuana" to listOf("Baja California"),
            // Central and South America, Caribbean
            "America/Sao_Paulo" to listOf("Rio de Janeiro", "Brasilia", "Belo Horizonte", "Curitiba", "Porto Alegre"),
            "America/Argentina/Buenos_Aires" to listOf("Cordoba", "Rosario", "Mendoza"),
            "America/Bogota" to listOf("Medellin", "Cali", "Cartagena"),
            "America/Lima" to listOf("Cusco", "Arequipa"),
            "America/Santiago" to listOf("Valparaiso"),
            "America/Caracas" to listOf("Maracaibo"),
            "America/Guayaquil" to listOf("Quito"),
            "America/La_Paz" to listOf("Santa Cruz", "Sucre"),
            "America/Puerto_Rico" to listOf("San Juan"),
            "America/Santo_Domingo" to listOf("Punta Cana"),
            "America/Costa_Rica" to listOf("San Jose Costa Rica"),
            // Europe
            "Europe/London" to listOf("Manchester", "Birmingham", "Liverpool", "Leeds", "Glasgow", "Edinburgh", "Bristol", "Cardiff", "Belfast"),
            "Europe/Paris" to listOf("Marseille", "Lyon", "Nice", "Toulouse", "Bordeaux"),
            "Europe/Berlin" to listOf("Munich", "Frankfurt", "Hamburg", "Cologne", "Stuttgart", "Dusseldorf"),
            "Europe/Madrid" to listOf("Barcelona", "Valencia", "Seville", "Malaga", "Bilbao", "Ibiza"),
            "Europe/Rome" to listOf("Milan", "Naples", "Florence", "Venice", "Turin", "Bologna"),
            "Europe/Amsterdam" to listOf("Rotterdam", "The Hague", "Utrecht", "Eindhoven"),
            "Europe/Brussels" to listOf("Antwerp", "Ghent", "Bruges"),
            "Europe/Zurich" to listOf("Geneva", "Bern", "Basel", "Lausanne"),
            "Europe/Vienna" to listOf("Salzburg", "Innsbruck", "Graz"),
            "Europe/Stockholm" to listOf("Gothenburg", "Malmo"),
            "Europe/Oslo" to listOf("Bergen"),
            "Europe/Copenhagen" to listOf("Aarhus"),
            "Europe/Warsaw" to listOf("Krakow", "Gdansk", "Wroclaw"),
            "Europe/Prague" to listOf("Brno"),
            "Europe/Lisbon" to listOf("Porto"),
            "Europe/Athens" to listOf("Thessaloniki", "Santorini", "Crete"),
            "Europe/Istanbul" to listOf("Ankara", "Izmir", "Antalya"),
            "Europe/Moscow" to listOf("St Petersburg", "Saint Petersburg", "Kazan"),
            "Europe/Kiev" to listOf("Kiev", "Kharkiv", "Odesa", "Lviv"),
            "Europe/Dublin" to listOf("Cork", "Galway"),
            "Europe/Bucharest" to listOf("Cluj"),
            // Middle East and Africa
            "Asia/Dubai" to listOf("Abu Dhabi", "Sharjah"),
            "Asia/Riyadh" to listOf("Jeddah", "Mecca", "Medina", "Dammam"),
            "Asia/Qatar" to listOf("Doha"),
            "Asia/Jerusalem" to listOf("Tel Aviv", "Haifa"),
            "Asia/Tehran" to listOf("Mashhad", "Isfahan"),
            "Asia/Baghdad" to listOf("Basra", "Erbil"),
            "Africa/Cairo" to listOf("Alexandria", "Giza"),
            "Africa/Lagos" to listOf("Abuja", "Kano", "Ibadan"),
            "Africa/Nairobi" to listOf("Mombasa"),
            "Africa/Johannesburg" to listOf("Cape Town", "Durban", "Pretoria"),
            "Africa/Casablanca" to listOf("Rabat", "Marrakech", "Fes"),
            "Africa/Addis_Ababa" to listOf("Ethiopia"),
            "Africa/Accra" to listOf("Kumasi"),
            "Africa/Dar_es_Salaam" to listOf("Dodoma", "Zanzibar"),
            // Asia
            "Asia/Kolkata" to
                listOf(
                    "Delhi", "Mumbai", "Bombay", "Bangalore", "Bengaluru", "Chennai", "Madras", "Hyderabad",
                    "Pune", "Ahmedabad", "Jaipur", "Surat", "Lucknow", "Kanpur", "Nagpur", "Indore", "Bhopal",
                    "Patna", "Vadodara", "Visakhapatnam", "Vijayawada", "Guntur", "Kochi", "Cochin", "Coimbatore",
                    "Thiruvananthapuram", "Trivandrum", "Chandigarh", "Goa", "Noida", "Gurgaon", "Gurugram",
                    "Amritsar", "Varanasi", "Mysore", "Calcutta",
                ),
            "Asia/Karachi" to listOf("Lahore", "Faisalabad", "Rawalpindi", "Peshawar"),
            "Asia/Dhaka" to listOf("Chittagong"),
            "Asia/Colombo" to listOf("Kandy"),
            "Asia/Kathmandu" to listOf("Pokhara"),
            "Asia/Shanghai" to
                listOf(
                    "Peking", "Shenzhen", "Guangzhou", "Chengdu", "Wuhan", "Hangzhou", "Nanjing", "Xian",
                    "Chongqing", "Tianjin", "Suzhou",
                ),
            "Asia/Hong_Kong" to listOf("Kowloon"),
            "Asia/Taipei" to listOf("Kaohsiung", "Taichung"),
            "Asia/Tokyo" to listOf("Osaka", "Kyoto", "Yokohama", "Nagoya", "Sapporo", "Fukuoka", "Kobe", "Hiroshima"),
            "Asia/Seoul" to listOf("Busan", "Incheon", "Daegu"),
            "Asia/Singapore" to listOf("Sentosa"),
            "Asia/Kuala_Lumpur" to listOf("Penang", "Johor Bahru", "Putrajaya"),
            "Asia/Bangkok" to listOf("Phuket", "Chiang Mai", "Pattaya"),
            "Asia/Ho_Chi_Minh" to listOf("Hanoi", "Saigon", "Da Nang"),
            "Asia/Jakarta" to listOf("Surabaya", "Bandung"),
            "Asia/Makassar" to listOf("Bali", "Denpasar"),
            "Asia/Manila" to listOf("Cebu", "Quezon City", "Davao"),
            "Asia/Yangon" to listOf("Rangoon", "Mandalay"),
            "Asia/Almaty" to listOf("Astana"),
            "Asia/Tashkent" to listOf("Samarkand"),
            // Oceania
            "Australia/Sydney" to listOf("Canberra", "Melbourne", "Newcastle", "Wollongong"),
            "Australia/Brisbane" to listOf("Gold Coast", "Cairns", "Queensland"),
            "Australia/Perth" to listOf("Western Australia"),
            "Australia/Adelaide" to listOf("South Australia"),
            "Australia/Darwin" to listOf("Northern Territory"),
            "Australia/Hobart" to listOf("Tasmania"),
            "Pacific/Auckland" to listOf("Wellington", "Christchurch", "Queenstown"),
            "Pacific/Fiji" to listOf("Suva"),
        )

    /** Keyed by the device's canonical ids, which may differ from the ones above (Asia/Calcutta). */
    private val canonicalHeadlines by lazy { headlineCities.mapKeys { canonical(it.key) } }
    private val canonicalCities by lazy {
        citiesByZone.entries.groupBy({ canonical(it.key) }, { it.value }).mapValues { it.value.flatten() }
    }

    /** The city [zoneId] is shown as, if it isn't the zone's namesake; [zoneId] must be canonical. */
    fun headline(zoneId: String): String? = canonicalHeadlines[zoneId]

    /** Other cities on [zoneId]'s clock that people search by; [zoneId] must be canonical. */
    fun citiesIn(zoneId: String): List<String> = canonicalCities[zoneId].orEmpty()

    private fun canonical(zoneId: String): String =
        runCatching { TimeZone.getCanonicalID(zoneId) }.getOrNull() ?: zoneId
}

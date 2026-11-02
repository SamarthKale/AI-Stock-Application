package com.stockpredictor.app.data

/**
 * One of our offices, shown as a gold pin on the map screen.
 *
 * **The addresses and coordinates below are representative placeholders** (a well-known business
 * district in each city) -- replace them with the real office addresses by editing [OfficeData.all];
 * nothing else in the app needs to change. Latitude/longitude can be copied from Google Maps
 * (long-press the building -> the coordinates appear in the search box).
 *
 * [phone] is null on purpose until real numbers are filled in: the map's "Call" button is only
 * shown for offices that have one, so the app never dials a made-up number.
 */
data class OfficeLocation(
    val id: String,
    val name: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val hours: String = "Mon–Sat · 10:00–19:00",
    val phone: String? = null,
)

object OfficeData {
    val all: List<OfficeLocation> = listOf(
        OfficeLocation(
            id = "mumbai",
            name = "Mumbai HQ",
            city = "Mumbai",
            address = "Bandra Kurla Complex, Bandra East, Mumbai, Maharashtra 400051",
            latitude = 19.0660,
            longitude = 72.8679,
        ),
        OfficeLocation(
            id = "pune",
            name = "Pune Office",
            city = "Pune",
            address = "Rajiv Gandhi Infotech Park, Hinjewadi Phase 1, Pune, Maharashtra 411057",
            latitude = 18.5913,
            longitude = 73.7389,
        ),
        OfficeLocation(
            id = "nagpur",
            name = "Nagpur Office",
            city = "Nagpur",
            address = "Sitabuldi, Nagpur, Maharashtra 440012",
            latitude = 21.1458,
            longitude = 79.0882,
        ),
        OfficeLocation(
            id = "nashik",
            name = "Nashik Office",
            city = "Nashik",
            address = "College Road, Nashik, Maharashtra 422005",
            latitude = 20.0059,
            longitude = 73.7650,
        ),
    )
}

package com.example.data

import com.example.data.model.DropdownOption

/**
 * The single source for bundled ICAI dropdown data. The scraper never returns
 * these as live results; the ViewModel only falls back to them when a fetch
 * fails, and marks the UI "offline list" so stale values are visible. At check
 * time POU/course values are re-resolved against the live portal by displayed
 * text, so bundled numbers are lookup hints, not ground truth.
 */
object IcaiCatalog {

    val FALLBACK_REGIONS: List<DropdownOption> = listOf(
        DropdownOption("1", "Central"),
        DropdownOption("2", "Eastern"),
        DropdownOption("3", "Northern"),
        DropdownOption("4", "Southern"),
        DropdownOption("5", "Western")
    )

    fun fallbackPousForRegion(regionValue: String): List<DropdownOption> =
        when (regionValue) {
            "1" -> listOf(
                DropdownOption("101", "Kanpur"),
                DropdownOption("102", "Jaipur"),
                DropdownOption("103", "Lucknow"),
                DropdownOption("104", "Indore"),
                DropdownOption("105", "Bhopal"),
                DropdownOption("106", "Raipur"),
                DropdownOption("107", "Patna"),
                DropdownOption("108", "Varanasi"),
                DropdownOption("109", "Allahabad (Prayagraj)")
            )
            "2" -> listOf(
                DropdownOption("201", "Kolkata"),
                DropdownOption("202", "Bhubaneswar"),
                DropdownOption("203", "Guwahati"),
                DropdownOption("204", "Cuttack"),
                DropdownOption("205", "Siliguri"),
                DropdownOption("206", "Rourkela"),
                DropdownOption("207", "Jamshedpur"),
                DropdownOption("208", "Asansol")
            )
            "3" -> listOf(
                DropdownOption("301", "Delhi (Central)"),
                DropdownOption("302", "Delhi (North)"),
                DropdownOption("303", "Delhi (South)"),
                DropdownOption("304", "Chandigarh"),
                DropdownOption("305", "Gurgaon (Gurugram)"),
                DropdownOption("306", "Noida"),
                DropdownOption("307", "Faridabad"),
                DropdownOption("308", "Ludhiana"),
                DropdownOption("309", "Amritsar"),
                DropdownOption("310", "Ghaziabad")
            )
            "4" -> listOf(
                DropdownOption("3", "Chennai"),
                DropdownOption("4", "Bengaluru"),
                DropdownOption("5", "Hyderabad"),
                DropdownOption("6", "Coimbatore"),
                DropdownOption("7", "Ernakulam (Kochi)"),
                DropdownOption("8", "Madurai"),
                DropdownOption("9", "Visakhapatnam"),
                DropdownOption("10", "Vijayawada"),
                DropdownOption("11", "Kozhikode"),
                DropdownOption("12", "Thiruvananthapuram"),
                DropdownOption("13", "Mangaluru"),
                DropdownOption("14", "Mysuru"),
                DropdownOption("15", "Salem"),
                DropdownOption("16", "Tiruchirappalli")
            )
            "5" -> listOf(
                DropdownOption("501", "Mumbai"),
                DropdownOption("502", "Pune"),
                DropdownOption("503", "Ahmedabad"),
                DropdownOption("504", "Surat"),
                DropdownOption("505", "Nagpur"),
                DropdownOption("506", "Vadodara"),
                DropdownOption("507", "Rajkot"),
                DropdownOption("508", "Nashik"),
                DropdownOption("509", "Thane"),
                DropdownOption("510", "Navi Mumbai"),
                DropdownOption("511", "Goa")
            )
            else -> listOf(
                DropdownOption("3", "Chennai"),
                DropdownOption("4", "Bengaluru"),
                DropdownOption("501", "Mumbai"),
                DropdownOption("301", "Delhi"),
                DropdownOption("201", "Kolkata")
            )
        }

    // The portal has no standalone course endpoint (courses appear only after a
    // POU postback), so this bundled list is the entry point; live checks
    // re-resolve the value by course label with token matching.
    val COURSES: List<DropdownOption> = listOf(
        DropdownOption("48", "AICITSS - Advanced Information Technology (Adv ITT)"),
        DropdownOption("49", "AICITSS - Management & Communication Skills (MCS)"),
        DropdownOption("46", "ICITSS - Information Technology Course (ITT)"),
        DropdownOption("47", "ICITSS - Orientation Course (OC)")
    )
}

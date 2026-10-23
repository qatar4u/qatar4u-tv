package net.qatar4u.tv.data

data class City(val id: String, val ar: String, val en: String, val lat: Double, val lon: Double, val zone: String) {
    fun name(lang: String) = if (lang == "ar") ar else en
}

/** Cities offered in the picker. Qatar first, then a few Gulf capitals. Coordinates are city centres. */
object Cities {
    val DOHA = City("doha", "الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar")

    val all = listOf(
        DOHA,
        City("lusail", "لوسيل", "Lusail", 25.4300, 51.4900, "Asia/Qatar"),
        City("rayyan", "الريان", "Al Rayyan", 25.2919, 51.4244, "Asia/Qatar"),
        City("wakrah", "الوكرة", "Al Wakrah", 25.1715, 51.6034, "Asia/Qatar"),
        City("khor", "الخور", "Al Khor", 25.6839, 51.5058, "Asia/Qatar"),
        City("dukhan", "دخان", "Dukhan", 25.4250, 50.7850, "Asia/Qatar"),
        City("mesaieed", "مسيعيد", "Mesaieed", 24.9900, 51.5500, "Asia/Qatar"),
        City("shamal", "مدينة الشمال", "Madinat ash Shamal", 26.1293, 51.2009, "Asia/Qatar"),
        City("umm_salal", "أم صلال", "Umm Salal", 25.4150, 51.4040, "Asia/Qatar"),
        City("riyadh", "الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        City("makkah", "مكة المكرمة", "Makkah", 21.3891, 39.8579, "Asia/Riyadh"),
        City("dubai", "دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
        City("abudhabi", "أبوظبي", "Abu Dhabi", 24.4539, 54.3773, "Asia/Dubai"),
        City("kuwait", "الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"),
        City("manama", "المنامة", "Manama", 26.2285, 50.5860, "Asia/Bahrain"),
        City("muscat", "مسقط", "Muscat", 23.5880, 58.3829, "Asia/Muscat"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

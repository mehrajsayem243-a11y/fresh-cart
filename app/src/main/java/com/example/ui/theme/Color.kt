package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppColorPalette(
    val displayName: String,
    val primaryHex: String,
    val lightPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val lightFilledBackground: Color,
    val darkFilledBackground: Color
) {
    DYNAMIC(
        displayName = "Dynamic (Wallpaper)",
        primaryHex = "#0B57D0",
        lightPrimary = Color(0xFF0B57D0),
        lightContainer = Color(0xFFD3E3FD),
        darkPrimary = Color(0xFFA8C7FA),
        darkContainer = Color(0xFF04409F),
        lightFilledBackground = Color(0xFFF0F4FC),
        darkFilledBackground = Color(0xFF14171E)
    ),
    BLUE(
        displayName = "Ocean Blue",
        primaryHex = "#0B57D0",
        lightPrimary = Color(0xFF0B57D0),
        lightContainer = Color(0xFFD3E3FD),
        darkPrimary = Color(0xFFA8C7FA),
        darkContainer = Color(0xFF04409F),
        lightFilledBackground = Color(0xFFF0F4FC),
        darkFilledBackground = Color(0xFF13171F)
    ),
    GREEN(
        displayName = "Fresh Green",
        primaryHex = "#1B6D36",
        lightPrimary = Color(0xFF1B6D36),
        lightContainer = Color(0xFFA3F5B0),
        darkPrimary = Color(0xFF88D896),
        darkContainer = Color(0xFF005322),
        lightFilledBackground = Color(0xFFF1F8F2),
        darkFilledBackground = Color(0xFF131A14)
    ),
    ORANGE(
        displayName = "Sunset Orange",
        primaryHex = "#C05600",
        lightPrimary = Color(0xFF984700),
        lightContainer = Color(0xFFFFDCC5),
        darkPrimary = Color(0xFFFFB784),
        darkContainer = Color(0xFF753400),
        lightFilledBackground = Color(0xFFFDF4EE),
        darkFilledBackground = Color(0xFF1E1712)
    ),
    PURPLE(
        displayName = "Royal Berry",
        primaryHex = "#784A88",
        lightPrimary = Color(0xFF764A83),
        lightContainer = Color(0xFFFBD7FF),
        darkPrimary = Color(0xFFE5B4F1),
        darkContainer = Color(0xFF5C336A),
        lightFilledBackground = Color(0xFFF9F2FB),
        darkFilledBackground = Color(0xFF1A131C)
    ),

    // 23 Additional Designer Palettes
    TEAL(
        displayName = "Deep Teal",
        primaryHex = "#006A6A",
        lightPrimary = Color(0xFF006A6A),
        lightContainer = Color(0xFF9CF1F0),
        darkPrimary = Color(0xFF80D5D4),
        darkContainer = Color(0xFF004F4F),
        lightFilledBackground = Color(0xFFEEF8F8),
        darkFilledBackground = Color(0xFF111818)
    ),
    CRIMSON(
        displayName = "Ruby Crimson",
        primaryHex = "#BA1A1A",
        lightPrimary = Color(0xFFBA1A1A),
        lightContainer = Color(0xFFFFDAD6),
        darkPrimary = Color(0xFFFFB4AB),
        darkContainer = Color(0xFF93000A),
        lightFilledBackground = Color(0xFFFCF2F1),
        darkFilledBackground = Color(0xFF1C1312)
    ),
    ROSE(
        displayName = "Blush Rose",
        primaryHex = "#9C4266",
        lightPrimary = Color(0xFF9C4266),
        lightContainer = Color(0xFFFFD9E4),
        darkPrimary = Color(0xFFFFAFD0),
        darkContainer = Color(0xFF7F2B4E),
        lightFilledBackground = Color(0xFFFDF2F5),
        darkFilledBackground = Color(0xFF1C1216)
    ),
    AMBER(
        displayName = "Golden Amber",
        primaryHex = "#855400",
        lightPrimary = Color(0xFF855400),
        lightContainer = Color(0xFFFFDDB8),
        darkPrimary = Color(0xFFFFB961),
        darkContainer = Color(0xFF653E00),
        lightFilledBackground = Color(0xFFFDF5EB),
        darkFilledBackground = Color(0xFF1C1710)
    ),
    INDIGO(
        displayName = "Midnight Indigo",
        primaryHex = "#3E5AA9",
        lightPrimary = Color(0xFF3E5AA9),
        lightContainer = Color(0xFFDBE1FF),
        darkPrimary = Color(0xFFB4C5FF),
        darkContainer = Color(0xFF234290),
        lightFilledBackground = Color(0xFFF1F3FB),
        darkFilledBackground = Color(0xFF13151D)
    ),
    EMERALD(
        displayName = "Vibrant Emerald",
        primaryHex = "#006C4C",
        lightPrimary = Color(0xFF006C4C),
        lightContainer = Color(0xFF89F8C6),
        darkPrimary = Color(0xFF6CDBAB),
        darkContainer = Color(0xFF005138),
        lightFilledBackground = Color(0xFFEFF9F4),
        darkFilledBackground = Color(0xFF111915)
    ),
    CORAL(
        displayName = "Living Coral",
        primaryHex = "#A53C2D",
        lightPrimary = Color(0xFFA53C2D),
        lightContainer = Color(0xFFFFDAD3),
        darkPrimary = Color(0xFFFFB4A6),
        darkContainer = Color(0xFF842518),
        lightFilledBackground = Color(0xFFFDF3F1),
        darkFilledBackground = Color(0xFF1D1412)
    ),
    LAVENDER(
        displayName = "Lavender Mist",
        primaryHex = "#67558E",
        lightPrimary = Color(0xFF67558E),
        lightContainer = Color(0xFFEBDDFF),
        darkPrimary = Color(0xFFD2BCFD),
        darkContainer = Color(0xFF4F3E75),
        lightFilledBackground = Color(0xFFF6F2FB),
        darkFilledBackground = Color(0xFF17141C)
    ),
    LIME(
        displayName = "Electric Lime",
        primaryHex = "#556500",
        lightPrimary = Color(0xFF556500),
        lightContainer = Color(0xFFD7ED76),
        darkPrimary = Color(0xFFBBD15D),
        darkContainer = Color(0xFF3F4C00),
        lightFilledBackground = Color(0xFFF8F9EC),
        darkFilledBackground = Color(0xFF17190F)
    ),
    MINT(
        displayName = "Crisp Mint",
        primaryHex = "#006B58",
        lightPrimary = Color(0xFF006B58),
        lightContainer = Color(0xFF7BF7DC),
        darkPrimary = Color(0xFF5CDBBF),
        darkContainer = Color(0xFF005142),
        lightFilledBackground = Color(0xFFEFF8F6),
        darkFilledBackground = Color(0xFF111816)
    ),
    PEACH(
        displayName = "Sweet Peach",
        primaryHex = "#9E4627",
        lightPrimary = Color(0xFF9E4627),
        lightContainer = Color(0xFFFFDBD0),
        darkPrimary = Color(0xFFFFB59E),
        darkContainer = Color(0xFF802F13),
        lightFilledBackground = Color(0xFFFDF4F1),
        darkFilledBackground = Color(0xFF1C1412)
    ),
    SKY(
        displayName = "Cerulean Sky",
        primaryHex = "#006495",
        lightPrimary = Color(0xFF006495),
        lightContainer = Color(0xFFCBE6FF),
        darkPrimary = Color(0xFF8FCDFF),
        darkContainer = Color(0xFF004B72),
        lightFilledBackground = Color(0xFFEFF6FB),
        darkFilledBackground = Color(0xFF10161C)
    ),
    PLUM(
        displayName = "Sugar Plum",
        primaryHex = "#814C68",
        lightPrimary = Color(0xFF814C68),
        lightContainer = Color(0xFFFFD7E9),
        darkPrimary = Color(0xFFF4B3D4),
        darkContainer = Color(0xFF673550),
        lightFilledBackground = Color(0xFFFAF2F6),
        darkFilledBackground = Color(0xFF1A1318)
    ),
    FOREST(
        displayName = "Alpine Forest",
        primaryHex = "#2F6A44",
        lightPrimary = Color(0xFF2F6A44),
        lightContainer = Color(0xFFB2F2C2),
        darkPrimary = Color(0xFF97D5A7),
        darkContainer = Color(0xFF15512F),
        lightFilledBackground = Color(0xFFF1F7F3),
        darkFilledBackground = Color(0xFF131814)
    ),
    CHOCOLATE(
        displayName = "Warm Cocoa",
        primaryHex = "#825136",
        lightPrimary = Color(0xFF825136),
        lightContainer = Color(0xFFFFDBCA),
        darkPrimary = Color(0xFFF6B896),
        darkContainer = Color(0xFF673A21),
        lightFilledBackground = Color(0xFFFAF4F0),
        darkFilledBackground = Color(0xFF1A1512)
    ),
    SLATE(
        displayName = "Nordic Slate",
        primaryHex = "#4F6070",
        lightPrimary = Color(0xFF4F6070),
        lightContainer = Color(0xFFD2E4F7),
        darkPrimary = Color(0xFFB7C8DB),
        darkContainer = Color(0xFF374857),
        lightFilledBackground = Color(0xFFF2F4F7),
        darkFilledBackground = Color(0xFF14171A)
    ),
    MAGENTA(
        displayName = "Vivid Magenta",
        primaryHex = "#9F2B68",
        lightPrimary = Color(0xFF9F2B68),
        lightContainer = Color(0xFFFFD8E5),
        darkPrimary = Color(0xFFFFAFD2),
        darkContainer = Color(0xFF820F4F),
        lightFilledBackground = Color(0xFFFCF1F6),
        darkFilledBackground = Color(0xFF1B1117)
    ),
    OLIVE(
        displayName = "Tuscan Olive",
        primaryHex = "#606024",
        lightPrimary = Color(0xFF606024),
        lightContainer = Color(0xFFE6E59A),
        darkPrimary = Color(0xFFCAC980),
        darkContainer = Color(0xFF48480F),
        lightFilledBackground = Color(0xFFF8F7EE),
        darkFilledBackground = Color(0xFF171710)
    ),
    TERRACOTTA(
        displayName = "Terracotta Sun",
        primaryHex = "#964926",
        lightPrimary = Color(0xFF964926),
        lightContainer = Color(0xFFFFDBCD),
        darkPrimary = Color(0xFFFFB693),
        darkContainer = Color(0xFF793211),
        lightFilledBackground = Color(0xFFFDF4EF),
        darkFilledBackground = Color(0xFF1C1410)
    ),
    CYAN(
        displayName = "Polar Cyan",
        primaryHex = "#00687A",
        lightPrimary = Color(0xFF00687A),
        lightContainer = Color(0xFFABEDFF),
        darkPrimary = Color(0xFF55D6F4),
        darkContainer = Color(0xFF004E5D),
        lightFilledBackground = Color(0xFFEEF7F9),
        darkFilledBackground = Color(0xFF10171A)
    ),
    MARIGOLD(
        displayName = "Marigold Sun",
        primaryHex = "#7A5900",
        lightPrimary = Color(0xFF7A5900),
        lightContainer = Color(0xFFFFDF94),
        darkPrimary = Color(0xFFFABD00),
        darkContainer = Color(0xFF5C4200),
        lightFilledBackground = Color(0xFFFAF6EC),
        darkFilledBackground = Color(0xFF1A170F)
    ),
    CHERRY(
        displayName = "Black Cherry",
        primaryHex = "#8F444E",
        lightPrimary = Color(0xFF8F444E),
        lightContainer = Color(0xFFFFD9DD),
        darkPrimary = Color(0xFFFFB2BC),
        darkContainer = Color(0xFF732E37),
        lightFilledBackground = Color(0xFFFAF2F3),
        darkFilledBackground = Color(0xFF1A1214)
    ),
    CHARCOAL(
        displayName = "Onyx Noir",
        primaryHex = "#484848",
        lightPrimary = Color(0xFF333333),
        lightContainer = Color(0xFFE0E0E0),
        darkPrimary = Color(0xFFCCCCCC),
        darkContainer = Color(0xFF333333),
        lightFilledBackground = Color(0xFFF2F2F2),
        darkFilledBackground = Color(0xFF121212)
    )
}

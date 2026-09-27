package com.scanflow.app.domain.model

enum class CompressionLevel(val displayName: String, val imageQuality: Int, val maxImageDpi: Int) {
    LOW("Low Compression", 80, 200),
    MEDIUM("Medium Compression", 60, 150),
    HIGH("High Compression", 40, 100),
    CUSTOM("Custom Compression", 65, 150)
}

data class CompressionConfig(
    val level: CompressionLevel = CompressionLevel.MEDIUM,
    val customQuality: Int = 65,
    val customDpi: Int = 150,
    val stripMetadata: Boolean = true,
    val compressImages: Boolean = true
)

enum class WatermarkPosition {
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    DIAGONAL_CENTER
}

data class WatermarkConfig(
    val text: String = "",
    val imagePath: String? = null,
    val opacity: Float = 0.35f,
    val rotationDegrees: Float = -45f,
    val fontSizeSp: Float = 36f,
    val position: WatermarkPosition = WatermarkPosition.DIAGONAL_CENTER,
    val pageRange: String = "all" // "all", "1,2,3", "1-5", "odd", "even"
)

enum class PageNumberPosition {
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    TOP_LEFT
}

data class PageNumberConfig(
    val position: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val startNumber: Int = 1,
    val prefix: String = "Page ",
    val suffix: String = "",
    val fontSizeSp: Float = 10f,
    val marginPt: Float = 36f,
    val pageRange: String = "all"
)

data class SecurityConfig(
    val userPassword: String? = null,
    val ownerPassword: String? = null,
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = true,
    val keyLengthBits: Int = 128 // 128 or 256
)

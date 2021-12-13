package org.ametro.utils.misc

import androidx.annotation.ColorInt

data class ColorUtils(
    val a: Float,
    val r: Float,
    val g: Float,
    val b: Float
) {
    companion object {
        fun fromColorInt(@ColorInt color: Int): ColorUtils {
            return ColorUtils(
                /* alpha */ ((color shr 24) and 0xff) / 255.0f,
                /* red   */ ((color shr 16) and 0xff) / 255.0f,
                /* green */ ((color shr 8) and 0xff) / 255.0f,
                /* blue  */ ((color) and 0xff) / 255.0f
            )
        }
    }

    @ColorInt
    fun toColorInt(): Int {
        return /* */ ((a * 255.0f + 0.5f).toInt() shl 24) or
                /**/ ((r * 255.0f + 0.5f).toInt() shl 16) or
                /**/ ((g * 255.0f + 0.5f).toInt() shl 8) or
                /**/ ((b * 255.0f + 0.5f).toInt())
    }

    private inline fun applyAll(other: ColorUtils, 
                                f: (Float, Float) -> Float): ColorUtils {
        return ColorUtils(
            f(a, other.a),
            f(r, other.r),
            f(g, other.g),
            f(b, other.b)
        )
    }

    fun multiply(other: ColorUtils): ColorUtils {
        return applyAll(other) { a, b -> a * b }
    }

    fun multiply(@ColorInt other: Int): ColorUtils {
        return multiply(fromColorInt(other))
    }

    fun screen(other: ColorUtils): ColorUtils {
        return applyAll(other) { a, b -> 1f - (1f - a) * (1f - b) }
    }

    fun screen(@ColorInt other: Int): ColorUtils {
        return screen(fromColorInt(other))
    }
}
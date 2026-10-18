package com.fenji.scoretrace.ui.screen.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/** 再选科目赋分等级与转换区间。 */
enum class ConvertGrade(val label: String, val t1: Int, val t2: Int) {
    A("A", 86, 100),
    B("B", 71, 85),
    C("C", 56, 70),
    D("D", 41, 55),
    E("E", 30, 40),
}

data class ConvertResult(
    val subject: String,
    val rawScore: Int,
    val percentile: Float,
    val grade: ConvertGrade,
    val convertedScore: Int,
    val province: String,
    val supported: Boolean,
)

/**
 * 新高考 3+1+2 再选科目赋分估算。
 *
 * 云南等级比例：A 17% / B 33% / C 33% / D 16% / E 1%；
 * 其他省份按全国常见的 15/35/35/13/2 比例，但当前版本明确标注为暂不支持。
 *
 * 原始分区间没有官方公布，按常见区间估算：A 90-100 / B 80-89 / C 70-79 / D 60-69 / E 30-59，
 * 再代入等比例转换公式。结果仅作参考，不等同于官方赋分。
 */
@HiltViewModel
class ScoreConvertViewModel @Inject constructor(
    userPreferences: UserPreferences,
) : ViewModel() {

    val province: StateFlow<String> = userPreferences.province.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "云南",
    )

    fun isSupported(province: String): Boolean = province.startsWith("云南")

    fun calculate(
        subject: String,
        rawScoreText: String,
        percentileText: String,
        province: String,
    ): ConvertResult? {
        val raw = rawScoreText.toFloatOrNull() ?: return null
        val percentile = percentileText.toFloatOrNull() ?: return null
        if (raw !in 0f..100f || percentile !in 0f..100f) return null
        if (!isSupported(province)) {
            return ConvertResult(
                subject = subject,
                rawScore = raw.roundToInt(),
                percentile = percentile,
                grade = ConvertGrade.E,
                convertedScore = 0,
                province = province,
                supported = false,
            )
        }

        val grade = gradeOf(percentile)
        val band = rawBandOf(grade)
        val s = raw.coerceIn(band.first, band.second)
        val (s1, s2) = band
        val denominator = (s2 - s1).coerceAtLeast(0.0001f)
        val converted = (
            (s - s1) * grade.t2 + (s2 - s) * grade.t1
        ) / denominator
        return ConvertResult(
            subject = subject,
            rawScore = raw.roundToInt(),
            percentile = percentile,
            grade = grade,
            convertedScore = converted.roundToInt().coerceIn(grade.t1, grade.t2),
            province = province,
            supported = true,
        )
    }

    /** 云南累计比例：17 / 50 / 83 / 99 / 100。 */
    private fun gradeOf(percentile: Float): ConvertGrade = when {
        percentile <= 17f -> ConvertGrade.A
        percentile <= 50f -> ConvertGrade.B
        percentile <= 83f -> ConvertGrade.C
        percentile <= 99f -> ConvertGrade.D
        else -> ConvertGrade.E
    }

    /**
     * 估算该等级对应的原始分区间 [S1, S2]。
     * 官方不公布原始分切线，这里用常见区间估算：A 90-100 / B 80-89 / C 70-79 / D 60-69 / E 30-59。
     */
    private fun rawBandOf(grade: ConvertGrade): Pair<Float, Float> = when (grade) {
        ConvertGrade.A -> 90f to 100f
        ConvertGrade.B -> 80f to 89f
        ConvertGrade.C -> 70f to 79f
        ConvertGrade.D -> 60f to 69f
        ConvertGrade.E -> 30f to 59f
    }

    fun formatPercentile(value: Float): String = String.format(Locale.US, "%.1f", value)
}

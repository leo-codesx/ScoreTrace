package com.fenji.scoretrace.ui.screen.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.repository.ScoreSectionData
import com.fenji.scoretrace.data.repository.ScoreSectionRepository
import com.fenji.scoretrace.data.repository.ScoreSectionTable
import com.fenji.scoretrace.data.repository.cutoffScoreForPercentile
import com.fenji.scoretrace.data.repository.entryOf
import com.fenji.scoretrace.data.repository.percentileOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val source: String,
    val cumulative: Int? = null,
    val total: Int? = null,
)

/**
 * 新高考 3+1+2 再选科目赋分。
 *
 * 优先用 assets/score_section_yunnan.json 的一分一段表：
 * 原始分 → 累计人数 → 排名百分比 → 等级 → 用表内等级切线做等比例转换。
 * 数据未录入、分数未命中或非云南时，回退到手动百分比 + 固定原始分区间估算。
 *
 * 等级比例：A 15% / B 35% / C 35% / D 13% / E 2%。
 */
@HiltViewModel
class ScoreConvertViewModel @Inject constructor(
    userPreferences: UserPreferences,
    private val scoreSectionRepository: ScoreSectionRepository,
) : ViewModel() {

    val province: StateFlow<String> = userPreferences.province.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "云南",
    )

    private val _sectionData = MutableStateFlow<ScoreSectionData?>(null)
    val sectionData: StateFlow<ScoreSectionData?> = _sectionData.asStateFlow()

    init {
        viewModelScope.launch {
            _sectionData.value = scoreSectionRepository.load()
        }
    }

    fun tableFor(subject: String, province: String): ScoreSectionTable? {
        if (!province.startsWith("云南")) return null
        return _sectionData.value?.tableForSubject(subject)
    }

    fun hasSectionData(subject: String, province: String): Boolean = tableFor(subject, province) != null

    /**
     * @param manualPercentileText 仅当一分一段数据不可用/未命中时使用。
     */
    fun calculate(
        subject: String,
        rawScoreText: String,
        manualPercentileText: String,
        province: String,
    ): ConvertResult? {
        val raw = rawScoreText.toFloatOrNull() ?: return null
        if (raw !in 0f..100f) return null

        val table = tableFor(subject, province)
        val entry = table?.entryOf(raw.roundToInt())
        if (table != null && entry != null) {
            val percentile = table.percentileOf(entry)
            return convert(
                subject = subject,
                raw = raw,
                percentile = percentile,
                grade = gradeOf(percentile),
                band = dataBandOf(table, gradeOf(percentile)),
                province = province,
                source = "一分一段表",
                cumulative = entry.cumulative,
                total = table.total,
            )
        }

        val percentile = manualPercentileText.toFloatOrNull() ?: return null
        if (percentile !in 0f..100f) return null
        return convert(
            subject = subject,
            raw = raw,
            percentile = percentile,
            grade = gradeOf(percentile),
            band = estimatedBandOf(gradeOf(percentile)),
            province = province,
            source = "手动估算",
        )
    }

    /** 等级比例：A 15 / B 35 / C 35 / D 13 / E 2。 */
    private fun gradeOf(percentile: Float): ConvertGrade = when {
        percentile <= 15f -> ConvertGrade.A
        percentile <= 50f -> ConvertGrade.B
        percentile <= 85f -> ConvertGrade.C
        percentile <= 98f -> ConvertGrade.D
        else -> ConvertGrade.E
    }

    /**
     * 用一分一段表反查等级原始分区间：
     * A [cut(15), max]、B [cut(50), cut(15)]、C [cut(85), cut(50)]、
     * D [cut(98), cut(85)]、E [cut(100), cut(98)]。
     */
    private fun dataBandOf(table: ScoreSectionTable, grade: ConvertGrade): Pair<Float, Float> {
        val maxScore = table.sections.firstOrNull()?.score?.toFloat() ?: 100f
        val cut15 = table.cutoffScoreForPercentile(15f)
        val cut50 = table.cutoffScoreForPercentile(50f)
        val cut85 = table.cutoffScoreForPercentile(85f)
        val cut98 = table.cutoffScoreForPercentile(98f)
        val cut100 = table.cutoffScoreForPercentile(100f)
        return when (grade) {
            ConvertGrade.A -> cut15 to maxScore
            ConvertGrade.B -> cut50 to cut15
            ConvertGrade.C -> cut85 to cut50
            ConvertGrade.D -> cut98 to cut85
            ConvertGrade.E -> cut100 to cut98
        }
    }

    /** 无一分一段数据时的原始分区间估算。 */
    private fun estimatedBandOf(grade: ConvertGrade): Pair<Float, Float> = when (grade) {
        ConvertGrade.A -> 90f to 100f
        ConvertGrade.B -> 80f to 89f
        ConvertGrade.C -> 70f to 79f
        ConvertGrade.D -> 60f to 69f
        ConvertGrade.E -> 30f to 59f
    }

    private fun convert(
        subject: String,
        raw: Float,
        percentile: Float,
        grade: ConvertGrade,
        band: Pair<Float, Float>,
        province: String,
        source: String,
        cumulative: Int? = null,
        total: Int? = null,
    ): ConvertResult {
        val (s1, s2) = band
        val denominator = (s2 - s1).takeIf { it > 0f } ?: 1f
        val s = raw.coerceIn(s1, s2)
        val converted = ((s - s1) * grade.t2 + (s2 - s) * grade.t1) / denominator
        return ConvertResult(
            subject = subject,
            rawScore = raw.roundToInt(),
            percentile = percentile,
            grade = grade,
            convertedScore = converted.roundToInt().coerceIn(grade.t1, grade.t2),
            province = province,
            source = source,
            cumulative = cumulative,
            total = total,
        )
    }

    fun formatPercentile(value: Float): String = String.format(Locale.US, "%.1f", value)
}

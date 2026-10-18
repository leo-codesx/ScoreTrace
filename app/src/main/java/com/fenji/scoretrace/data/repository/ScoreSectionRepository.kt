package com.fenji.scoretrace.data.repository

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 一分一段表中的一行：本段人数与累计人数（位次）。 */
data class ScoreSectionEntry(
    val score: Int,
    val count: Int,
    val cumulative: Int,
)

/** 某一科类/科目的完整一分一段表。 */
data class ScoreSectionTable(
    val total: Int,
    val sections: List<ScoreSectionEntry>,
)

/** assets/score_section_yunnan.json 的根结构。 */
data class ScoreSectionData(
    val province: String,
    val year: Int,
    val placeholder: Boolean = true,
    val note: String = "",
    val physics: ScoreSectionTable = ScoreSectionTable(0, emptyList()),
    val history: ScoreSectionTable = ScoreSectionTable(0, emptyList()),
    val subjects: Map<String, ScoreSectionTable> = emptyMap(),
    /** 再选科目无官方一分一段表时，subjects 为估算数据。 */
    val subjectsEstimated: Boolean = false,
) {
    fun tableForCategory(category: String): ScoreSectionTable? {
        val table = when (category) {
            "物理" -> physics
            "历史" -> history
            else -> null
        }
        return table?.takeIf { it.total > 0 && it.sections.isNotEmpty() }
    }

    fun tableForSubject(subject: String): ScoreSectionTable? =
        subjects[subject]?.takeIf { it.total > 0 && it.sections.isNotEmpty() }
}

interface ScoreSectionRepository {
    suspend fun load(): ScoreSectionData?
}

@Singleton
class DefaultScoreSectionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
) : ScoreSectionRepository {

    private var cached: ScoreSectionData? = null

    override suspend fun load(): ScoreSectionData? = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        runCatching {
            val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            gson.fromJson(json, ScoreSectionData::class.java)
        }.onSuccess { cached = it }.getOrNull()
    }

    private companion object {
        const val ASSET_NAME = "score_section_yunnan.json"
    }
}

/** 查某分数在本表中的行；没有精确行返回 null。 */
fun ScoreSectionTable.entryOf(score: Int): ScoreSectionEntry? =
    sections.firstOrNull { it.score == score }

/** 累计人数 → 排名百分比（前 x%）。 */
fun ScoreSectionTable.percentileOf(entry: ScoreSectionEntry): Float =
    if (total > 0) (entry.cumulative * 100f / total).coerceIn(0f, 100f) else 0f

/** 给定排名百分比，反查对应的原始分切线（用于等级区间边界）。 */
fun ScoreSectionTable.cutoffScoreForPercentile(percentile: Float): Float {
    if (total <= 0 || sections.isEmpty()) return 0f
    val target = total * percentile.coerceIn(0f, 100f) / 100f
    if (target <= 0f) return sections.first().score.toFloat()
    return (sections.firstOrNull { it.cumulative >= target } ?: sections.last()).score.toFloat()
}

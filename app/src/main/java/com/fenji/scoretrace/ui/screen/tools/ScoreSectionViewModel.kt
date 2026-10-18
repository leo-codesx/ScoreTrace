package com.fenji.scoretrace.ui.screen.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.repository.ScoreSectionData
import com.fenji.scoretrace.data.repository.ScoreSectionRepository
import com.fenji.scoretrace.data.repository.entryOf
import com.fenji.scoretrace.data.repository.percentileOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectionQuery(
    val category: String,
    val score: Int,
    val count: Int,
    val cumulative: Int,
    val percentile: Float,
    val total: Int,
)

@HiltViewModel
class ScoreSectionViewModel @Inject constructor(
    private val repository: ScoreSectionRepository,
) : ViewModel() {

    private val _data = MutableStateFlow<ScoreSectionData?>(null)
    val data: StateFlow<ScoreSectionData?> = _data.asStateFlow()

    init {
        viewModelScope.launch { _data.value = repository.load() }
    }

    fun hasData(category: String): Boolean =
        _data.value?.tableForCategory(category) != null

    fun query(category: String, scoreText: String): SectionQuery? {
        val score = scoreText.toIntOrNull() ?: return null
        val table = _data.value?.tableForCategory(category) ?: return null
        val entry = table.entryOf(score) ?: return null
        return SectionQuery(
            category = category,
            score = score,
            count = entry.count,
            cumulative = entry.cumulative,
            percentile = table.percentileOf(entry),
            total = table.total,
        )
    }
}

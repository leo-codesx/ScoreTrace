package com.fenji.scoretrace.ui.screen.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.model.Major
import com.fenji.scoretrace.data.model.MajorTreeData
import com.fenji.scoretrace.data.repository.MajorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MajorSubGroup(
    val name: String,
    val majors: List<Major>,
)

data class MajorGroup(
    val categoryName: String,
    val total: Int,
    val subGroups: List<MajorSubGroup>,
)

/**
 * 选专业：严格读取 assets/majors_v2.json 的 requiredSubjects 字段。
 * 该字段由 tools/fetch_major_requirements.py 从掌上高考接口写入；
 * null 表示未收录，按“可报”展示并在 UI 提示，不再用关键词猜测。
 */
@HiltViewModel
class MajorExplorerViewModel @Inject constructor(
    private val majorRepository: MajorRepository,
) : ViewModel() {

    private val _data = MutableStateFlow<MajorTreeData?>(null)
    val data: StateFlow<MajorTreeData?> = _data.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { majorRepository.loadMajorTree() }
                .onSuccess { _data.value = it }
                .onFailure { _data.value = null }
            _loading.value = false
        }
    }

    fun filter(firstChoice: String, electives: Set<String>): List<MajorGroup> {
        val tree = _data.value ?: return emptyList()
        return tree.categories.mapNotNull { category ->
            val subGroups = category.subCategories.mapNotNull { sub ->
                val majors = sub.majors.filter { major ->
                    matches(major, firstChoice, electives)
                }
                if (majors.isEmpty()) null else MajorSubGroup(name = sub.name, majors = majors)
            }
            if (subGroups.isEmpty()) {
                null
            } else {
                MajorGroup(
                    categoryName = category.name,
                    total = subGroups.sumOf { it.majors.size },
                    subGroups = subGroups,
                )
            }
        }
    }

    private fun matches(
        major: Major,
        firstChoice: String,
        electives: Set<String>,
    ): Boolean {
        val requirements = major.requiredSubjects ?: return true
        if ("物理" in requirements && firstChoice != "物理") return false
        if ("历史" in requirements && firstChoice != "历史") return false
        if ("物理" in requirements && "历史" in requirements) return false

        val electiveRequirements = requirements.filter { it in ELECTIVES }
        if (electiveRequirements.size > 2) return false
        return electiveRequirements.all { it in electives }
    }

    private companion object {
        val ELECTIVES = listOf("化学", "生物", "政治", "地理")
    }
}

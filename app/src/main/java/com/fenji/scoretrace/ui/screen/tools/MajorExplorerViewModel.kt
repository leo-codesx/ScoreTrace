package com.fenji.scoretrace.ui.screen.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scoretrace.data.model.Major
import com.fenji.scoretrace.data.model.MajorCategory
import com.fenji.scoretrace.data.model.MajorSubCategory
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
 * 选专业：按 3+1+2 选科组合筛选 assets/majors_v2.json。
 *
 * 专业目录本身不含选科要求，这里按学科门类 + 专业名称做保守估算：
 * 工学/理学/医学/农学/交叉学科默认要求物理；历史学要求历史；
 * 名称含化学/材料/医学/生物等关键词的再要求对应再选科目。
 * 结果仅用于筛选参考，具体以院校招生专业目录为准。
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
                    matches(category, sub, major, firstChoice, electives)
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
        category: MajorCategory,
        sub: MajorSubCategory,
        major: Major,
        firstChoice: String,
        electives: Set<String>,
    ): Boolean {
        val text = "${major.name} ${sub.name}"
        val requirements = requirementsOf(category.name, text)
        if ("物理" in requirements && firstChoice != "物理") return false
        if ("历史" in requirements && firstChoice != "历史") return false
        if ("物理" in requirements && "历史" in requirements) return false

        val electiveRequirements = requirements.filter { it in ELECTIVES }
        if (electiveRequirements.size > 2) return false
        return electiveRequirements.all { it in electives }
    }

    private fun requirementsOf(categoryName: String, text: String): Set<String> {
        val requirements = mutableSetOf<String>()
        when (categoryName) {
            "工学", "理学", "医学", "农学", "交叉学科" -> requirements += "物理"
            "历史学" -> requirements += "历史"
        }

        if (text.containsAny("化学", "化工", "材料", "环境", "药学", "制药", "医学", "临床", "口腔", "预防", "食品", "农学", "林学", "动物", "植物", "生态")) {
            requirements += "化学"
        }
        if (text.containsAny("生物", "医学", "临床", "口腔", "护理", "药学", "农学", "林学", "动物", "植物", "食品", "生态")) {
            requirements += "生物"
        }
        if (text.containsAny("政治", "法学", "公安", "马克思", "社会学", "国际政治", "外交")) {
            requirements += "政治"
        }
        if (text.containsAny("地理", "测绘", "地质", "环境", "城乡规划", "旅游")) {
            requirements += "地理"
        }
        return requirements
    }

    private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it) }

    private companion object {
        val ELECTIVES = listOf("化学", "生物", "政治", "地理")
    }
}

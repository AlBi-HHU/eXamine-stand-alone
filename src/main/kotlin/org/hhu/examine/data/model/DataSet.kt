package org.hhu.examine.data.model

import org.hhu.examine.data.table.Row

const val MODULE_CATEGORY = "Module"
const val OTHER_CATEGORY = "Miscellaneous"

class NetworkNode(override val index: Int) : Row

class NetworkLink(override val index: Int, val source: NetworkNode, val target: NetworkNode) : Row

class NetworkAnnotation(override val index: Int, val nodes: Set<NetworkNode>) : Row

class DataSet(
        val name: String,
        val nodes: DataTable<NetworkNode>,
        val links: DataTable<NetworkLink>,
        val annotations: DataTable<NetworkAnnotation>
) {

    val annotationCategories = annotations.rows.groupBy {
        annotations.categories[it] ?: OTHER_CATEGORY
    }

    val modules = annotationCategories[MODULE_CATEGORY] ?: emptyList()

}

fun emptyDataSet() = DataSet(
        "Empty DataSet",
        emptyDataTable(),
        emptyDataTable(),
        emptyDataTable()
)
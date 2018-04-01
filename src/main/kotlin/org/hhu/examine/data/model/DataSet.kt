package org.hhu.examine.data.model

const val MODULE_CATEGORY = "Module"
const val OTHER_CATEGORY = "Miscellaneous"

class DataSet(
        val name: String,
        override val nodes: DataTable<NetworkNode>,
        override val links: DataTable<NetworkLink>,
        override val annotations: DataTable<NetworkAnnotation>
) : Network {

    override val graph by lazy { networkToGraph(this) }

    val categories = annotations.rows.groupBy {
        annotations.categories[it] ?: OTHER_CATEGORY
    }

    val modules = categories[MODULE_CATEGORY] ?: emptyList()

}

fun emptyDataSet() = DataSet(
        "Empty DataSet",
        emptyDataTable(),
        emptyDataTable(),
        emptyDataTable()
)
package org.cwi.examine.data

import java.io.File

fun listDataSets(directory: String): List<DataSet> = File(directory)
        .listFiles(File::isDirectory)
        .map(::DataSet)
        .sortedBy(DataSet::name)

class DataSet(directory: File) {
    val name: String = directory.name
    val network: Network = NetworkReader(directory).readNetwork()
}
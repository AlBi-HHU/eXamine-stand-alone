package org.hhu.examine.data.csv

import com.opencsv.CSVReader
import org.hhu.examine.data.csv.columnwriter.ColumnWriters
import org.hhu.examine.data.model.*
import org.hhu.examine.data.table.Row
import java.io.File
import java.io.FileReader
import java.lang.IllegalStateException
import java.util.*

const val IDENTIFIER_HEADER = "Identifier"

fun listDataSets(directory: String): List<File> = File(directory)
        .listFiles(File::isDirectory)
        .sortedBy(File::getName)

fun readDataSet(directory: File): DataSet {

    val membershipMap = loadMemberships(listFilesWithPostFix(directory, ".memberships"))

    val (idToNode, nodeTable) = loadDataTable(
            listFilesWithPostFix(directory, ".nodes"),
            { index, _ -> NetworkNode(index) }
    )
    val (idToAnnotation, annotationTable) = loadDataTable(
            listFilesWithPostFix(directory, ".annotations"),
            { index, id ->
                NetworkAnnotation(
                        index,
                        membershipMap.getOrDefault(id, emptySet()).mapNotNull(idToNode::get).toSet()
                )
            }
    )

    val linkTable = loadLinks(listFilesWithPostFix(directory, ".links"), idToNode)

    return DataSet(
            directory.name,
            nodeTable,
            linkTable,
            annotationTable
    )
}

private fun <R : Row> loadDataTable(files: Collection<File>, rowFactory: (Int, String) -> R): Pair<Map<String, R>, DataTable<R>> {

    // Construct identifier index.
    val readers = createReaders(files)
    val fileHeaders = readers.map(CSVReader::readNext)

    val idToIndex = HashMap<String, Int>()
    fileHeaders.zip(readers).forEach { (header, reader) ->
        if (header == null) throw IllegalStateException("Missing file header.")

        val idIndex = header.indexOfFirst { it == IDENTIFIER_HEADER }
        if (idIndex < 0) throw IllegalStateException("Missing identifier column.")

        reader.forEach { line ->
            if (header.size != line.size) throw IllegalStateException("Row and header size mismatch.")

            val id = line[idIndex]
            idToIndex.putIfAbsent(id, idToIndex.size)
        }
    }

    // Allocate table.
    val columnHeaders = fileHeaders
            .filterNotNull()
            .flatMap { it.toList() }
            .distinct()
            .filterNotNull()
    val columnWriters = ColumnWriters(idToIndex.size, columnHeaders)

    // Fill table.
    createReaders(files).forEach { reader ->
        val header = reader.readNext()!!
        val writers = header.map { it?.let(columnWriters.writerMap::get) }

        val idIndex = header.indexOfFirst { it == IDENTIFIER_HEADER }

        reader.forEach { line ->
            val rowIdentifier = line[idIndex]
            val rowIndex = idToIndex[rowIdentifier]!!
            line.forEachIndexed { index, cell -> writers[index]!!.writeValue(rowIndex, cell) }
        }
    }

    val orderedIdentifiers = Array(idToIndex.size, { "" })
    idToIndex.forEach { id, index -> orderedIdentifiers[index] = id }
    val elements = orderedIdentifiers.mapIndexed(rowFactory)

    return Pair(
            idToIndex.mapValues { (id, index) -> elements[index] },
            DataTable(
                    elements,
                    columnWriters.stringWriters.map { Pair(it.identifier, it.column) }.toMap(),
                    columnWriters.numberWriters.map { Pair(it.identifier, it.column) }.toMap(),
                    columnWriters.hrefWriters.map { Pair(it.identifier, it.column) }.toMap()
            )
    )
}

private fun loadMemberships(files: Collection<File>): Map<String, Set<String>> {
    val memberships = HashMap<String, HashSet<String>>()

    createReaders(files).forEach { reader ->
        reader.forEach { line ->
            if (line.size > 1) {
                val lineMembers = memberships.getOrPut(line[0], ::HashSet)
                (1 until line.size).forEach { index -> lineMembers.add(line[index]) }
            }
        }
    }

    return memberships
}

private fun loadLinks(files: Collection<File>, idToNode: Map<String, NetworkNode>): DataTable<NetworkLink> {
    val links = ArrayList<NetworkLink>()

    createReaders(files).forEach { reader ->
        reader.forEach { line ->
            if (line.size == 2) links.add(NetworkLink(links.size, idToNode[line[0]]!!, idToNode[line[1]]!!))
        }
    }

    return DataTable(
            links,
            emptyMap(),
            emptyMap(),
            emptyMap()
    )
}

private fun listFilesWithPostFix(directory: File, postFix: String): List<File> = directory
        .listFiles { file -> file.name.endsWith(postFix) }
        ?.let { Arrays.asList(*it) } ?: emptyList()

private fun createReaders(files: Collection<File>) = files.map { CSVReader(FileReader(it), '\t') }
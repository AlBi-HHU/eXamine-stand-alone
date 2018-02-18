package org.cwi.examine.data

import com.opencsv.CSVReader
import com.opencsv.bean.CsvToBean
import com.opencsv.bean.HeaderColumnNameMappingStrategy
import com.opencsv.bean.HeaderColumnNameTranslateMappingStrategy
import org.jgrapht.Graphs
import org.jgrapht.UndirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.Pseudograph
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.*

/**
 * CSV network loader.
 */
class NetworkReader(private val filePath: String) {

    /**
     * Load network from the csv files that can be found at filePath.
     */
    @Throws(FileNotFoundException::class)
    fun readNetwork(): Network {

        // Nodes.
        val idToNode = HashMap<String, NetworkNode>()
        for (file in resolveFiles(".nodes")) {
            loadNodes(file, idToNode)
        }
        val superGraph = Pseudograph<NetworkNode, DefaultEdge>(DefaultEdge::class.java)
        idToNode.values.forEach { node -> superGraph.addVertex(node) }

        // Id to id links, for both node <-> node and node <-> annotation.
        val idGraph = Pseudograph<String, DefaultEdge>(DefaultEdge::class.java)
        for (file in resolveFiles(".links")) {
            loadLinks(file, idGraph)
        }
        for (file in resolveFiles(".memberships")) {
            loadLinks(file, idGraph)
        }

        // Resolve node <-> node links.
        idGraph.edgeSet()
                .filter { edge -> idToNode.containsKey(idGraph.getEdgeSource(edge)) && idToNode.containsKey(idGraph.getEdgeTarget(edge)) }
                .forEach { edge ->
                    superGraph.addEdge(
                            idToNode[idGraph.getEdgeSource(edge)],
                            idToNode[idGraph.getEdgeTarget(edge)])
                }

        // Annotations and categories.
        val idToAnnotation = HashMap<String, NetworkAnnotation>()
        val categoryToAnnotations = HashMap<String, MutableList<NetworkAnnotation>>()
        for (file in resolveFiles(".annotations")) {
            loadAnnotations(file, idToNode, idGraph, idToAnnotation, categoryToAnnotations)
        }

        // Categories.
        val categories = ArrayList<NetworkCategory>()
        categoryToAnnotations.forEach { id, hAnnotations -> categories.add(NetworkCategory(id, hAnnotations)) }

        return Network(superGraph, categories)
    }

    @Throws(FileNotFoundException::class)
    private fun loadNodes(file: File, idToNode: MutableMap<String, NetworkNode>) {

        val nodeColumns = HashMap<String, String>()
        nodeColumns.put("Identifier", "identifier")
        nodeColumns.put("Symbol", "name")
        nodeColumns.put("URL", "url")
        nodeColumns.put("Score", "score")

        val nodeEntryBeans = csvToBean(file, NodeEntry::class.java, nodeColumns)

        val graphNodes = nodeEntryBeans.map { nodeEntry ->
            NetworkNode(
                    nodeEntry.identifier!!,
                    nodeEntry.name!!,
                    nodeEntry.url!!,
                    nodeEntry.score ?: .0)
        }

        mapIdToElement(graphNodes, idToNode)
    }

    @Throws(FileNotFoundException::class)
    private fun loadLinks(linkFile: File, idGraph: UndirectedGraph<String, DefaultEdge>) {

        val csvReader = CSVReader(FileReader(linkFile), '\t')
        csvReader.forEach { ids ->
            idGraph.addVertex(ids[0])

            for (i in 1 until ids.size) {
                idGraph.addVertex(ids[i])
                idGraph.addEdge(ids[0], ids[i])
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun loadAnnotations(file: File,
                                idToNode: Map<String, NetworkNode>,
                                idGraph: UndirectedGraph<String, DefaultEdge>,
                                idToAnnotation: MutableMap<String, NetworkAnnotation>,
                                categoryToAnnotations: MutableMap<String, MutableList<NetworkAnnotation>>) {

        val annotationColumns = HashMap<String, String>()
        annotationColumns.put("Identifier", "identifier")
        annotationColumns.put("Symbol", "name")
        annotationColumns.put("URL", "url")
        annotationColumns.put("Score", "score")
        annotationColumns.put("Category", "category")

        val annotationEntries = csvToBean(file, AnnotationEntry::class.java, annotationColumns)

        // Category <-> annotationEntries.
        annotationEntries.forEach { annotationEntry ->

            val members: Set<NetworkNode> =
                    if (idGraph.containsVertex(annotationEntry.identifier))
                        Graphs.neighborListOf(idGraph, annotationEntry.identifier)
                                .mapNotNull(idToNode::get)
                                .toSet()
                    else
                        emptySet()

            val annotation = NetworkAnnotation(
                    annotationEntry.identifier!!,
                    annotationEntry.name!!,
                    annotationEntry.url!!,
                    annotationEntry.score ?: .0,
                    members)

            annotationEntry.identifier?.let { idToAnnotation.put(it, annotation) }
            annotationEntry.category?.let {
                categoryToAnnotations
                        .computeIfAbsent(it, { _ -> ArrayList() })
                        .add(annotation)
            }
        }
    }

    private fun <T : NetworkElement> mapIdToElement(elements: List<T>, idToElement: MutableMap<String, T>) {
        elements.forEach { e -> idToElement.put(e.identifier, e) }
    }

    private fun resolveFiles(postFix: String): List<File> {

        val dataRoot = File(filePath)
        val files = dataRoot.listFiles { file -> file.name.endsWith(postFix) }
        return Arrays.asList(*files!!)
    }

    @Throws(FileNotFoundException::class)
    private fun <T> csvToBean(csvFile: File, strategy: HeaderColumnNameMappingStrategy<T>): List<T> {

        val csvToBean = CsvToBean<T>()
        val csvReader = CSVReader(FileReader(csvFile), '\t')
        return csvToBean.parse(strategy, csvReader)
    }

    @Throws(FileNotFoundException::class)
    private fun <T> csvToBean(
            csvFile: File,
            classToMap: Class<T>,
            columnToBeanNames: Map<String, String>): List<T> {

        val strategy = HeaderColumnNameTranslateMappingStrategy<T>()
        strategy.type = classToMap
        strategy.columnMapping = columnToBeanNames
        return csvToBean(csvFile, strategy)
    }

}
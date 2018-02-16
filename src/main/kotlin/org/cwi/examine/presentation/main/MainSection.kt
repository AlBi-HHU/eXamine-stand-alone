package org.cwi.examine.presentation.main

import org.cwi.examine.model.Network
import org.cwi.examine.presentation.Section
import java.util.function.Consumer

class MainSection(private val superNetwork: Network) : Section {

    override val view: MainView = MainView()
    private val viewModel: MainViewModel = MainViewModel()

    init {
        bindViewModel()
        initializeActiveNetwork()
    }

    private fun bindViewModel() {

        val annotationTabs = view.annotationOverview
        annotationTabs.categoriesProperty().bindContent(viewModel.categories)
        annotationTabs.annotationColorsProperty().bind(viewModel.annotationColorProperty())
        annotationTabs.highlightedAnnotationsProperty().bind(viewModel.highlightedAnnotationsProperty())
        annotationTabs.onToggleAnnotationProperty().set(Consumer { viewModel.toggleAnnotation(it) })
        annotationTabs.onHighlightAnnotationsProperty().set(Consumer { viewModel.highlightAnnotations(it) })

        val nodeLinkContourView = view.nodeLinkContourView
        nodeLinkContourView.networkProperty().bind(viewModel.activeNetworkProperty())
        nodeLinkContourView.selectedAnnotationsProperty().bind(viewModel.selectedAnnotationsProperty())
        nodeLinkContourView.annotationWeightsProperty().bind(viewModel.annotationWeightsProperty())
        nodeLinkContourView.annotationColorsProperty().bind(viewModel.annotationColorProperty())
        nodeLinkContourView.highlightedNodesProperty().bind(viewModel.highlightedNodesProperty())
        nodeLinkContourView.highlightedLinksProperty().bind(viewModel.highlightedLinksProperty())
    }

    /**
     * Initialize active network that is visualized. For now it is the union of all known modules.
     */
    private fun initializeActiveNetwork() {

        val moduleNetwork = superNetwork.induce(superNetwork.modules)

        viewModel.activateNetwork(moduleNetwork)
    }

}

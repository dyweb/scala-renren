/*
Copyright 2008-2010 Gephi
Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
*/
package utils

import java.awt.Color
import java.io.File
import java.io.IOException
import java.awt.BorderLayout
import javax.swing.JFrame
import org.gephi.data.attributes.api.AttributeColumn
import org.gephi.data.attributes.api.AttributeController
import org.gephi.data.attributes.api.AttributeModel
import org.gephi.filters.api.FilterController
import org.gephi.filters.api.Query
import org.gephi.filters.api.Range
import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter
import org.gephi.graph.api._
import org.gephi.io.exporter.api.ExportController
import org.gephi.io.importer.api.Container
import org.gephi.io.importer.api.EdgeDefault
import org.gephi.io.importer.api.ImportController
import org.gephi.io.processor.plugin.DefaultProcessor
import org.gephi.layout.plugin.force.StepDisplacement
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout
import org.gephi.preview.types.EdgeColor
import org.gephi.project.api.ProjectController
import org.gephi.project.api.Workspace
import org.gephi.ranking.api.Ranking
import org.gephi.ranking.api.RankingController
import org.gephi.ranking.api.Transformer
import org.gephi.ranking.plugin.transformer.AbstractColorTransformer
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer
import org.gephi.statistics.plugin.GraphDistance
import org.gephi.statistics.plugin.Modularity
import org.openide.util.Lookup
import org.gephi.io.exporter.preview._
import org.gephi.io.importer.api.Container
import org.gephi.io.importer.api.ImportController
import org.gephi.io.processor.plugin.DefaultProcessor
import org.gephi.preview.api._
import org.gephi.preview.types.DependantOriginalColor
import processing.core.PApplet

import scala.collection.{ mutable, immutable, generic }
import scala.collection.mutable.{ HashMap }
import scala.util.Random

import renren.Friend
import utils.Reporters._

/**
 * This demo shows several actions done with the toolkit, aiming to do a complete chain,
 * from data import to results.
 * <p>
 * This demo shows the following steps:
 * <ul><li>Create a project and a workspace, it is mandatory.</li>
 * <li>Import the <code>polblogs.gml</code> graph file in an import container.</li>
 * <li>Append the container to the main graph structure.</li>
 * <li>Filter the graph, using <code>DegreeFilter</code>.</li>
 * <li>Run layout manually.</li>
 * <li>Compute graph distance metrics.</li>
 * <li>Rank color by degree values.</li>
 * <li>Rank size by centrality values.</li>
 * <li>Configure preview to display labels and mutual edges differently.</li>
 * <li>Export graph as PDF.</li></ul>
 *
 * @author Mathieu Bastian
 */
class Grapher(val network: HashMap[Friend, List[Friend]], reporter: Reporter) {
  val minSize = 10
  val maxSize = 50
  val height = 1080
  val width = 1920
  val colorMap: HashMap[Integer, Color] = new HashMap()

  def generateRandomColor(mix: Color): Color = {
    var random = new Random()
    var red = random.nextInt(256)
    var green = random.nextInt(256)
    var blue = random.nextInt(256)

    // mix the color
    red = (red + mix.getRed()) / 2
    green = (green + mix.getGreen()) / 2
    blue = (blue + mix.getBlue()) / 2

    var color = new Color(red, green, blue)
    return color
  }

  def fillMap() = {
    var mix = new Color(0, 0, 0)
    for (i <- 0 to 100) {
      colorMap(i) = generateRandomColor(mix)
      mix = colorMap(i)
    }
  }

  fillMap()

  def rgbInt2Float(a: Integer): Float = {
    return a.toFloat / 256f
  }

  def script(): Unit = {
    reporter.info("Begin to draw")
    //Init a project - and therefore a workspace
    var pc = Lookup.getDefault().lookup(classOf[ProjectController])
    pc.newProject()
    var workspace = pc.getCurrentWorkspace()

    //Get models and controllers for this new workspace - will be useful later
    var attributeModel = Lookup.getDefault().lookup(classOf[AttributeController]).getModel()
    var graphModel = Lookup.getDefault().lookup(classOf[GraphController]).getModel()
    var previewController = Lookup.getDefault().lookup(classOf[PreviewController])
    var model = previewController.getModel()
    var filterController = Lookup.getDefault().lookup(classOf[FilterController])
    var rankingController = Lookup.getDefault().lookup(classOf[RankingController])

    // Import the data
    var directedGraph = graphModel.getDirectedGraph()
    var nodes: HashMap[Friend, Node] = new HashMap()
    // println(network)
    for ((k, _) <- network) {
      var n0 = graphModel.factory().newNode(k.uid)
      n0.getNodeData().setLabel(k.name)
      nodes(k) = n0
      directedGraph.addNode(n0)
    }
    for ((k, value) <- network) {
      for (ele <- value) {
        var e0 = graphModel.factory().newEdge(nodes(k), nodes(ele), 1f, true)
        directedGraph.addEdge(e0)
      }
    }

    // System.out.println("Nodes: "+directedGraph.getNodeCount()+" Edges: "+directedGraph.getEdgeCount())

    //Filter      
    var degreeFilter = new DegreeRangeFilter()
    degreeFilter.init(directedGraph)
    degreeFilter.setRange(new Range(1, Integer.MAX_VALUE)) //Remove nodes with degree < 30
    var query = filterController.createQuery(degreeFilter)
    var view = filterController.filter(query)
    graphModel.setVisibleView(view) //Set the filter result as the visible view

    //See visible graph stats
    var graphVisible = graphModel.getUndirectedGraphVisible()
    reporter.info("---The Graph Info---")
    reporter.info("Nodes: " + graphVisible.getNodeCount())
    reporter.info("Edges: " + graphVisible.getEdgeCount())

    //Get Centrality
    var distance = new GraphDistance()
    distance.setDirected(true)
    distance.execute(graphModel, attributeModel)

    //Rank color by Degree
    // var degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING)
    // var colorTransformer = rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR).asInstanceOf[AbstractColorTransformer[Ranking[AttributeColumn]]]
    // colorTransformer.setColors(Array(new Color(0xFEF0D9), new Color(0xB30000)))
    // rankingController.transform(degreeRanking,colorTransformer)

    //Rank size by centrality
    var centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS)
    var centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId)
    var sizeTransformer = rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE).asInstanceOf[AbstractSizeTransformer[Ranking[AttributeColumn]]]
    sizeTransformer.setMinSize(minSize)
    sizeTransformer.setMaxSize(maxSize)
    rankingController.transform(centralityRanking, sizeTransformer)

    // Rank color by modularity
    var modularity = new Modularity
    modularity.execute(graphModel, attributeModel)
    var modularityColumn = attributeModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS)
    for (node <- graphModel.getDirectedGraph.getNodes.toArray) {
      var rgb = node.getNodeData().getAttributes().getValue(modularityColumn.getIndex()).asInstanceOf[Int]
      var colorBuf = colorMap(rgb)
      node.getNodeData().setColor(rgbInt2Float(colorBuf.getRed), rgbInt2Float(colorBuf.getGreen), rgbInt2Float(colorBuf.getBlue))
    }

    // use layout
    reporter.info("Run layout algorithm")
    val layout = new ForceAtlasLayout(null)
    layout.setGraphModel(graphModel)
    layout.resetPropertiesValues()
    layout.initAlgo()

    var i = 0
    while (i < 1000 && layout.canAlgo()) {
      i = i + 1
      layout.goAlgo()
    }
    layout.endAlgo()
    reporter.info("Layout end")

    // Test: get all the AttributeColumn
    // for (col <- attributeModel.getNodeTable().getColumns()) {
    // println(col)
    // }

    //Preview
    model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, true)
    model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8))
    model.getProperties().putValue(PreviewProperty.EDGE_CURVED, false)
    previewController.refreshPreview()

    //New Processing target, get the PApplet
    var target = previewController.getRenderTarget(RenderTarget.PROCESSING_TARGET).asInstanceOf[ProcessingTarget]
    var applet = target.getApplet()
    applet.init()

    //Refresh the preview and reset the zoom
    previewController.render(target)
    target.refresh()
    target.resetZoom()

    //Add the applet to a JFrame and display
    reporter.info("Build JFrame")
    var frame = new JFrame("Renren Friend Relationship")
    frame.setLayout(new BorderLayout())

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.add(applet, BorderLayout.CENTER)

    frame.pack()
    frame.setVisible(true)
    var ec = Lookup.getDefault().lookup(classOf[ExportController])
    val pe = new PNGExporter
    ec.exportFile(new File("headless_simple.png"), pe)
    val ps = new SVGExporter
    ec.exportFile(new File("headless_simple.svg"), ps)

  }
}

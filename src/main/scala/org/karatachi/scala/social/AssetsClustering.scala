package org.karatachi.scala.social

import java.awt.Dimension

import scala.Array.canBuildFrom

import org.apache.commons.collections15.Transformer
import org.karatachi.scala.social.DataSource.Finance
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.session.Database.threadLocalSession

import edu.uci.ics.jung.algorithms.layout.KKLayout
import edu.uci.ics.jung.graph.DelegateTree
import edu.uci.ics.jung.visualization.renderers.Renderer
import edu.uci.ics.jung.visualization.BasicVisualizationServer
import javax.swing.JFrame

object AssetsClustering extends App {

  DataSource.db withSession {
    val companies = DataSource.nikkei225

    val title = Account.totalassets.id

    val years = (2001 to 2011).toArray

    val assets = {
      for ((companyId, _, _, _) <- companies) yield {
        val q = for (s <- Finance if s.companyId === companyId && s.title === title) yield s.year ~ s.value
        val r = q.toMap.withDefaultValue(Double.NaN)
        years.map(r(_))
      }
    }.toArray

    val tree = HierarchialClustering.clustering(assets, Distance.pearson)

    val g = new DelegateTree[HierarchialClustering.Tree, String]()
    g.addVertex(tree);
    HierarchialClustering.graph(tree, g)

    val transformer = new Transformer[HierarchialClustering.Tree, String]() {
      override def transform(i: HierarchialClustering.Tree): String = {
        if (i.id >= 0) {
          companies(i.id)._4
        } else {
          ""
        }
      }
    }

    val layout = new KKLayout[HierarchialClustering.Tree, String](g);
    //val layout = new SpringLayout(g)
    layout.setSize(new Dimension(600, 600))

    val panel = new BasicVisualizationServer[HierarchialClustering.Tree, String](layout, layout.getSize())
    panel.getRenderContext().setVertexLabelTransformer(transformer)
    panel.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR)

    val frame = new JFrame()
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.getContentPane().add(panel)
    frame.pack()
    frame.setVisible(true)
  }
}

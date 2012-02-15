package org.karatachi.scala.social

import java.awt.Dimension
import java.sql.Date

import scala.Array.canBuildFrom

import org.apache.commons.collections15.Transformer
import org.joda.time.LocalDate
import org.scalaquery.ql.TypeMapper.DateTypeMapper
import org.scalaquery.ql.TypeMapper.DoubleTypeMapper
import org.scalaquery.ql.TypeMapper.StringTypeMapper
import org.scalaquery.ql.extended.MySQLDriver.Implicit.baseColumnToColumnOps
import org.scalaquery.ql.extended.MySQLDriver.Implicit.queryToQueryInvoker
import org.scalaquery.ql.extended.MySQLDriver.Implicit.tableToQuery
import org.scalaquery.ql.extended.MySQLDriver.Implicit.valueToConstColumn
import org.scalaquery.ql.extended.{ExtendedTable => Table}
import org.scalaquery.ql.Query
import org.scalaquery.session.Database.threadLocalSession

import edu.uci.ics.jung.algorithms.layout.TreeLayout
import edu.uci.ics.jung.graph.DelegateTree
import edu.uci.ics.jung.visualization.renderers.Renderer
import edu.uci.ics.jung.visualization.BasicVisualizationServer
import javax.swing.JFrame

object Bootstrap extends App {

  def toSql(localDate: LocalDate) = new Date(localDate.toDate.getTime)

  val start = toSql(new LocalDate(2011, 2, 1))
  val end = toSql(new LocalDate(2011, 12, 1).minusDays(1))

  DataSource.db withSession {
    val companies = {
      val q = for (c <- Company if c.countryId === "JPN") yield c.*
      q.list.filter(c => Nikkei225.list.contains(c._3))
    }

    val dates = {
      val q = for (
        s <- Share if s.companyId === "JP3585800000" && s.date.between(start, end);
        _ <- Query orderBy s.date.asc
      ) yield s.date
      q.list.toArray
    }

    val shares = {
      for ((companyId, _, _, _) <- companies) yield {
        val q = for (s <- Share if s.companyId === companyId && s.date.between(start, end)) yield s.date ~ s.change
        val r = q.toMap.withDefaultValue(0.0)
        dates.map(r(_))
      }
    }.toArray

    val tree = HierarchialClustering.clustering(shares, Distance.pearson)

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
    /*
    val out = new BufferedWriter(new FileWriter("/Users/chimera/test.xml"))
    val w = new GraphMLWriter[HierarchialClustering.Tree, String]()
    w.setVertexDescriptions(transformer)
    w.setVertexIDs(new Transformer[HierarchialClustering.Tree, String]() {
      override def transform(i: HierarchialClustering.Tree): String = {
        i.id.toString
      }
    })
    w.save(g, out)
    out.close
    */

    val layout = new TreeLayout[HierarchialClustering.Tree, String](g);
    //val layout = new SpringLayout(g)
    //layout.setSize(new Dimension(1200, 1200))

    val panel = new BasicVisualizationServer[HierarchialClustering.Tree, String](layout, layout.getSize())
    panel.getRenderContext().setVertexLabelTransformer(transformer)
    panel.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR)

    val frame = new JFrame("Simple Graph View")
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.getContentPane().add(panel)
    frame.pack()
    frame.setVisible(true)

    /*
    val clusters = KMeansClustering.clustering(shares, Distance.pearson, 20, 100)
    for (cluster <- clusters) 
      println(cluster.map(companies(_)._4).mkString(","))
    */
  }

  object Company extends Table[(String, String, String, String)]("company") {
    def companyId = column[String]("company_id")
    def countryId = column[String]("country_id")
    def symbolId = column[String]("symbol_id")
    def name = column[String]("name")
    def * = companyId ~ countryId ~ symbolId ~ name
  }

  object Share extends Table[(String, Date, Double)]("share") {
    def companyId = column[String]("company_id")
    def date = column[Date]("date")
    def change = column[Double]("change")
    def * = companyId ~ date ~ change
  }
}

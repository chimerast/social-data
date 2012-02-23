package org.karatachi.scala.social

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import scala.Array.canBuildFrom
import scala.io.Source

import org.karatachi.scala.social.DataSource.Finance
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.session.Database.threadLocalSession

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

    Dendrogram(tree, companies.map(_._4))
  }
}

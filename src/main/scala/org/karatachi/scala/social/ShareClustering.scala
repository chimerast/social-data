package org.karatachi.scala.social

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import scala.Array.canBuildFrom
import scala.io.Source

import org.joda.time.LocalDate
import org.karatachi.scala.social.DataSource.Share
import org.karatachi.scala.social.DataSource.toSql
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.ql.Query
import org.scalaquery.session.Database.threadLocalSession

object ShareClustering extends App {
  val start = toSql(new LocalDate(2011, 1, 1))
  val end = toSql(new LocalDate(2011, 12, 1).minusDays(1))

  DataSource.db withSession {
    val companies = DataSource.nikkei225

    val title = Account.totalassets.id

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

    Dendrogram(tree, companies.map(_._4))

    val json = HierarchialClustering.json2(tree, companies(_)._4)

    val html = Source.fromInputStream(getClass.getResourceAsStream("dendrogram2.html")).getLines.mkString("\n")

    val file = File.createTempFile("tree", ".html")
    val out = new BufferedWriter(new FileWriter(file))
    out.write(html.format(json))
    out.close()

    Runtime.getRuntime().exec("open " + file.getAbsolutePath())
  }
}

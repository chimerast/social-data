package org.karatachi.scala.social

import java.sql.Date
import org.scalaquery.ql.TypeMapper.DateTypeMapper
import org.scalaquery.ql.TypeMapper.DoubleTypeMapper
import org.scalaquery.ql.TypeMapper.StringTypeMapper
import org.scalaquery.ql.extended.MySQLDriver.Implicit.baseColumnToColumnOps
import org.scalaquery.ql.extended.MySQLDriver.Implicit.queryToQueryInvoker
import org.scalaquery.ql.extended.MySQLDriver.Implicit.tableToQuery
import org.scalaquery.ql.extended.MySQLDriver.Implicit.valueToConstColumn
import org.scalaquery.ql.extended.{ ExtendedTable => Table }
import org.scalaquery.session.Database.threadLocalSession
import org.scalaquery.ql.Query
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer
import org.joda.time.LocalDate

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
    HierarchialClustering.print(tree, companies(_)._4)

    val clusters = KMeansClustering.clustering(shares, Distance.pearson, 20, 100)

    for (cluster <- clusters) {
      println(cluster.map(companies(_)._4).mkString(","))
    }
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

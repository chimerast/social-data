package org.karatachi.scala.social

import java.sql.Date

import org.joda.time.LocalDate
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.ql.extended.{ExtendedTable => Table}
import org.scalaquery.session._

import com.mongodb.casbah.MongoConnection

object DataSource {
  val db = Database.forURL("jdbc:mysql://localhost/speeda", driver = "com.mysql.jdbc.Driver", user = "root")
  val geocoding = MongoConnection("localhost")("speeda")("geocoding")

  def nikkei225()(implicit session: Session) = {
    val q = for (c <- Company if c.countryId === "JPN") yield c.*
    q.list.filter(c => Nikkei225.list.contains(c._3)).toArray
  }

  def toSql(localDate: LocalDate) = new Date(localDate.toDate.getTime)

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

  object Finance extends Table[(String, Int, Int, Double)]("finance") {
    def companyId = column[String]("company_id")
    def title = column[Int]("title")
    def year = column[Int]("year")
    def value = column[Double]("value")
    def * = companyId ~ title ~ year ~ value
  }
}

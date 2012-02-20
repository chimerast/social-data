package org.karatachi.scala.social

import scala.Array.canBuildFrom
import scala.io.Source

import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.ql.extended.{ ExtendedTable => Table }
import org.scalaquery.session.Database.threadLocalSession

object FinanceLoader extends App {

  val accounts = List(110004000, 210000100, 210000900, 210002500, 510300100, 510300800)

  accounts.foreach(load)

  def load(title: Int): Unit = {
    val path = "/Users/chimera/Downloads/speeda/%09d.csv".format(title)

    DataSource.db withSession {
      val source = Source.fromFile(path, "UTF-8")
      try {
        val lines = source.getLines
        val years = lines.next.split(",").drop(1).map(java.lang.Integer.parseInt(_))

        for (line <- lines) {
          val datas = line.split(",")

          val companyId = datas(0)
          for ((year, data) <- years zip datas.drop(1)) {
            try {
              data match {
                case "null" =>
                  ;
                case data =>
                  val value = java.lang.Double.parseDouble(data)

                  Finance.insert(companyId, title, year, value)
              }
            } catch {
              case e: NumberFormatException =>
                ;
            }
          }
        }
      } finally {
        source.close
      }
    }
  }

  object Finance extends Table[(String, Int, Int, Double)]("finance") {
    def companyId = column[String]("company_id")
    def title = column[Int]("title")
    def year = column[Int]("year")
    def value = column[Double]("value")
    def * = companyId ~ title ~ year ~ value
  }
}

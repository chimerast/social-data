package org.karatachi.scala.social

case class Account(id: Int, name: String)

object Account {
  val totalassets = Account(110004000, "資産合計")
  val revenue = Account(210000100, "売上高合計")
  val operatingincome = Account(210000900, "営業利益")
  val netincome = Account(210002500, "当期純利益")
  val operatingmargin = Account(510300100, "売上高営業利益率")
  val netmargin = Account(510300800, "売上高当期利益率")

  val accounts = List(
    totalassets,
    revenue,
    operatingincome,
    netincome,
    operatingmargin,
    netmargin)
}

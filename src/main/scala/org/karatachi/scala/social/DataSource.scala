package org.karatachi.scala.social

import org.scalaquery.session._

import com.mongodb.casbah.MongoConnection

object DataSource {
  val db = Database.forURL("jdbc:mysql://localhost/speeda", driver = "com.mysql.jdbc.Driver", user = "root")
  val geocoding = MongoConnection("localhost")("speeda")("geocoding")
}

package org.karatachi.scala.social

import com.mongodb.casbah.MongoConnection
import org.scalaquery.session.Database

object DataSource {
  val db = Database.forURL("jdbc:mysql://localhost/speeda", driver = "com.mysql.jdbc.Driver", user = "root")
  val geocoding = MongoConnection("localhost")("speeda")("geocoding")
}

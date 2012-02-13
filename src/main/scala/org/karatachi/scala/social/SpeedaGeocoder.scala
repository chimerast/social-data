package org.karatachi.scala.social

import org.scalaquery.session._
import org.scalaquery.session.Database.threadLocalSession
import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ ExtendedTable => Table }
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import com.google.code.geocoder.Geocoder
import com.google.code.geocoder.GeocoderRequestBuilder
import com.google.code.geocoder.model.GeocoderStatus
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import com.mongodb.util.JSON

object SpeedaGeoCoder extends App {
  val db = Database.forURL("jdbc:mysql://localhost/speeda", driver = "com.mysql.jdbc.Driver", user = "root")
  val geocoding = MongoConnection("localhost")("speeda")("geocoding")

  val geocoder = new Geocoder()
  val gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

  db withSession {
    val q = for (c <- Company) yield c.*
    for (
      (companyId, address) <- q.list;
      if geocoding.findOne(MongoDBObject("company_id" -> companyId)).isEmpty;
      address <- address
    ) {
      val request = new GeocoderRequestBuilder().setAddress(address).setLanguage("en").getGeocoderRequest()
      val response = geocoder.geocode(request)

      println(companyId)

      if (response.getStatus() == GeocoderStatus.OK) {
        // 住所を解決できた場合はJSONを丸ごとresultとして入れる
        val results = response.getResults()
        geocoding.save(
          MongoDBObject("company_id" -> companyId, "result" -> JSON.parse(gson.toJson(results.get(0)))))
      } else if (response.getStatus() == GeocoderStatus.ZERO_RESULTS) {
        // 住所を解決できなかった場合はcompany_idだけを入れる
        geocoding.save(
          MongoDBObject("company_id" -> companyId))
      } else {
        throw new Exception(response.getStatus().toString())
      }

      // Googleの制限を回避するためにwaitをいれる
      Thread.sleep(200)
    }
  }
}

object Company extends Table[(String, Option[String])]("company") {
  def companyId = column[String]("company_id", O PrimaryKey)
  def address = column[Option[String]]("address")
  def * = companyId ~ address
}

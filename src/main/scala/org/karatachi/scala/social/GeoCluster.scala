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
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import de.micromata.opengis.kml.v_2_2_0.Placemark
import de.micromata.opengis.kml.v_2_2_0.KmlFactory

object GeoCluster extends App {
  case class Company(name: String, lat: Double, lng: Double)

  DataSource.db withSession {
    val companies = DataSource.global

    val locations = {
      for ((companyId, _, _, name) <- companies) yield {
        for (
          root <- DataSource.geocoding.findOne(MongoDBObject("company_id" -> companyId), MongoDBObject("result.geometry.location" -> 1));
          result <- Option(root.get("result").asInstanceOf[DBObject]);
          geometry <- Option(result.get("geometry").asInstanceOf[DBObject]);
          location <- Option(geometry.get("location").asInstanceOf[DBObject]);
          lat <- Option(location.get("lat").asInstanceOf[Double]);
          lng <- Option(location.get("lng").asInstanceOf[Double])
        ) yield {
          (Company(name, lat, lng), Array(lat, lng))
        }
      }
    }.flatten

    val results = KMeansClustering.clustering(locations.map(_._2), Distance.euclidean, 8, 100)

    val kml = KmlFactory.createKml
    val document = kml.createAndSetDocument
      .withName("Speeda Companies")

    document
      .createAndAddStyle().withId("style0")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png")
    document
      .createAndAddStyle().withId("style1")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png")
    document
      .createAndAddStyle().withId("style2")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/grn-pushpin.png")
    document
      .createAndAddStyle().withId("style3")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/ltblu-pushpin.png")
    document
      .createAndAddStyle().withId("style4")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/pink-pushpin.png")
    document
      .createAndAddStyle().withId("style5")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/purple-pushpin.png")
    document
      .createAndAddStyle().withId("style6")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png")
    document
      .createAndAddStyle().withId("style7")
      .createAndSetIconStyle().withScale(1.0)
      .createAndSetIcon().withHref("http://maps.google.com/mapfiles/kml/pushpin/wht-pushpin.png")

    for ((result, i) <- results.zipWithIndex) {
      for (j <- result._1) {
        val company = locations(j)._1
        val placemark = document.createAndAddPlacemark()
          .withName(company.name)
          .withStyleUrl("#style" + i)
        placemark.createAndSetPoint()
          .addToCoordinates(company.lng, company.lat)
      }
    }

    kml.marshal(new File("/Users/chimera/global.kml"))
  }
}

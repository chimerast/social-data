package org.karatachi.scala.social

import scala.collection.mutable.Set
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import org.karatachi.scala.social.DataSource.Company
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.MySQLDriver.Implicit._
import org.scalaquery.session.Database.threadLocalSession
import scala.util.parsing.json.JSONObject
import scala.util.parsing.json.JSONArray
import scala.io.Source
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

object CountryTree extends App {

  val map = new HashMap[String, Set[String]] with MultiMap[String, String]

  val json = DataSource.db withSession {
    val q = for (c <- Company) yield c.countryId ~ c.name
    q.foreach(c => map.add(c._1, c._2))

    val items = for ((country, companies) <- map) yield {
      val children = for (company <- companies) yield { new JSONObject(Map("name" -> company)) }
      new JSONObject(Map("name" -> country, "children" -> new JSONArray(children.toList)))
    }
    new JSONObject(Map("label" -> "name", "items" -> new JSONArray(items.toList.sortBy(_.obj("name").asInstanceOf[String]))))
  }

  val file = File.createTempFile("dojo", ".html")
  val out = new BufferedWriter(new FileWriter(file))
  out.write(html.format(json))
  out.close()

  Runtime.getRuntime().exec("open " + file.getAbsolutePath())

  def html(): String = """
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Clustered Contacts</title>
<link rel="stylesheet"
  href="http://ajax.googleapis.com/ajax/libs/dojo/1.5/dojo/resources/dojo.css">
<link rel="stylesheet"
  href="http://ajax.googleapis.com/ajax/libs/dojo/1.5/dijit/themes/claro/claro.css">
<script src="http://ajax.googleapis.com/ajax/libs/dojo/1.5/dojo/dojo.xd.js"
  type="text/javascript" djConfig="parseOnLoad:true"></script>
<script language="JavaScript" type="text/javascript">
  dojo.require("dojo.data.ItemFileReadStore");
  dojo.require("dijit.Tree");
  dojo.require("dojo.parser");
</script>
<script language="JavaScript" type="text/javascript">
  var data = %s; //substituted by Python by XXX
</script>
</head>
<body class="claro">
  <div dojoType="dojo.data.ItemFileReadStore" jsId="jobsStore" data="data"></div>

  <div dojoType="dijit.Tree" id="mytree" store="jobsStore"
    label="Clustered Contacts" openOnClick="true"></div>
</body>
</html>
"""
}

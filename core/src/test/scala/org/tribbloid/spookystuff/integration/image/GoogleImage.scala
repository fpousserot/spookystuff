package org.tribbloid.spookystuff.integration.image

import org.tribbloid.spookystuff.entity.client._
import org.tribbloid.spookystuff.integration.TestCore

/**
 * Created by peng on 10/06/14.
 */
object GoogleImage extends TestCore {

  import spooky._

  override def doMain() = {

    ((noInput
      +> Visit("http://www.utexas.edu/world/univ/alpha/")
      !=!())
      .sliceJoin("div.box2 a")(limit = 10)
      .extract(
        "name" -> (_.text1("*"))
      )
      .repartition(10)
      +> Visit("http://images.google.com/")
      +> DelayFor("form[action=\"/search\"]")
      +> TextInput("input[name=\"q\"]","Logo #{name}")
      +> Submit("input[name=\"btnG\"]")
      +> DelayFor("div#search")
      !=!())
      .wgetJoin("div#search img","src")(limit = 1)
      .saveContent(pageRow =>
      "file://"+System.getProperty("user.home")+"/spOOky/"+appName+"/images/"+pageRow("name"))
      .extract(
        "path" -> (_.saved)
      )
      .asSchemaRDD()
  }
}
package org.tribbloid.spookystuff.example.encyclopedia

import org.tribbloid.spookystuff.SpookyContext
import org.tribbloid.spookystuff.actions._
import org.tribbloid.spookystuff.dsl._
import org.tribbloid.spookystuff.example.QueryCore

/**
 * Created by peng on 14/06/15.
 */
object DBPedia_Image extends QueryCore {

  def imgPages(spooky: SpookyContext, cls: String, str: String) = spooky.fetch(
    Wget(s"http://lookup.dbpedia.org/api/search/KeywordSearch?QueryClass=$cls&QueryString=$str")
  ).wgetJoin(
      S"Result URI".text,
      failSafe = 2
    ).wgetExplore(
      S"""a[rel^=dbpedia-owl][href*=dbpedia],a[rev^=dbpedia-owl][href*=dbpedia]""".distinctBy(_.href).slice(0,8),
      failSafe = 2,
      depthKey = 'depth,
      maxDepth = 2
    ).join(S"h1#title a".text, distinct = true)(
      Visit("http://images.google.com/")
        +> TextInput("input[name=\"q\"]",'A)
        +> Submit("input[name=\"btnG\"]")
    )()

  override def doMain(spooky: SpookyContext) = {

    val str = "Rob Ford"
    val cls = "person"

    imgPages(spooky, cls, str)
      .wgetJoin(S"div#search img".src, maxOrdinal = 1)
      .persist()
      .savePages(
        x"file://${System.getProperty("user.home")}/spooky-example/$appName/${str}_$cls/level_${'depth}_${'name.andMap(v =>v.toString.replaceAll("[^\\w]","_"))}"
      ).select(
        S.saved ~ 'path
      ).toDF()
  }
}
package com.tribbloids.spookystuff.example.forum

import com.tribbloids.spookystuff.SpookyContext
import com.tribbloids.spookystuff.actions._
import com.tribbloids.spookystuff.dsl._
import com.tribbloids.spookystuff.example.QueryCore

/**
 * Created by peng on 20/08/14.
 */
object Imdb extends QueryCore {

  override def doMain(spooky: SpookyContext) = {

    spooky
      .fetch(
        Wget("http://www.imdb.com/chart")
      )
      .flatExtract(S("div#boxoffice tbody tr"))(
        A"tr td.titleColumn".ownText.replaceAll("\"","").trim ~ 'rank,
        A"tr td.titleColumn a".text ~ 'name,
        A("tr td.titleColumn span").text ~ 'year,
        A("tr td.ratingColumn",0).text ~ 'box_weekend,
        A("td.ratingColumn",1).text ~ 'box_gross,
        A("tr td.weeksColumn").text ~ 'weeks
      )
      .wgetJoin(S"tr td.titleColumn a") //go to movie pages, e.g. http://www.imdb.com/title/tt2015381/?ref_=cht_bo_1
      .select(
        S("td#overview-top div.titlePageSprite").text ~ 'score,
        S("td#overview-top span[itemprop=ratingCount]").text ~ 'rating_count,
        S("td#overview-top span[itemprop=reviewCount]").text ~ 'review_count
      )
      .wgetJoin(S("div#maindetails_quicklinks a:contains(Reviews)")) //go to review pages, e.g. http://www.imdb.com/title/tt2015381/reviews?ref_=tt_urv
      .wgetExplore(S"div#tn15content a:has(img[alt~=Next])", depthKey = 'page, optimizer = Narrow) //grab all pages by using the right arrow button.
      .flatExtract(S("div#tn15content div:has(h2)"))(
        A("img[alt]").attr("alt") ~ 'review_rating,
        A("h2").text ~ 'review_title,
        A("small").text ~ 'review_meta
      )
      .wgetJoin(S("a")) //go to reviewers' page, e.g. http://www.imdb.com/user/ur23582121/
      .select(
        S("div.user-profile h1").text ~ 'user_name,
        S("div.user-profile div.timestamp").text ~ 'user_timestamp,
        S("div.user-lists div.see-more").text ~ 'user_post_count,
        S("div.ratings div.see-more").text ~ 'user_rating_count,
        S("div.reviews div.see-more").text ~ 'user_review_count,
        S("div.overall div.histogram-horizontal a").attrs("title") ~ 'user_rating_histogram
      )
      .toDF()
  }
}

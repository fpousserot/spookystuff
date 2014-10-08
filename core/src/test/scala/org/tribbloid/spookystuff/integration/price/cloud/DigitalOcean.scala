package org.tribbloid.spookystuff.integration.price.cloud

import org.tribbloid.spookystuff.entity.client._
import org.tribbloid.spookystuff.integration.TestCore

/**
* Created by peng on 25/08/14.
*/
object DigitalOcean extends TestCore {

  import spooky._

  override def doMain() = {
    (noInput
      +> Visit("https://www.digitalocean.com/pricing/")
      !=!())
      .sliceJoin("div.plan")(indexKey = "row")
      .extract(
        "Memory" -> {_.text("ul li")(0)},
        "Core" -> {_.text("ul li")(1)},
        "Drive" -> {_.text("ul li")(2)},
        "Transfer" -> {_.text("ul li")(3)},
        "price_monthly" -> {_.attr1("span.amount","data-dollar-amount")},
        "price_hourly" -> {_.attr1("span.amount","data-hourly-amount")}
      )
      .asSchemaRDD()
  }
}
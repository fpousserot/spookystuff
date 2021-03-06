package com.tribbloids.spookystuff.row

import com.tribbloids.spookystuff.SpookyContext
import com.tribbloids.spookystuff.actions._
import com.tribbloids.spookystuff.doc._
import org.apache.spark.ml.dsl.utils.Message

//TODO: extends Spark SQL Row
trait SpookyRow extends Message {

//  override def length: Int = this.productArity
//
//  override def get(i: Int): Any = this.productElement(i)
//
//  override def copy(): ProductRow = this //TODO: problems?
}

object FetchedRow {

  //  def apply(
  //             dataRow: DataRow = DataRow(),
  //             pageLikes: Seq[Fetched] = Seq()
  //           ): FetchedRow = dataRow -> pageLikes

}

/**
  * abstracted data structure where expression can be resolved.
  * not the main data structure in execution plan, SquashedPageRow is
  */
case class FetchedRow(
                       dataRow: DataRow = DataRow(),
                       fetched: Seq[Fetched] = Seq()
                     ) extends SpookyRow {

  //TODO: trace implementation is not accurate: the last backtrace has all previous exports removed
  def squash(spooky: SpookyContext): SquashedFetchedRow = SquashedFetchedRow(
    Array(dataRow),
    TraceView(
      docs = fetched
    )
  )

  def pages: Seq[Doc] = fetched.flatMap {
    case page: Doc => Some(page)
    case _ => None
  }

  def noPages: Seq[NoDoc] = fetched.flatMap {
    case noPage: NoDoc => Some(noPage)
    case _ => None
  }

  def getOnlyPage: Option[Doc] = {
    val pages = this.pages

    if (pages.size > 1) throw new UnsupportedOperationException("Ambiguous key referring to multiple pages")
    else pages.headOption
  }

  def getPage(keyStr: String): Option[Doc] = {

    //    if (keyStr == Const.onlyPageWildcard) return getOnlyPage

    val pages = this.pages.filter(_.name == keyStr)

    if (pages.size > 1) throw new UnsupportedOperationException("Ambiguous key referring to multiple pages")
    else pages.headOption
  }

  def getUnstructured(field: Field): Option[Unstructured] = {

    val page = getPage(field.name)
    val value = dataRow.getTyped[Unstructured](field)

    if (page.nonEmpty && value.nonEmpty) throw new UnsupportedOperationException("Ambiguous key referring to both pages and data")
    else page.orElse(value)
  }

  @transient lazy val dryrun: DryRun = fetched.toList.map(_.uid.backtrace).distinct
}
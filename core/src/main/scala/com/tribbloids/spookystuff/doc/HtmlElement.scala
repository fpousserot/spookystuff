package com.tribbloids.spookystuff.doc

import de.l3s.boilerpipe.extractors.ArticleExtractor
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{HttpHeaders, Metadata}
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.ToXMLContentHandler
import org.jsoup.nodes.Element
import org.jsoup.{Jsoup, select}

import scala.collection.JavaConverters

/**
  * Created by peng on 11/30/14.
  */
object HtmlElement {

  def apply(html: String, uri: String): HtmlElement = new HtmlElement(null, html, None, uri)

  def breadcrumb(e: Element): Seq[String] = {

    import JavaConverters._

    e.parents().asScala.map(_.tagName()).reverse :+ e.tagName()
  }
}

object TikaMetadataXMLElement {

  def apply(content: Array[Byte], charSet: String, mimeType: String, uri: String): HtmlElement = {

    val handler = new ToXMLContentHandler()

    val metadata = new Metadata()
    val stream = TikaInputStream.get(content, metadata)
    val html = try {
      metadata.set(HttpHeaders.CONTENT_ENCODING, charSet)
      metadata.set(HttpHeaders.CONTENT_TYPE, mimeType)
      val parser = new AutoDetectParser()
      val context = new ParseContext()
      parser.parse(stream, handler, metadata, context)
      handler.toString
    } finally {
      stream.close()
    }
    HtmlElement.apply(html, uri)
  }
}

class HtmlElement private (
                            @transient val _parsed: Element,
                            val html: String,
                            val tag: Option[String],
                            override val uri: String
                          ) extends Unstructured {

  //constructor for HtmlElement returned by .children()
  private def this(_parsed: Element) = this(
    _parsed,
    {
      _parsed.ownerDocument().outputSettings().prettyPrint(false)
      _parsed.outerHtml()
    },
    Some(_parsed.tagName()),
    _parsed.baseUri()
  )

  override def equals(obj: Any): Boolean = obj match {
    case other: HtmlElement =>
      (this.html == other.html) && (this.uri == other.uri)
    case _ => false
  }

  override def hashCode(): Int = (this.html, this.uri).hashCode()

  @transient lazy val parsed = Option(_parsed).getOrElse {

    tag match {
      case Some(_tag) =>
        val container = if (_tag == "tr" || _tag == "td") Jsoup.parseBodyFragment(s"<table>$html</table>", uri)
        else Jsoup.parseBodyFragment(html, uri)

        container.select(_tag).first()
      case _ =>
        Jsoup.parse(html, uri)
    }
  }

  import scala.collection.JavaConversions._

  override def findAll(selector: String) = new Elements(parsed.select(selector).map(new HtmlElement(_)).toList)

  override def findAllWithSiblings(selector: String, range: Range) = {

    val found: select.Elements = parsed.select(selector)
    expand(found, range)
  }

  private def expand(found: select.Elements, range: Range) = {
    val colls = found.map{
      self =>
        val selfIndex = self.elementSiblingIndex()
        //        val siblings = self.siblingElements()
        val siblings = self.parent().children()

        val prevChildIndex = siblings.lastIndexWhere(ee => found.contains(ee), selfIndex - 1)
        val head = if (prevChildIndex == -1) selfIndex + range.head
        else Math.max(selfIndex + range.head, prevChildIndex + 1)

        val nextChildIndex = siblings.indexWhere(ee => found.contains(ee), selfIndex + 1)
        val tail = if (nextChildIndex == -1) selfIndex + range.last
        else Math.min(selfIndex + range.last, nextChildIndex -1)

        val selected = siblings.slice(head,  tail + 1)

        new Siblings(selected.map(new HtmlElement(_)).toList)
    }
    new Elements(colls.toList)
  }

  override def children(selector: Selector) = {

    val found: select.Elements = new select.Elements(parsed.select(selector).filter(ee => parsed.children().contains(ee)))
    new Elements(found.map(new HtmlElement(_)).toList)
  }

  override def childrenWithSiblings(selector: Selector, range: Range): Elements[Siblings[Unstructured]] = {

    val found: select.Elements = new select.Elements(parsed.select(selector).filter(ee => parsed.children().contains(ee)))
    expand(found, range)
  }

  override def code: Option[String] = {
    Some(html)
  }

  override def formattedCode: Option[String] = {
    parsed.ownerDocument().outputSettings().prettyPrint(true)
    Some(parsed.outerHtml())
  }

  override def allAttr: Option[Map[String, String]] = {
    val result = Map(parsed.attributes().toSeq.map{
      attr =>
        attr.getKey -> attr.getValue
    }: _*)
    Some(result)
  }

  override def attr(attr: String, noEmpty: Boolean = true): Option[String] = {

    val result = parsed.attr(attr)

    if (noEmpty && result.trim.replaceAll("\u00A0", "").isEmpty) None
    else Option(result)
  }

  override def href = attr("abs:href") //TODO: if this is identical to the uri itself, it should be considered an inline/invalid link and have None output

  override def src = attr("abs:src")

  override def text: Option[String] = Option(parsed.text)

  override def ownText: Option[String] = Option(parsed.ownText)

  override def boilerPipe: Option[String] = {
    val result = ArticleExtractor.INSTANCE.getText(html)

    Option(result)
  }

  override def toString: String = html

  override def breadcrumb: Option[Seq[String]] = Some(HtmlElement.breadcrumb(this.parsed))
}
package com.tribbloids.spookystuff.testutils

object LocalPathResolver extends ResourceJARResolver("testutils")

trait LocalPathDocsFixture extends RemoteDocsFixture {

  import LocalPathResolver._

  override def HTML_URL = unpacked("testutils/files/Wikipedia.html")
  override def JSON_URL = unpacked("testutils/files/tribbloid.json")
  override def PNG_URL =  unpacked("testutils/files/logo11w.png")
  override def PDF_URL = unpacked("testutils/files/Test.pdf")
  override def XML_URL = unpacked("testutils/files/pom.xml")
  override def CSV_URL = unpacked("testutils/files/table.csv")

  def DIR_URL = unpacked("testutils/files")
}
package com.keevol.keenotes.desk.utils

import scala.sys.process.ProcessLogger
/**
 * @author fq@keevol.com
 */
class SimpleProcessLoggerFactory {
  val output = new StringBuilder
  val error = new StringBuilder

  val logger: ProcessLogger = ProcessLogger(o => output.append(s"$o \n"), e => error.append(s"$e \n"))

  def getConsoleOutput(): (String, String) = (output.toString(), error.toString())
}

package com.keevol.keenotes.desk.domains

import java.util.Date
/**
 * @author fq@keevol.com
 */
case class Note(var content: String, var channel: String, var dt: Date) {
  def this() = this("", "", new Date())
}
package com.keevol.keenotes.desk.domains

import java.util.Date

class Note(var content: String, var channel: String, var dt: Date) {
  def this() = this("", "", new Date())
}
package com.keevol.keenotes.desk

import org.apache.commons.io.FileUtils
import org.sqlite.SQLiteConfig

import java.io.File
import java.sql.{Connection, DriverManager}
import scala.collection.JavaConverters._

/**
 * https://stackoverflow.com/questions/21879872/sqlite-java-sqlite-error-with-load-extension
 */
object Sqlite3Sandbox {
  def main(args: Array[String]): Unit = {

    println(s"""java.io.tmpdir=`${System.getProperty("java.io.tmpdir")}`""")

    FileUtils.copyURLToFile(getClass.getResource("/so/libsimple.dylib"), new File(System.getProperty("java.io.tmpdir"), "libsimple.dylib"))
    FileUtils.listFiles(new File(System.getProperty("java.io.tmpdir")), Array("dylib"), false).asScala.foreach(println)



    val cfg = new SQLiteConfig()
    cfg.enableLoadExtension(true) // 1. prerequisites

    var conn: Connection = null
    try {
      conn = DriverManager.getConnection(s"""jdbc:sqlite:memory""", cfg.toProperties) // 2.
      val stat = conn.createStatement()
      stat.executeUpdate("""SELECT load_extension('/usr/local/bin/libsimple')""") // 3. /usr/local/bin/libsimple.dylib
      stat.close()
    } finally {
      conn.close()
    }
  }
}
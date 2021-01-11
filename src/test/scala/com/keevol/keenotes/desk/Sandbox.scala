package com.keevol.keenotes.desk

import java.io.File
import java.util.concurrent.TimeUnit

object Sandbox{
  def main(args: Array[String]): Unit = {
//    Runtime.getRuntime().exec(Array("osascript", "-e", """display notification "This is a message" with title "Title" subtitle "Subtitle" sound name "Glass""""));
//
//    TimeUnit.SECONDS.sleep(3)


    val f = new File("~/aa.txt")
    println(f.getAbsolutePath)

  }
}
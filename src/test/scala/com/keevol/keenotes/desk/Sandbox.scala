package com.keevol.keenotes.desk

import java.util.concurrent.TimeUnit

object Sandbox{
  def main(args: Array[String]): Unit = {
    Runtime.getRuntime().exec(Array("osascript", "-e", """display notification "This is a message" with title "Title" subtitle "Subtitle" sound name "Glass""""));

    TimeUnit.SECONDS.sleep(3)


  }
}
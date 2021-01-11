package com.keevol.keenotes.desk.repository

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.springframework.jdbc.core.JdbcTemplate

import java.io.File

class Sqlite3(location: File) {

  private val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl(s"jdbc:sqlite:${location.getAbsolutePath}")
  private val ds: HikariDataSource = new HikariDataSource(config)
  val jdbc = new JdbcTemplate(ds)

  def execute(sql: String): Unit = jdbc.execute(sql)

  def getExecutor(): JdbcTemplate = jdbc

  def dispose() = ds.close()

}
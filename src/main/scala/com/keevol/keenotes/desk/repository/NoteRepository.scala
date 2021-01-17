package com.keevol.keenotes.desk.repository

import com.keevol.keenotes.desk.domains.Note
import com.keevol.keenotes.desk.settings.Settings
import com.keevol.utils.DateFormalizer
import javafx.beans.value.{ChangeListener, ObservableValue}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.jdbc.core.RowMapper

import java.io.File
import java.sql.{PreparedStatement, ResultSet}
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.ListBuffer

class NoteRepository(settings: Settings) {
  val logger:Logger = LoggerFactory.getLogger(classOf[NoteRepository])

  val noteRowMapper:RowMapper[Note] = new RowMapper[Note] {
    override def mapRow(rs: ResultSet, i: Int): Note = {
      val note = new Note()
      note.channel = rs.getString("tags")
      note.content = rs.getString("content")
      note.dt = DateFormalizer.fromSqliteLocalTime(rs.getString("updated"))
      note
    }
  }

  val executor: AtomicReference[Sqlite3] = new AtomicReference[Sqlite3]()

  val sqlite3 = new Sqlite3(new File(settings.sqliteFileProperty.get()))
  executor.set(sqlite3)

  settings.sqliteFileProperty.addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      val oldDB = executor.get()
      if (oldDB != null) oldDB.dispose()
      executor.set(new Sqlite3(new File(newValue)))
    }
  })

  executor.get().execute(
    """CREATE TABLE IF NOT EXISTS notes (
      |                	content text NOT NULL,
      |                	tags text NOT NULL,
      |                	updated TEXT DEFAULT (datetime('now','localtime')) -- datetime in ISO8601 format
      |                );""".stripMargin)



  def load(limitCount: Int = 11): List[Note] = {
    val q = s"""select * from notes order by datetime(updated) desc limit $limitCount"""
    logger.info(s"load notes from db with sql='${q}'")
    val jdbc = executor.get().getExecutor()
    jdbc.query(q, noteRowMapper).asScala.toList
  }

  def search(keyword: String): List[Note] = executor.get().getExecutor().query(s"""select * from notes where content like "%$keyword%"""", noteRowMapper).asScala.toList

  def insert(note: Note) = {
    executor.get().getExecutor().update("insert into notes(content, tags, updated) values(?,?,?)", (ps: PreparedStatement) => {
      ps.setString(1, note.content)
      ps.setString(2, note.channel)
      ps.setString(3, DateFormalizer.sqliteLocal(note.dt)) // not necessary in fact
    })
  }

  def dispose(): Unit = executor.get().dispose()

}
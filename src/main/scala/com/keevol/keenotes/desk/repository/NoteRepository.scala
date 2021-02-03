package com.keevol.keenotes.desk.repository

import com.keevol.keenotes.desk.domains.Note
import com.keevol.keenotes.desk.settings.Settings
import com.keevol.utils.{DateFormalizer, Sqlite3}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}
import org.sqlite.SQLiteConfig

import java.io.File
import java.sql.{PreparedStatement, ResultSet}
import java.util.concurrent.atomic.AtomicReference

/**
 * @author fq@keevol.com
 */
class NoteRepository(settings: Settings) {
  val logger: Logger = LoggerFactory.getLogger(classOf[NoteRepository])

  val noteRowMapper: ResultSet => Note = { rs: ResultSet =>
    val note = new Note()
    note.channel = rs.getString("tags")
    note.content = rs.getString("content")
    note.dt = DateFormalizer.fromSqliteLocalTime(rs.getString("updated"))
    note
  }

  val sqliteLocation = new AtomicReference[String]()

  val executor: AtomicReference[Sqlite3] = new AtomicReference[Sqlite3]()
  setupSqliteExecutor()

  def refreshSqliteDBIfNecessary() = {
    if (!StringUtils.equalsAnyIgnoreCase(settings.sqliteFileProperty.get(), sqliteLocation.get())) {
      val oldDB = executor.get()
      if (oldDB != null) oldDB.dispose()
      setupSqliteExecutor()
    }
  }


  def setupSqliteExecutor(): Unit = {
    sqliteLocation.set(settings.sqliteFileProperty.get())

    val cfg = new SQLiteConfig()
    cfg.enableLoadExtension(true)

    val sqlite3 = new Sqlite3(new File(settings.sqliteFileProperty.get()), cfg)
    sqlite3.connect()

    val extractedSoFile = new File(System.getProperty("java.io.tmpdir"), "libsimple.dylib")
    logger.info(s"extract libsimple.dylib in classpath to ${extractedSoFile.getAbsolutePath}")
    FileUtils.copyURLToFile(getClass.getResource("/so/libsimple.dylib"), extractedSoFile)

    val loadCmd = s"""SELECT load_extension('${System.getProperty("java.io.tmpdir")}libsimple')"""
    logger.info(s"load simple extension with command: $loadCmd")
    sqlite3.execute(loadCmd)

    executor.set(sqlite3)

    setupTables()
    migrateDataIfNecessary()
  }

  def setupTables() = {
    executor.get().execute("CREATE VIRTUAL TABLE IF NOT EXISTS NoteSearch USING fts5(content, tags, updated, tokenize='simple');")
    executor.get().execute("CREATE TABLE IF NOT EXISTS migration_mark(mark text, updated TEXT DEFAULT (datetime('now','localtime')))")
  }

  def migrateDataIfNecessary() = {
    val markList = executor.get().query("select * from migration_mark") { rs =>
      rs.getString("mark")
    }

    if (markList.isEmpty) {
      logger.info("migrate notes from `notes` table to NoteSearch...")
      executor.get().execute("INSERT INTO NoteSearch SELECT content, tags, updated FROM notes;")
      logger.info("mark migration done!")
      executor.get().execute(s"INSERT INTO migration_mark(mark) values('1')")

      executor.get().execute("""DROP TABLE IF EXISTS notes""")
      executor.get().execute("""DROP INDEX IF EXISTS notes_updated_idx""")
    }
  }


  def load(limitCount: Int = 11): List[Note] = {
    val q = s"""select * from NoteSearch order by datetime(updated) desc limit $limitCount"""
    logger.info(s"load notes from db with sql='${q}'")
    executor.get().query(q)(noteRowMapper)
  }

  def search(keyword: String): List[Note] = {
    val so = s"""SELECT * FROM NoteSearch WHERE NoteSearch MATCH 'content:${keyword}*' order by datetime(updated) desc;"""
    logger.info(s"""search with keyword=`$keyword` and search phrase=`$so` """)
    executor.get().query(so)(noteRowMapper)
  }

  def insert(note: Note) = {
    executor.get().update("insert into NoteSearch(content, tags, updated) values(?,?,?)") { (ps: PreparedStatement) =>
      ps.setString(1, note.content)
      ps.setString(2, note.channel)
      ps.setString(3, DateFormalizer.sqliteLocal(note.dt)) // not necessary in fact
    }
  }

  def delete(content: String, ch: String): Int = executor.get().update("delete from NoteSearch where content=? and tags=?") { (ps: PreparedStatement) =>
    ps.setString(1, content)
    ps.setString(2, ch)
  }

  def dispose(): Unit = {
    val db = executor.get()
    if (db != null) db.dispose()
  }

}
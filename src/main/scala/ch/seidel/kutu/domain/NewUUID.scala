package ch.seidel.kutu.domain

import org.sqlite.{Function, SQLiteConnection}

import java.sql.{SQLDataException, SQLException}
import java.util.UUID

object NewUUID {
  def install(c: SQLiteConnection): Unit = {
    Function.create(c, "NewUUID", new NewUUID)
  }
}

class NewUUID extends Function {
  @throws[SQLException]
  override protected def xFunc(): Unit = {
    try result(UUID.randomUUID.toString)
    catch {
      case exception: Exception =>
        throw new SQLDataException("NewUUID(): Problem occoured: " + exception.getLocalizedMessage)
    }
  }
}
package com.gamebot.repositories

import io.reactivex.Single

import scala.language.implicitConversions
import scala.jdk.CollectionConverters._
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.lang.scala.json.Json.{arr, obj}
import io.vertx.reactivex.sqlclient.{Row, RowSet, SqlClient, Tuple}


abstract class Repository(table: String, schema: String, da: DatabaseAccess) {

  val tableName = s"$schema.$table"

  def all(connection: Option[SqlClient] = None): Single[JsonArray] = {
    queryWithSqlClient(s"SELECT * FROM $tableName", Tuple.tuple(), connection).map(rowSet => {
      rowSet.getRows
    })
  }

  def find(id: String, connection: Option[SqlClient] = None): Single[JsonObject] = {
    val result: Single[JsonObject] = queryWithSqlClient(s"SELECT * FROM $tableName WHERE id = $$1", Tuple.of(id), connection).map(_.getRow)
    //    if (result.isEmpty)
    //      throw ModelNotFoundException("No object found with ID", jArr(id))
    result
  }

  def insert(data: JsonObject, connection: Option[SqlClient] = None): Single[JsonObject] = {
    queryWithSqlClient(s"INSERT INTO $tableName (data) VALUES ($$1::jsonb) RETURNING *", Tuple.of(data), connection).map(_.getRow)
  }

  def update(id: String, data: JsonObject, connection: Option[SqlClient] = None): Single[JsonObject] = {
    queryWithSqlClient(s"UPDATE $tableName SET data = $$1 WHERE id = $$2 RETURNING *", Tuple.of(data, id), connection).map(_.getRow)
  }

  def delete(id: String, connection: Option[SqlClient] = None): Single[JsonObject] = {
    val deleted: Single[JsonObject] = queryWithSqlClient(s"DELETE FROM $tableName WHERE id = $$1 RETURNING id", Tuple.of(id), connection).map(_.getRow)
    //    if (deleted.isEmpty)
    //      throw ModelNotFoundException("Tried to delete an item that does not exist", jArr(id))
    deleted
  }

  def query(sql: String, tuple: Tuple, connection: Option[SqlClient] = None): Single[JsonArray] = queryWithSqlClient(sql, tuple, connection).map(_.getRows)

  def queryOne(sql: String, tuple: Tuple, connection: Option[SqlClient] = None): Single[JsonObject] = queryWithSqlClient(sql, tuple, connection).map(_.getRow)

  def queryWithSqlClient(sql: String, tuple: Tuple, connection: Option[SqlClient] = None): Single[RowSet[Row]] = {
    connection match {
      case None => da.getConnection(conn => conn.preparedQuery(sql).rxExecute(tuple))
      case Some(client) => client.preparedQuery(sql).rxExecute(tuple)
    }
  }

  implicit class RowSetExtensions(rowSet: RowSet[Row]) {
    def getRows: JsonArray = {
      val seq = rowSet.iterator().asScala.toList
      val row = arr(seq.map { row => jsonRow(row, rowSet.columnsNames().asScala.toList) })
      row
    }

    def getRow: JsonObject = {
      val seq: List[Row] = rowSet.iterator().asScala.toList
      val result: Option[JsonObject] = seq.map { row => jsonRow(row, rowSet.columnsNames().asScala.toList) }.headOption

      result match {
        case Some(json) => json
        case _ => obj()
      }
    }
  }

  private def jsonRow(row: Row, columnNames: List[String]): JsonObject = {
    val extraValues = columnNames.filter(columnName => columnName != "data").map(columnName => (columnName, row.getValue(columnName))).toMap
    val json = if (columnNames.contains("data"))
      row.getJsonObject("data").mergeIn(obj(extraValues))
    else
      new JsonObject()

    json
  }

}

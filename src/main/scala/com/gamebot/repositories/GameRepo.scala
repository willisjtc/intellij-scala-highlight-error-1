package com.gamebot.repositories

import io.reactivex.Single
import io.vertx.lang.scala.json.JsonArray
import io.vertx.reactivex.sqlclient.Tuple


case class GameRepo(schema: String, da: DatabaseAccess) extends Repository("game", schema, da) {

  def all(): Single[JsonArray] = {
    queryWithSqlClient(s"select * from $tableName", Tuple.tuple()).map(_.getRows)
  }
}

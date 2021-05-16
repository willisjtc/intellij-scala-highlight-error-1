package com.gamebot.repositories

import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.SqlClient
import io.vertx.sqlclient.PoolOptions
import scala.jdk.CollectionConverters._


case class DatabaseAccess(config: JsonObject, vertx: Vertx) {
  val dbName: String = config.getString("SERVICE_DB_NAME")
  val connectOptions: PgConnectOptions = new PgConnectOptions().setPort(config.getInteger("SERVICE_DB_PORT"))
                                                               .setHost(config.getString("SERVICE_DB_HOST"))
                                                               .setDatabase(dbName)
                                                               .setUser(config.getString("SERVICE_DB_USER"))
                                                               .setPassword(config.getString("SERVICE_DB_PASSWORD"))
                                                               .setProperties(Map[String, String]("search_path" -> config.getString("schema", "public")).asJava)
  val poolOptions: PoolOptions = new PoolOptions().setMaxSize(10)
  val pool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)

  def getConnection[T](dbAction: (SqlClient) => Single[T]): Single[T] = {
    val result: Single[T] = pool.rxGetConnection().flatMap(connection => {
      dbAction(connection).doOnError(error => {
        error.printStackTrace()
      }).doFinally(() => {
        connection.close()
      })
    })
    result
  }

  def getTransaction[T](dbAction: (SqlClient) => T): Single[T] = {
    val result: Single[T] = pool.rxGetConnection().flatMap(connection => {
      val transaction = connection.rxBegin()
      transaction.flatMap(transaction => {
        val dbActionResult = dbAction(connection)
        Single.just(dbActionResult).doOnSuccess(_ => {
          transaction.commit()
        }).doOnError(error => {
          transaction.rollback()
          error.printStackTrace()
        }).doFinally(() => {
          connection.close()
        })
      })
    })

    result
  }
}

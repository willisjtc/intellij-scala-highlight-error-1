package com.gamebot.migrations

import com.gamebot.config.Config
import io.reactivex.{CompletableObserver, Single}
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.vertx.lang.scala.json.JsonObject
import io.vertx.reactivex.core.Vertx

import scala.language.implicitConversions
import scala.jdk.CollectionConverters._
import org.flywaydb.core.Flyway


object Migrator extends App {
  val vertx = Vertx.vertx
  println("about to call Config.get")
  Config.get(vertx).flatMap(config => {
    Migration().migrate(config)
    Single.just("Success!")
  }).doFinally(() => {
    vertx.close()
  }).subscribe((result: String) => {
    println("success!")
  }, (error: Throwable) => {
    error.printStackTrace()
  })
  println("here")
}

case class Migration() {
  def migrate(dbConfig: JsonObject) {
    val url = s"jdbc:postgresql://${dbConfig.getString("SERVICE_DB_HOST")}:${dbConfig.getInteger("SERVICE_DB_PORT")}/${dbConfig.getString("SERVICE_DB_NAME")}"
    val user = dbConfig.getString("SERVICE_DB_USER")
    val password = dbConfig.getString("SERVICE_DB_PASSWORD")
    Array[String]("public", "test").foreach { schema =>
      println(s"on schema $schema")
      val flyway = Flyway.configure().placeholders(Map[String, String]("schema" -> schema).asJava).schemas().dataSource(url, user, password).load()
      println("about to migrate")
      flyway.migrate()
    }
  }
}

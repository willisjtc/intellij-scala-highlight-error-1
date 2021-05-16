package com.gamebot.config

import io.reactivex.Single
import io.vertx.config.{ConfigRetrieverOptions, ConfigStoreOptions}
import io.vertx.lang.scala.json.Json.obj
import io.vertx.lang.scala.json.JsonObject
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx

object Config {
  def get(vertx: Vertx): Single[JsonObject] = {
    val fileStoreOptions = new ConfigStoreOptions().setType("file").setConfig(obj(Map[String, String](("path", "config.json"))))
    val configRetrieverOptions = new ConfigRetrieverOptions().addStore(fileStoreOptions)
    ConfigRetriever.create(vertx, configRetrieverOptions).rxGetConfig()
  }
}

package com.gamebot.main

import com.gamebot.config.Config
import com.gamebot.graphql.GraphQLService
import com.gamebot.repositories.{DatabaseAccess, GameRepo}
import io.reactivex.Completable
import io.vertx.reactivex.core.AbstractVerticle

class MainVerticle extends AbstractVerticle {

  override def rxStart(): Completable = {
    Config.get(vertx).flatMapCompletable(config => {
      val da = DatabaseAccess(config, vertx)
      val gameRepo = GameRepo("public", da)
      val router = GraphQLService(vertx, gameRepo).setupGraphQL()
      vertx.createHttpServer()
           .requestHandler(router)
           .rxListen(8888)
           .ignoreElement()
    })
  }
}


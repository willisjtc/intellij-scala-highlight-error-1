package com.gamebot.main

import io.vertx.core.{DeploymentOptions, VertxOptions}
import io.vertx.reactivex.core.Vertx


object GameApp extends App {
  val vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(10))
  vertx.rxDeployVerticle(new MainVerticle())
       .doOnError((error: Throwable) => {
         error.printStackTrace()
         vertx.rxClose().subscribe()
       }).subscribe()
}

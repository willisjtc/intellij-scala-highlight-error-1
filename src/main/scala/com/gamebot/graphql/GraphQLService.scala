package com.gamebot.graphql

import com.gamebot.repositories.GameRepo
import graphql.GraphQL
import graphql.scalars.ExtendedScalars
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.{SchemaGenerator, SchemaParser, TypeDefinitionRegistry, TypeRuntimeWiring}
import hu.akarnokd.rxjava2.interop.SingleInterop
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions
import io.vertx.lang.scala.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.handler.BodyHandler
import io.vertx.reactivex.ext.web.handler.graphql.{GraphQLHandler, GraphiQLHandler}

import java.util
import java.util.concurrent.CompletionStage
import java.util.stream.Collectors
import scala.jdk.CollectionConverters.CollectionHasAsScala

case class GraphQLService(vertx: Vertx, gameRepo: GameRepo) {

  def setupGraphQL(): Router = {
    val router = Router.router(vertx)
    val options = new GraphiQLHandlerOptions().setEnabled(true)
    router.route().handler(BodyHandler.create())
    router.route("/graphql").handler(GraphQLHandler.create(setupSchema()))
    router.route("/graphiql/*").handler(GraphiQLHandler.create(options))
    router
  }

  def setupSchema(): GraphQL = {
    val schema = vertx.fileSystem.readFileBlocking("games.graphqls").toString
    val schemaParser = new SchemaParser()
    val typeDefinitionRegistry: TypeDefinitionRegistry = schemaParser.parse(schema)

    val runtimeWiring = newRuntimeWiring().scalar(ExtendedScalars.DateTime).scalar(ExtendedScalars.Json)
                                          .`type`("Query", (t: TypeRuntimeWiring.Builder) => t.dataFetcher("games", games)).build()

    val schemaGenerator = new SchemaGenerator()
    val graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
    GraphQL.newGraphQL(graphQLSchema).build()
  }

  def games: DataFetcher[CompletionStage[util.List[util.Map[String, Object]]]] = {
    val games = gameRepo.all()
    val dataFetcher: DataFetcher[CompletionStage[util.List[util.Map[String, Object]]]] = (environment) => {
      println("merged field: " + environment.getMergedField.getFields.asScala.mkString(","))
      games.map(array => array.getList.asInstanceOf[util.List[JsonObject]]
                                    .stream()
                                    .map(item => item.getMap)
                                    .collect(Collectors.toList[util.Map[String, Object]]))
                 .to(SingleInterop.get())
    }
    dataFetcher
  }
}


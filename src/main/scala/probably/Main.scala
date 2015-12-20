package probably

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.AskSupport
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.json.DefaultJsonProtocol._

trait Protocols {
  implicit val addedFormat = jsonFormat1(Added.apply)
  implicit val probableResultFormat = jsonFormat2(ProbableResult.apply)
  implicit val statsFormat = jsonFormat1(Stats.apply)
}


object Main extends App with AskSupport with Protocols {
  implicit val system = ActorSystem("probably")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(100, TimeUnit.SECONDS)

  val config = new Settings(ConfigFactory.load())
  val structures = new AllStructures(system.actorOf(Props(classOf[Structures])))

  val routes:Route = path(Segment) { name=>
      (post & entity(as[List[String]])) { keys =>
        complete {
          structures addAllTo(name, keys)
          Accepted
        }
      } ~ get {
        complete {
          structures statsOf name
        }
      }
    }~path(Segment/Segment) { (name,key)=> {
          put {
            complete {
              structures addTo(name, key)
            }
          } ~ get {
              complete {
                structures getFrom(name, key)
              }
            }
          }
        }

  Http().bindAndHandle(Route.handlerFlow(routes), config.httpHost, config.httpPort)
}
package probably

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.pattern.AskSupport
import akka.persistence.Persistence
import akka.util.Timeout

import scala.concurrent.ExecutionContext

case class CreateOrGet(name:String)

class Structures extends Actor {
  var structures = Map[String, ActorRef]()
  val logger = Logging(context.system, getClass)

  override def receive: Receive = {
    case CreateOrGet(name) => {
      if(!(structures contains name))
          structures = structures + (name -> context.system.actorOf(Props(classOf[BloomFilter], name), s"bloom-${name}"))
      sender ! structures(name)
    }
    case m => logger.info(m.toString)
  }
}


class AllStructures(structures:ActorRef)(implicit val timeout:Timeout, val context: ExecutionContext) extends AskSupport {
  def addTo(name:String, key:String) = {
    for(
      structure <- (structures ? CreateOrGet(name)).mapTo[ActorRef];
      result <- (structure ? Add(key)).mapTo[Added]
    ) yield result
  }

  def addAllTo(name:String, keys:List[String]) = {
    for(structure <- (structures ? CreateOrGet(name)).mapTo[ActorRef])
      structure ! AddAll(keys)
  }

  def getFrom(name:String, key:String) = {
    for(
      structure <- (structures ? CreateOrGet(name)).mapTo[ActorRef];
      result <- (structure ? IsPresent(key)).mapTo[ProbableResult]
    ) yield result
  }

  def statsOf(name:String) = {
   for(
     structure <- (structures ? CreateOrGet(name)).mapTo[ActorRef];
     result <- (structure ? GetStats).mapTo[Stats]
   ) yield result
  }
}
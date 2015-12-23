package probably

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.pattern.AskSupport
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.Timeout
import probably.structures.StructureFactory

import scala.concurrent.ExecutionContext

case class Create(name:String)
case class GetOrCreate(name:String)
case class Get(name:String)
case class Exists(name:String)
case class StructureSettings(expectedInsertions: Int)

object Structures {
  def structureName(set:String, name:String) = s"${set}-${name}"
}

class Structures(structureFactory:StructureFactory[_],  expectedErrorPercent:Double) extends PersistentActor {
  var structures = Map[String, ActorRef]()
  val logger = Logging(context.system, getClass)

  override def persistenceId: String = "all-structures"

  override def receiveRecover: Receive = {
    case request:Create => sender ! create(request.name)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case request:Create => persist(request){request => sender ! create(request.name)}
    case GetOrCreate(name) => if(structures contains name) self.forward(Get(name))
                                else self.forward(Create(name))
    case Get(name) => sender ! structures(name)
    case Exists(name) => sender ! (structures contains name)
    case m => logger.info(s"Ignoring $m")
  }

  def create(name:String) = {
    if(!(structures contains name))
      structures = structures +
        (name -> context.system.actorOf(Props(classOf[Structure], name, structureFactory.create(expectedErrorPercent)), s"${structureFactory.name}-${name}"))
    structures(name)
  }
}


class AllStructures(structures:Map[String, ActorRef])(implicit val timeout:Timeout, val context: ExecutionContext) extends AskSupport {
  def addTo(set:String, name:String, key:String) = {
    for(
      structure <- (structures(set) ? GetOrCreate(name)).mapTo[ActorRef];
      result <- (structure ? Add(key)).mapTo[Added]
    ) yield result
  }

  def addAllTo(set:String, name:String, keys:List[String]) = {
    for(structure <- (structures(set) ? GetOrCreate(name)).mapTo[ActorRef])
      structure ! AddAll(keys)
  }

  def getFrom(set:String, name:String, key:String) = {
    for(
      structure <- (structures(set) ? Get(name)).mapTo[ActorRef];
      result <- (structure ? IsPresent(key)).mapTo[ProbableResult]
    ) yield result
  }

  def statsOf(set:String, name:String) = {
   for(
     structure <- (structures(set) ? Get(name)).mapTo[ActorRef];
     result <- (structure ? GetStats).mapTo[Stats]
   ) yield result
  }

  def exists(set:String, name:String) = (structures(set) ? Exists(name)).mapTo[Boolean]
}
package probably

import akka.actor.{ActorRef, Props}
import akka.event.Logging
import akka.pattern.AskSupport
import akka.persistence.{PersistentActor, RecoveryCompleted}
import akka.util.Timeout
import probably.structures.BloomFilter

import scala.concurrent.ExecutionContext

case class Create(name:String)
case class GetOrCreate(name:String)
case class Get(name:String)
case class Exists(name:String)
case class StructureSettings(expectedInsertions: Int)

class Structures extends PersistentActor {
  var structures = Map[String, ActorRef]()
  val logger = Logging(context.system, getClass)

  override def persistenceId: String = "all-structures"

  override def receiveRecover: Receive = {
    case request:Create => sender ! create(request.name)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case request:Create => persist(request){request => sender ! create(request.name)}
    case request:GetOrCreate => if(structures contains request.name) self.forward(Get(request.name))
                                else self.forward(Create(request.name))
    case Get(name) => sender ! structures(name)
    case Exists(name) => sender ! (structures contains name)
    case m => logger.info(s"Ignoring $m")
  }

  def create(name:String) = {
    if(!(structures contains name))
      structures = structures + (name -> context.system.actorOf(Props(classOf[Structure], name, new BloomFilter()), s"bloom-${name}"))
    structures(name)
  }
}


class AllStructures(structures:ActorRef)(implicit val timeout:Timeout, val context: ExecutionContext) extends AskSupport {
  def addTo(name:String, key:String) = {
    for(
      structure <- (structures ? GetOrCreate(name)).mapTo[ActorRef];
      result <- (structure ? Add(key)).mapTo[Added]
    ) yield result
  }

  def addAllTo(name:String, keys:List[String]) = {
    for(structure <- (structures ? GetOrCreate(name)).mapTo[ActorRef])
      structure ! AddAll(keys)
  }

  def getFrom(name:String, key:String) = {
    for(
      structure <- (structures ? Get(name)).mapTo[ActorRef];
      result <- (structure ? IsPresent(key)).mapTo[ProbableResult]
    ) yield result
  }

  def statsOf(name:String) = {
   for(
     structure <- (structures ? Get(name)).mapTo[ActorRef];
     result <- (structure ? GetStats).mapTo[Stats]
   ) yield result
  }

  def exists(name:String) = (structures ? Exists(name)).mapTo[Boolean]
}
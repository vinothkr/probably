package probably

import akka.event.Logging
import akka.persistence.{SnapshotOffer, RecoveryCompleted, PersistentActor}
import scala.concurrent.duration.DurationInt

case class ProbableResult(value:Boolean, probability:Double)


case class Add(key:String)
case class AddAll(keys:List[String])
case class IsPresent(key:String)
case object GetStats
case object SnapshotTick

case class Added(key:String)
case class Stats(approxCount:Long, expectedError: Double)

trait ProbableSet {
  def put(key:String):Unit
  def putAll(keys:List[String]):Unit
  def isPresent(key:String): ProbableResult
  def getStats: Stats
}

class Structure(name:String, startState:ProbableSet) extends PersistentActor {
  var structure = startState
  val logger = Logging(context.system, getClass)
  implicit val executionContext = context.system.dispatcher
  var haveEditsSinceLastSnapshot = false

  override def persistenceId: String = name

  override def preStart = {
    context.system.scheduler.schedule(15 minutes, 15 minutes, self, SnapshotTick)
  }

  override def receiveRecover: Receive = {
    case Add(key) => structure.put(key)
    case AddAll(keys) => structure.putAll(keys)
    case RecoveryCompleted =>
    case SnapshotOffer(meta, snap:ProbableSet) => structure = snap
  }

  override def receiveCommand: Receive = {
    case add:Add =>
      val _sender = sender()
      persist(add){add => structure.put(add.key); _sender ! Added(add.key)}
      haveEditsSinceLastSnapshot = true
    case addAll:AddAll =>
      persist(addAll){addAll => structure.putAll(addAll.keys)}
      haveEditsSinceLastSnapshot = true
    case IsPresent(key) => sender ! structure.isPresent(key)
    case GetStats => sender ! structure.getStats
    case SnapshotTick => {
      if(haveEditsSinceLastSnapshot) saveSnapshot(structure)
      haveEditsSinceLastSnapshot = false
    }
  }
}

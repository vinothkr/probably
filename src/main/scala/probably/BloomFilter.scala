package probably

import akka.event.Logging
import akka.persistence.{SnapshotOffer, RecoveryCompleted, PersistentActor}
import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter => GoogleBloom, Funnels}

import scala.concurrent.duration.DurationInt

case class ProbableResult(value:Boolean, probability:Double)


case class Add(key:String)
case class AddAll(keys:List[String])
case class IsPresent(key:String)
case object GetStats
case object SnapshotTick

case class Added(key:String)
case class Stats(expectedError: Double)


class BloomFilter(name:String) extends PersistentActor{
  val bloom = GoogleBloom.create(Funnels.stringFunnel(Charsets.UTF_8),1000000)
  val logger = Logging(context.system, getClass)
  implicit val executionContext = context.system.dispatcher
  var haveEditsSinceLastSnapshot = false

  override def persistenceId: String = name

  override def preStart = {
    context.system.scheduler.schedule(15 minutes, 15 minutes, self, SnapshotTick)
  }

  override def receiveRecover: Receive = {
    case Add(key) => bloom.put(key)
    case AddAll(keys) => keys.foreach(bloom.put)
    case RecoveryCompleted =>
    case SnapshotOffer(meta, snap:GoogleBloom[CharSequence]) => bloom.putAll(snap)
  }

  override def receiveCommand: Receive = {
    case add:Add =>
      val _sender = sender()
      persist(add){add => bloom.put(add.key); _sender ! Added(add.key)}
      haveEditsSinceLastSnapshot = true
    case addAll:AddAll =>
      persist(addAll){addAll => addAll.keys.foreach(bloom.put)}
      haveEditsSinceLastSnapshot = true
    case IsPresent(key) => sender !(if(bloom.mightContain(key)) ProbableResult(true, 1.0 - bloom.expectedFpp()) else ProbableResult(false, 1.0))
    case GetStats => sender ! Stats(bloom.expectedFpp())
    case SnapshotTick => {
      if(haveEditsSinceLastSnapshot) saveSnapshot(bloom)
      haveEditsSinceLastSnapshot = false
    }
  }
}

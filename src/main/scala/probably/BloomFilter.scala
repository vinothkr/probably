package probably

import akka.persistence.{RecoveryCompleted, PersistentActor}
import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter => GoogleBloom, Funnels}

case class ProbableResult(value:Boolean, probability:Double)


case class Add(key:String)
case class IsPresent(key:String)
case class Added(key:String)

class BloomFilter(name:String) extends PersistentActor{
  val bloom = GoogleBloom.create(Funnels.stringFunnel(Charsets.UTF_8),1000000)

  override def persistenceId: String = name

  override def receiveRecover: Receive = {
    case Add(key) => bloom.put(key)
    case RecoveryCompleted =>
  }

  override def receiveCommand: Receive = {
    case add:Add => {
      val _sender = sender()
      persist(add){add => bloom.put(add.key); _sender ! Added(add.key)}
    }
    case IsPresent(key) => sender !(if(bloom.mightContain(key)) ProbableResult(true, 1.0 - bloom.expectedFpp()) else ProbableResult(false, 1.0))
  }
}
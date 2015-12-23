package probably.structures

import com.twitter.algebird.{HyperLogLogMonoid, HyperLogLog, HLL}
import probably.{ProbableResult, Stats, ProbableSet}

import scala.util.Random

class HyperLogLog(expectedErrorPercent:Double) extends ProbableSet with Serializable {
  val hllMonoid = new HyperLogLogMonoid(HyperLogLog.bitsForError(expectedErrorPercent / 100))
  var hll = hllMonoid.zero

  override def put(key: String): Unit = hll = hll + hllMonoid.create(key.getBytes)

  override def isPresent(key: String): ProbableResult = ProbableResult(Random.nextBoolean(), 0.5)

  override def putAll(keys: List[String]): Unit = keys.foreach(put)

  override def getStats: Stats = Stats(hll.approximateSize.estimate, 0.0)
}
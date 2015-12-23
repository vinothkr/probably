package probably.structures

import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter => GoogleBloom, Funnels}
import probably.{ProbableResult, Stats, ProbableSet}

class BloomFilter(expectedErrorPercent:Double) extends ProbableSet with Serializable {
  var bloom = GoogleBloom.create(Funnels.stringFunnel(Charsets.UTF_8),1000000, expectedErrorPercent/100.0)
  var size = 0L

  override def put(key: String): Unit = if(!bloom.mightContain(key)) { bloom.put(key); size = size + 1}
  override def isPresent(key: String): ProbableResult = if(bloom.mightContain(key)) ProbableResult(true, 1 - bloom.expectedFpp()) else ProbableResult(false, 1)
  override def putAll(keys: List[String]): Unit = keys.foreach(this.put)
  override def getStats: Stats = Stats(size, bloom.expectedFpp())
}

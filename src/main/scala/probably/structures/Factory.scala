package probably.structures

import probably.ProbableSet

trait StructureFactory[T <: ProbableSet] {
  def create(expectedErrorPercent:Double):T
  def name:String
}

class BloomFilterFactory extends StructureFactory[BloomFilter]{
  def create(expectedErrorPercent:Double) = new BloomFilter(expectedErrorPercent)
  def name = "bloom"
}

class HyperLogLogFactory extends StructureFactory[HyperLogLog] {
  def create(expectedErrorPercent:Double) = new HyperLogLog(expectedErrorPercent)
  def name = "hll"
}
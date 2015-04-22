package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReaderProvider[K] {
  def reader(instance: CassandraInstance, layerId: LayerId, cassandraLayerMetaData: CassandraLayerMetaData, keyIndex: KeyIndex[K]): Reader[K, Tile]
}

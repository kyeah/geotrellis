package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {

  def setFilters(job: Job, layerId: LayerId, filterSet: FilterSet[SpatialKey], index: KeyIndex[SpatialKey]): Unit = {
    var tileBoundSet = false

    for(filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          val ranges =
            for(row <- bounds.rowMin to bounds.rowMax) yield {
              val min =  rowId(layerId, index.toIndex(SpatialKey(bounds.colMin, row)))
              val max = rowId(layerId, index.toIndex(SpatialKey(bounds.colMax, row)))
              new ARange(min, max)
            }

          InputFormatBase.setRanges(job, ranges)
      }
    }

    if (!tileBoundSet) setZoomBounds(job, layerId)

    //Set the filter for layer we need
    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)
  }

  def setZoomBounds(job: Job, layerId: LayerId): Unit = {
    val range = new ARange(
      new Text(f"${layerId.zoom}%02d"),
      new Text(f"${layerId.zoom+1}%02d")
    ) :: Nil

    InputFormatBase.setRanges(job, range)
  }

  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData, keyBounds: KeyBounds[SpatialKey], index: KeyIndex[SpatialKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filters: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val AccumuloLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        InputFormatBase.setInputTableName(job, tileTable)
        setFilters(job, layerId, filters, index)
        val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[BatchAccumuloInputFormat], classOf[Key], classOf[Value])
        val tileRdd =
          rdd.map { case (_, value) =>
            val (key, tileBytes) = KryoSerializer.deserialize[(SpatialKey, Array[Byte])](value.get)
            val tile =
              ArrayTile.fromBytes(
                tileBytes,
                rasterMetaData.cellType,
                rasterMetaData.tileLayout.tileCols,
                rasterMetaData.tileLayout.tileRows
              )

            (key, tile: Tile)
          }

        new RasterRDD(tileRdd, rasterMetaData)
      }
    }
}

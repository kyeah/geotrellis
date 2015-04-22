package geotrellis.spark.io.accumulo

import geotrellis.spark._

package object spatial {
  private[spatial] def rowId(id: LayerId, index: Long): String =
    f"${id.zoom}%02d_${index}%06d"
}

package geotrellis.spark

import geotrellis.raster._

trait SpatialRasterRDDMethods {
  val rdd: RasterRDD[SpatialKey]

  /**
   * Collects and stitches all the tiles in the RasterRDD into one CompositeTile.
   * If a tile is missing from the RDD it will be represented with EmptyTile.
   */
  def stitch: CompositeTile = {
    val tileMap = rdd.collect.toMap

    val rmd = rdd.metaData
    val tileCols = rmd.tileLayout.tileCols
    val tileRows = rmd.tileLayout.tileRows

    // discover what I have here, in reality RasterMetaData should reflect this already
    val te = GridBounds.envelope(tileMap.keys)

    val tiles = te.coords map { case (col, row) =>
      tileMap.getOrElse(col -> row, EmptyTile(rmd.cellType, tileCols, tileRows))
    }
    CompositeTile(tiles, TileLayout(te.width, te.height, tileCols, tileRows))
  }
}

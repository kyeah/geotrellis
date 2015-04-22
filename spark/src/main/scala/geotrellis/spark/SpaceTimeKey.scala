package geotrellis.spark

import geotrellis.spark.io.json._
import monocle._

import org.joda.time.DateTime

import spray.json._
import spray.json.DefaultJsonProtocol._

// TODO: Change this to be col, row, time, and have the compenent keys derived.
case class SpaceTimeKey(col: Int, row: Int, time: DateTime) {
  def spatialKey: SpatialKey = SpatialKey(col, row)
  def temporalKey: TemporalKey = TemporalKey(time)
}

object SpaceTimeKey {
  implicit object SpatialComponent extends SpatialComponent[SpaceTimeKey] {
    def lens =  createLens(k => k.spatialKey, sk => k => SpaceTimeKey(sk.col, sk.row, k.time))
  }

  implicit object TemporalComponent extends TemporalComponent[SpaceTimeKey] {
    def lens = createLens(k => k.temporalKey, tk => k => SpaceTimeKey(k.col, k.row, tk.time))
  }

  implicit def ordering: Ordering[SpaceTimeKey] =
    Ordering.by(stk => (stk.spatialKey, stk.temporalKey))

  def apply(spatialKey: SpatialKey, temporalKey: TemporalKey): SpaceTimeKey =
    SpaceTimeKey(spatialKey.col, spatialKey.row, temporalKey.time)

  implicit object SpaceTimeKeyFormat extends RootJsonFormat[SpaceTimeKey] {
    def write(key: SpaceTimeKey) =
      JsObject(
        "col" -> JsNumber(key.spatialKey.col),
        "row" -> JsNumber(key.spatialKey.row),
        "time" -> key.temporalKey.time.toJson
      )

    def read(value: JsValue): SpaceTimeKey =
      value.asJsObject.getFields("col", "row", "time") match {
        case Seq(JsNumber(col), JsNumber(row), time) =>
          SpaceTimeKey(col.toInt, row.toInt, time.convertTo[DateTime])
        case _ =>
          throw new DeserializationException("SpatialKey expected")
      }
  }
}

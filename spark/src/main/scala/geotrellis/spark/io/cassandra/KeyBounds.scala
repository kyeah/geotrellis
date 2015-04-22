package geotrellis.spark.io.cassandra

import spray.json._
import spray.json.DefaultJsonProtocol._

case class KeyBounds[K](
  minKey: K,
  maxKey: K
)

object KeyBounds {
  implicit def keyBoundsFormat[K: JsonFormat]: RootJsonFormat[KeyBounds[K]] =
    new RootJsonFormat[KeyBounds[K]] {

      def write(keyBounds: KeyBounds[K]) =
        JsObject(
          "minKey" -> keyBounds.minKey.toJson,
          "maxKey" -> keyBounds.maxKey.toJson
        )

      def read(value: JsValue): KeyBounds[K] =
        value.asJsObject.getFields("minKey", "maxKey") match {
          case Seq(minKey, maxKey) =>
            KeyBounds(minKey.convertTo[K], maxKey.convertTo[K])
          case _ =>
            throw new DeserializationException("${classOf[KeyBounds[K]] expected")
        }
    }
}

package org.typelevel.blog

import org.yaml.snakeyaml.Yaml
import scala.collection.JavaConverters._
import scala.util.Try
import shapeless._
import shapeless.labelled.FieldType

trait YAML[T] {
  def rawDecode(any: Any): T

  final def decode(any: Any): Try[T] =
    Try(rawDecode(any))
}

trait LowPriorityYAML {

  implicit def deriveInstance[F, G](implicit gen: LabelledGeneric.Aux[F, G], yg: Lazy[YAML[G]]): YAML[F] =
    new YAML[F] {
      def rawDecode(any: Any) = gen.from(yg.value.rawDecode(any))
    }

}

object YAML extends LowPriorityYAML {

  def apply[T](implicit T: YAML[T]): YAML[T] = T

  def decodeTo[T : YAML](any: Any): Try[T] =
    YAML[T].decode(any)

  implicit def listYAML[T : YAML]: YAML[List[T]] =
    new YAML[List[T]] {
      def rawDecode(any: Any) =
        any.asInstanceOf[java.util.List[_]].asScala.toList.map(YAML[T].rawDecode)
    }

  implicit def stringYAML: YAML[String] =
    new YAML[String] {
      def rawDecode(any: Any) =
        any.asInstanceOf[String]
    }

  implicit def deriveHNil: YAML[HNil] =
    new YAML[HNil] {
      def rawDecode(any: Any) = HNil
    }

  implicit def deriveHCons[K <: Symbol, V, T <: HList]
    (implicit
      key: Witness.Aux[K],
      yv: Lazy[YAML[V]],
      yt: Lazy[YAML[T]]
    ): YAML[FieldType[K, V] :: T] = new YAML[FieldType[K, V] :: T] {
    def rawDecode(any: Any) = {
      val k = key.value.name
      val map = any.asInstanceOf[java.util.Map[String, _]]
      if (!map.containsKey(k))
        throw new IllegalArgumentException(s"key $k not defined")
      val head: FieldType[K, V] = labelled.field(yv.value.rawDecode(map.get(k)))
      val tail = yt.value.rawDecode(map)
      head :: tail
    }
  }

}

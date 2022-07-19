package org.grapheco

import org.grapheco.lynx.types.property.LynxInteger
import org.grapheco.lynx.types.structural.LynxId

//object MyId {
//  def fromLynxId(lynxId: LynxId): MyId = new MyId(lynxId.value.asInstanceOf[Long])
//}
case class MyId(value: Long) extends LynxId {
  override def toLynxInteger: LynxInteger = LynxInteger(value)
}

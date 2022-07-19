package org.grapheco

import org.grapheco.lynx.types.LynxValue
import org.grapheco.lynx.types.structural.{LynxNode, LynxNodeLabel, LynxPropertyKey}

case class MyNode(id: MyId, labels: Seq[LynxNodeLabel], props: Map[LynxPropertyKey, LynxValue]) extends LynxNode {
  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = props.get(propertyKey)

  override def keys: Seq[LynxPropertyKey] = props.keys.toSeq
}

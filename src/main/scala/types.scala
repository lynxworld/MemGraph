import org.grapheco.lynx.{LynxId, LynxNode, LynxNodeLabel, LynxPropertyKey, LynxRelationship, LynxRelationshipType, LynxValue}

//object MyId {
//  def fromLynxId(lynxId: LynxId): MyId = new MyId(lynxId.value.asInstanceOf[Long])
//}
case class MyId(value: Long) extends LynxId

case class MyNode(id: MyId, labels: Seq[LynxNodeLabel], props: Map[LynxPropertyKey, LynxValue]) extends LynxNode{
  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = props.get(propertyKey)
}

case class MyRelationship(id: MyId,
                          startNodeId: MyId,
                          endNodeId: MyId,
                          relationType: Option[LynxRelationshipType],
                          props: Map[LynxPropertyKey, LynxValue]) extends LynxRelationship {
  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = props.get(propertyKey)
}
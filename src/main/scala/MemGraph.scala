import org.grapheco.lynx.{ContextualNodeInputRef, CypherRunner, GraphModel, LynxId, LynxNode, LynxNodeLabel, LynxPropertyKey, LynxRelationship, LynxRelationshipType, LynxResult, LynxValue, NodeInput, NodeInputRef, PathTriple, RelationshipInput, StoredNodeInputRef, WriteTask}
import scala.collection.mutable
import scala.language.implicitConversions

/**
 * @ClassName MemGraph
 * @Description TODO
 * @Author huchuan
 * @Date 2021/12/29
 * @Version 0.1
 */
class MemGraph extends GraphModel {

  private val _nodes: mutable.Map[MyId, MyNode] = mutable.Map()

  private val _relationships: mutable.Map[MyId, MyRelationship] = mutable.Map()

  private var _nodeId: Long = 0

  private var _relationshipId: Long = 0

  private def nodeId: MyId = {_nodeId += 1; MyId(_nodeId)}

  private def relationshipId: MyId = {_relationshipId += 1; MyId(_relationshipId)}

  private def nodeAt(id: LynxId): Option[MyNode] = _nodes.get(id)

  private def relationshipAt(id: LynxId): Option[MyRelationship] = _relationships.get(id)

  implicit def lynxId2myId(lynxId: LynxId): MyId = MyId(lynxId.value.asInstanceOf[Long])

  override def writeTask: WriteTask = _writeTask
  val _writeTask: WriteTask = new WriteTask {

    val _nodesBuffer: mutable.Map[MyId, MyNode] = mutable.Map()

    val _nodesToDelete: mutable.ArrayBuffer[MyId] = mutable.ArrayBuffer()

    val _relationshipsBuffer: mutable.Map[MyId, MyRelationship] = mutable.Map()

    val _relationshipsToDelete: mutable.ArrayBuffer[MyId] = mutable.ArrayBuffer()

    private def updateNodes(ids: Iterator[LynxId], update: MyNode => MyNode): Iterator[Option[LynxNode]] = {
      ids.map{ id =>
          val updated = _nodesBuffer.get(id).orElse(nodeAt(id)).map(update)
          updated.foreach(newNode => _nodesBuffer.update(newNode.id, newNode))
          updated
        }
    }

    private def updateRelationships(ids: Iterator[LynxId], update: MyRelationship => MyRelationship): Iterator[Option[MyRelationship]] = {
      ids.map{ id =>
          val updated = _relationshipsBuffer.get(id).orElse(relationshipAt(id)).map(update)
          updated.foreach(newRel => _relationshipsBuffer.update(newRel.id, newRel))
          updated
        }
    }

    override def createElements[T](nodesInput: Seq[(String, NodeInput)],
                                   relationshipsInput: Seq[(String, RelationshipInput)],
                                   onCreated: (Seq[(String, LynxNode)], Seq[(String, LynxRelationship)]) => T): T = {
      val nodesMap: Map[String, MyNode] = nodesInput.toMap
        .map{case (valueName,input) => valueName -> MyNode(nodeId, input.labels, input.props.toMap)}

      def localNodeRef(ref: NodeInputRef): MyId = ref match {
        case StoredNodeInputRef(id) => id
        case ContextualNodeInputRef(valueName) => nodesMap(valueName).id
      }

      val relationshipsMap: Map[String, MyRelationship] = relationshipsInput.toMap.map{
        case (valueName,input) =>
          valueName -> MyRelationship(relationshipId, localNodeRef(input.startNodeRef),
            localNodeRef(input.endNodeRef), input.types.headOption,input.props.toMap)
      }

      _nodesBuffer ++= nodesMap.map{ case (_, node) => (node.id, node)}
      _relationshipsBuffer ++= relationshipsMap.map{ case (_, relationship) => (relationship.id, relationship)}
      onCreated(nodesMap.toSeq, relationshipsMap.toSeq)
    }

    override def deleteRelations(ids: Iterator[LynxId]): Unit = ids.foreach{ id =>
        _relationshipsBuffer.remove(id)
        _relationshipsToDelete += id
    }

    override def deleteNodes(ids: Seq[LynxId]): Unit = ids.foreach{ id =>
        _nodesBuffer.remove(id)
        _nodesToDelete += id
    }

    override def setNodesProperties(nodeIds: Iterator[LynxId], data: Array[(LynxPropertyKey, Any)], cleanExistProperties: Boolean): Iterator[Option[LynxNode]] =
      updateNodes(nodeIds, old => MyNode(old.id, old.labels, if (cleanExistProperties) Map.empty else old.props ++ data.toMap.mapValues(LynxValue.apply)))

    override def setNodesLabels(nodeIds: Iterator[LynxId], labels: Array[LynxNodeLabel]): Iterator[Option[LynxNode]] =
      updateNodes(nodeIds, old => MyNode(old.id, (old.labels ++ labels.toSeq).distinct, old.props))

    override def setRelationshipsProperties(relationshipIds: Iterator[LynxId], data: Array[(LynxPropertyKey, Any)]): Iterator[Option[LynxRelationship]] =
      updateRelationships(relationshipIds, old => MyRelationship(old.id, old.startNodeId, old.endNodeId, old.relationType, data.toMap.mapValues(LynxValue.apply)))

    override def setRelationshipsType(relationshipIds: Iterator[LynxId], typeName: LynxRelationshipType): Iterator[Option[LynxRelationship]] =
      updateRelationships(relationshipIds, old => MyRelationship(old.id, old.startNodeId, old.endNodeId, Some(typeName), old.props))

    override def removeNodesProperties(nodeIds: Iterator[LynxId], data: Array[LynxPropertyKey]): Iterator[Option[LynxNode]] =
      updateNodes(nodeIds, old => MyNode(old.id, old.labels, old.props.filterNot(data.contains)))

    override def removeNodesLabels(nodeIds: Iterator[LynxId], labels: Array[LynxNodeLabel]): Iterator[Option[LynxNode]] =
      updateNodes(nodeIds, old => MyNode(old.id, old.labels.filterNot(labels.contains), old.props))

    override def removeRelationshipsProperties(relationshipIds: Iterator[LynxId], data: Array[LynxPropertyKey]): Iterator[Option[LynxRelationship]] =
      updateRelationships(relationshipIds, old => MyRelationship(old.id, old.startNodeId, old.endNodeId, old.relationType, old.props.filterNot(data.contains)))

    override def removeRelationshipsType(relationshipIds: Iterator[LynxId], typeName: LynxRelationshipType): Iterator[Option[LynxRelationship]] =
      updateRelationships(relationshipIds, old => MyRelationship(old.id, old.startNodeId, old.endNodeId, None, old.props))

    override def commit: Boolean = {
      _nodes ++= _nodesBuffer
      _nodes --= _nodesToDelete
      _relationships ++= _relationshipsBuffer
      _relationships --= _relationshipsToDelete
      _nodesBuffer.clear()
      _nodesToDelete.clear()
      _relationshipsBuffer.clear()
      _relationshipsToDelete.clear()
      true
    }
  }

  override def nodes(): Iterator[LynxNode] = _nodes.valuesIterator

  override def relationships(): Iterator[PathTriple] =
    _relationships.valuesIterator.map(rel => PathTriple(nodeAt(rel.startNodeId).get, rel, nodeAt(rel.endNodeId).get))

  private val runner = new CypherRunner(this)

  def run(query: String, param: Map[String, Any] = Map.empty[String, Any]): LynxResult = {
//    runner.compile(query)
    runner.run(query, param, None)
  }
}

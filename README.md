# org.grapheco.MemGraph: An in-memory graph database based on Lynx
## Introduction
通过maven引入Lynx
```
<dependency>
    <groupId>org.grapheco</groupId>
    <artifactId>lynx</artifactId>
    <version>0.4.1</version>
</dependency>
```
## Step 1: Base Element
首先需要实现Lynx中的图数据基本元素: LynxNode和LynxRelationship
#### org.grapheco.MyId
LynxId是用Lynx中节点和关系的唯一标识符. 这里我们使用一个Long来表示id.
```
case class org.grapheco.MyId(value: Long) extends LynxId
```
#### org.grapheco.MyNode
```
case class org.grapheco.MyNode(id: org.grapheco.MyId, labels: Seq[LynxNodeLabel], props: Map[LynxPropertyKey, LynxValue]) extends LynxNode{
  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = props.get(propertyKey)
}
```
#### org.grapheco.MyRelationship
```
case class org.grapheco.MyRelationship(id: org.grapheco.MyId,
                          startNodeId: org.grapheco.MyId,
                          endNodeId: org.grapheco.MyId,
                          relationType: Option[LynxRelationshipType],
                          props: Map[LynxPropertyKey, LynxValue]) extends LynxRelationship {
  override def property(propertyKey: LynxPropertyKey): Option[LynxValue] = props.get(propertyKey)
}
```
## Step 2: GraphModel
实现了基本类型之后, 开始实现GraphModel. 定义一个``org.grapheco.MemGraph``
继承Lynx中的``GraphModel``.
实现GraphModel需要实现以下基本方法:
- nodes: 返回一个迭代器, 迭代所有节点
- relationships: 返回一个迭代器, 迭代所有关系
- writeTask: 写操作类, 需要实现以下方法
  - createElements: 创建元素, 包括节点和关系
  - deleteNodes: 根据id删除一批节点
  - deleteRelations: 根据id删除一批关系
  - setNodesLabels: 设置节点标签
  - removeNodesLabels: 移除节点标签
  - setNodesProperties: 设置节点属性
  - removeNodesProperties: 删除节点属性
  - setRelationshipsType: 设置关系类型
  - removeRelationshipsType: 移除关系类型
  - setRelationshipsProperties: 设置关系属性
  - removeRelationshipsProperties: 删除关系属性
  - commit: 提交写操作

以下是具体实现:

首先, 定义两个可变Map用来存储节点和关系, 其中Key为元素的id, 值为该元素.
```
private val _nodes: mutable.Map[org.grapheco.MyId, org.grapheco.MyNode] = mutable.Map()

private val _relationships: mutable.Map[org.grapheco.MyId, org.grapheco.MyRelationship] = mutable.Map()
```
在创建元素时, 需要生成不重复的id. 这里简单地用一个递增的Long变量来记录id, 并提供获取方法.
```
private var _nodeId: Long = 0

private var _relationshipId: Long = 0

private def nodeId: org.grapheco.MyId = {_nodeId += 1; org.grapheco.MyId(_nodeId)}

private def relationshipId: org.grapheco.MyId = {_relationshipId += 1; org.grapheco.MyId(_relationshipId)}
```
为了方便后续操作, 定义两个根据id获取元素的方法:
```
private def nodeAt(id: LynxId): Option[org.grapheco.MyNode] = _nodes.get(id)

private def relationshipAt(id: LynxId): Option[org.grapheco.MyRelationship] = _relationships.get(id)
```
首先实现两个获取全量元素的方法, 如下. 需要注意的是, ``relationships()``返回的
是三元组的迭代, 因此要做一次转换.
```
override def nodes(): Iterator[LynxNode] = _nodes.valuesIterator

override def relationships(): Iterator[PathTriple] =
  _relationships.valuesIterator.map(rel => PathTriple(nodeAt(rel.startNodeId).get, rel, nodeAt(rel.endNodeId).get))
```
接下来实现WriteTask中的方法, 首先定义一组Map用于存放更改, 以及一组Array用于记录需要删除的元素的id.
```
val _nodesBuffer: mutable.Map[org.grapheco.MyId, org.grapheco.MyNode] = mutable.Map()

val _nodesToDelete: mutable.ArrayBuffer[org.grapheco.MyId] = mutable.ArrayBuffer()

val _relationshipsBuffer: mutable.Map[org.grapheco.MyId, org.grapheco.MyRelationship] = mutable.Map()

val _relationshipsToDelete: mutable.ArrayBuffer[org.grapheco.MyId] = mutable.ArrayBuffer()
```
实现创建元素方法: createElements有三个变量，前两个为输入元素，即要create的节点和关系。
onCreated为回调函数，传入创建好的元素。
需要注意的是，关系输入中的起始节点id是一个NodeInputRef类型，包括两种情况，一是该节点的绝对id，
或者该节点是nodesInput中的一个，此时由于还没有id，用name进行定位。这里针对这些情况定义了localNodeRef方法用于区分处理。
```
override def createElements[T](nodesInput: Seq[(String, NodeInput)],
                               relationshipsInput: Seq[(String, RelationshipInput)],
                               onCreated: (Seq[(String, LynxNode)], Seq[(String, LynxRelationship)]) => T): T = {
  val nodesMap: Map[String, org.grapheco.MyNode] = nodesInput.toMap
    .map{case (valueName,input) => valueName -> org.grapheco.MyNode(nodeId, input.labels, input.props.toMap)}

  def localNodeRef(ref: NodeInputRef): org.grapheco.MyId = ref match {
    case StoredNodeInputRef(id) => id
    case ContextualNodeInputRef(valueName) => nodesMap(valueName).id
  }

  val relationshipsMap: Map[String, org.grapheco.MyRelationship] = relationshipsInput.toMap.map{
    case (valueName,input) =>
      valueName -> org.grapheco.MyRelationship(relationshipId, localNodeRef(input.startNodeRef),
        localNodeRef(input.endNodeRef), input.types.headOption,input.props.toMap)
  }
  
  _nodesBuffer ++= nodesMap.map{ case (_, node) => (node.id, node)}
  _relationshipsBuffer ++= relationshipsMap.map{ case (_, relationship) => (relationship.id, relationship)}
  onCreated(nodesMap.toSeq, relationshipsMap.toSeq)
}
```
实现删除方法: 很简单, 就是把该id相关的修改删掉, 并在删除数组里加入这个id
```
override def deleteRelations(ids: Iterator[LynxId]): Unit = ids.foreach{ id =>
    _relationshipsBuffer.remove(id)
    _relationshipsToDelete += id
}

override def deleteNodes(ids: Seq[LynxId]): Unit = ids.foreach{ id =>
    _nodesBuffer.remove(id)
    _nodesToDelete += id
}
```
接下来实现一系列各类元素的标签,类型,属性的增删方法. 这一类方法都是对原始数据的更新,
因此, 这里先定义了一组update方法. 方法接受一组元素id,已经更新函数.
方法会先在Buffer中获取旧数据, 如果没有再去全局数据中找(如果都没有则表示没有这个元素, 无视之).
```
private def updateNodes(ids: Iterator[LynxId], update: org.grapheco.MyNode => org.grapheco.MyNode): Iterator[Option[LynxNode]] = {
  ids.map{ id =>
      val updated = _nodesBuffer.get(id).orElse(nodeAt(id)).map(update)
      updated.foreach(newNode => _nodesBuffer.update(newNode.id, newNode))
      updated
    }
}

private def updateRelationships(ids: Iterator[LynxId], update: org.grapheco.MyRelationship => org.grapheco.MyRelationship): Iterator[Option[org.grapheco.MyRelationship]] = {
  ids.map{ id =>
      val updated = _relationshipsBuffer.get(id).orElse(relationshipAt(id)).map(update)
      updated.foreach(newRel => _relationshipsBuffer.update(newRel.id, newRel))
      updated
    }
}
```
有了这组方法, 后续的一系列实现都较容易:
```
override def setNodesProperties(nodeIds: Iterator[LynxId], data: Array[(LynxPropertyKey, Any)], cleanExistProperties: Boolean): Iterator[Option[LynxNode]] =
  updateNodes(nodeIds, old => org.grapheco.MyNode(old.id, old.labels, if (cleanExistProperties) Map.empty else old.props ++ data.toMap.mapValues(LynxValue.apply)))

override def setNodesLabels(nodeIds: Iterator[LynxId], labels: Array[LynxNodeLabel]): Iterator[Option[LynxNode]] =
  updateNodes(nodeIds, old => org.grapheco.MyNode(old.id, (old.labels ++ labels.toSeq).distinct, old.props))

override def setRelationshipsProperties(relationshipIds: Iterator[LynxId], data: Array[(LynxPropertyKey, Any)]): Iterator[Option[LynxRelationship]] =
  updateRelationships(relationshipIds, old => org.grapheco.MyRelationship(old.id, old.startNodeId, old.endNodeId, old.relationType, data.toMap.mapValues(LynxValue.apply)))

override def setRelationshipsType(relationshipIds: Iterator[LynxId], typeName: LynxRelationshipType): Iterator[Option[LynxRelationship]] =
  updateRelationships(relationshipIds, old => org.grapheco.MyRelationship(old.id, old.startNodeId, old.endNodeId, Some(typeName), old.props))

override def removeNodesProperties(nodeIds: Iterator[LynxId], data: Array[LynxPropertyKey]): Iterator[Option[LynxNode]] =
  updateNodes(nodeIds, old => org.grapheco.MyNode(old.id, old.labels, old.props.filterNot(data.contains)))

override def removeNodesLabels(nodeIds: Iterator[LynxId], labels: Array[LynxNodeLabel]): Iterator[Option[LynxNode]] =
  updateNodes(nodeIds, old => org.grapheco.MyNode(old.id, old.labels.filterNot(labels.contains), old.props))

override def removeRelationshipsProperties(relationshipIds: Iterator[LynxId], data: Array[LynxPropertyKey]): Iterator[Option[LynxRelationship]] =
  updateRelationships(relationshipIds, old => org.grapheco.MyRelationship(old.id, old.startNodeId, old.endNodeId, old.relationType, old.props.filterNot(data.contains)))

override def removeRelationshipsType(relationshipIds: Iterator[LynxId], typeName: LynxRelationshipType): Iterator[Option[LynxRelationship]] =
  updateRelationships(relationshipIds, old => org.grapheco.MyRelationship(old.id, old.startNodeId, old.endNodeId, None, old.props))
```
最后是commit方法, 主要是把Buffer和Delete中的操作并入存储数据.
```
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
```
## Step 3: Runner
实现了GraphModel之后, 只需要创建一个CypherRunner, 并传入实现的GraphModel即可支持通过Cypher进行图查询.
```
private val runner = new CypherRunner(this)

def run(query: String, param: Map[String, Any] = Map.empty[String, Any]): LynxResult = {
  // runner.compile(query)
  runner.run(query, param, None)
}
```
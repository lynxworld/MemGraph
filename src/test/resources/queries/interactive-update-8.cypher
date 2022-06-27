MATCH (p1:Person {id: 1}), (p2:Person {id: 2})
CREATE (p1)-[:KNOWS {creationDate: 1277856000000}]->(p2)

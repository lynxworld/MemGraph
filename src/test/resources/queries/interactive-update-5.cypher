MATCH (f:Forum {id: 1}), (p:Person {id: 2})
CREATE (f)-[:HAS_MEMBER {joinDate: 1277856000000}]->(p)
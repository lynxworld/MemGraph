MATCH (c:City {id: 1})
CREATE (p:Person {
    id: 2,
    firstName: "zhang",
    lastName: "san",
    gender: "male",
    birthday: "1231312313",
    creationDate: "12030303303",
    locationIP: "192.168.1.1",
    browserUsed: "asdad",
    languages: "CN",
    email: "123@qq.com"
  })-[:IS_LOCATED_IN]->(c)
WITH p, count(*) AS dummy1
UNWIND [1,2,3] AS tagId
  MATCH (t:Tag {id: tagId})
  CREATE (p)-[:HAS_INTEREST]->(t)
WITH p, count(*) AS dummy2
UNWIND ["sad","chv"] AS s
  MATCH (u:Organisation {id: s[0]})
  CREATE (p)-[:STUDY_AT {classYear: s[1]}]->(u)
WITH p, count(*) AS dummy3
UNWIND ["asdr","icic"] AS w
  MATCH (comp:Organisation {id: w[0]})
  CREATE (p)-[:WORKS_AT {workFrom: w[1]}]->(comp)

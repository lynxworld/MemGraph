// Q4. New topics
/*
:param [{ personId, startDate, endDate }] => { RETURN
  4398046511333 AS personId,
  1275350400000 AS startDate,
  1277856000000 AS endDate
}
*/
MATCH (person:Person {id: 4398046511333 })-[:KNOWS]-(friend:Person),
      (friend)<-[:HAS_CREATOR]-(post:Post)-[:HAS_TAG]->(tag)
WITH DISTINCT tag, post
WITH tag,
     CASE
       WHEN 1277856000000 > post.creationDate >= 1275350400000 THEN 1
       ELSE 0
     END AS valid,
     CASE
       WHEN 1275350400000 > post.creationDate THEN 1
       ELSE 0
     END AS inValid
WITH tag, sum(valid) AS postCount, sum(inValid) AS inValidPostCount
WHERE postCount>0 AND inValidPostCount=0
RETURN tag.name AS tagName, postCount
ORDER BY postCount DESC, tagName ASC
LIMIT 10

// Q3. Friends and friends of friends that have been to given countries
/*
:param [{ personId, countryXName, countryYName, startDate, endDate }] => { RETURN
  6597069766734 AS personId,
  "Angola" AS countryXName,
  "Colombia" AS countryYName,
  1275393600000 AS startDate,
  1277812800000 AS endDate
}
*/
MATCH (countryX:Country {name: "Angola" }),
      (countryY:Country {name: "Colombia" }),
      (person:Person {id: 6597069766734 })
WITH person, countryX, countryY
LIMIT 1
MATCH (city:City)-[:IS_PART_OF]->(country:Country)
WHERE country IN [countryX, countryY]
WITH person, countryX, countryY, collect(city) AS cities
MATCH (person)-[:KNOWS*1..2]-(friend)-[:IS_LOCATED_IN]->(city)
WHERE NOT person=friend AND NOT city IN cities
WITH DISTINCT friend, countryX, countryY
MATCH (friend)<-[:HAS_CREATOR]-(message),
      (message)-[:IS_LOCATED_IN]->(country)
WHERE 1277812800000 > message.creationDate >= 1275393600000 AND
      country IN [countryX, countryY]
WITH friend,
     CASE WHEN country=countryX THEN 1 ELSE 0 END AS messageX,
     CASE WHEN country=countryY THEN 1 ELSE 0 END AS messageY
WITH friend, sum(messageX) AS xCount, sum(messageY) AS yCount
WHERE xCount>0 AND yCount>0
RETURN friend.id AS friendId,
       friend.firstName AS friendFirstName,
       friend.lastName AS friendLastName,
       xCount,
       yCount,
       xCount + yCount AS xyCount
ORDER BY xyCount DESC, friendId ASC
LIMIT 20

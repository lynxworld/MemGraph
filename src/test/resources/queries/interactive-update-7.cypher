MATCH
  (author:Person {id: 1}),
  (country:Country {id: 2}),
  (message:Message {id: 3 + 4 + 1}) // $replyToCommentId is -1 if the message is a reply to a post and vica versa (see spec)
CREATE (author)<-[:HAS_CREATOR]-(c:Comment:Message {
    id: 5,
    creationDate: 1277856000000,
    locationIP: "192.168.1.1",
    browserUsed: "asda",
    content: "content",
    length: 123
  })-[:REPLY_OF]->(message),
  (c)-[:IS_LOCATED_IN]->(country)
WITH c
UNWIND [1,2,3] AS tagId
  MATCH (t:Tag {id: tagId})
  CREATE (c)-[:HAS_TAG]->(t)
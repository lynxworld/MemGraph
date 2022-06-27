// Q2. Recent messages by your friends
/*
:param [{ personId, maxDate }] => { RETURN
  10995116278009 AS personId,
  1287230400000 AS maxDate
}
*/
MATCH (:Person {id: 10995116278009 })-[:KNOWS]-(friend:Person)<-[:HAS_CREATOR]-(message:Message)
    WHERE message.creationDate <= 1287230400000
    RETURN
        friend.id AS personId,
        friend.firstName AS personFirstName,
        friend.lastName AS personLastName,
        message.id AS postOrCommentId,
        coalesce(message.content,message.imageFile) AS postOrCommentContent,
        message.creationDate AS postOrCommentCreationDate
    ORDER BY
        postOrCommentCreationDate DESC,
        toInteger(postOrCommentId) ASC
    LIMIT 20

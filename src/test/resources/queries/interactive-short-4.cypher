// IS4. Content of a message
/*
:param messageId: 206158431836
 */
MATCH (m:Message {id:  206158431836 })
RETURN
    m.creationDate as messageCreationDate,
    coalesce(m.content, m.imageFile) as messageContent

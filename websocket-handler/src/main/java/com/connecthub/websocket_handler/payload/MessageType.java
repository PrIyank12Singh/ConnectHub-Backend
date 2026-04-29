package com.ConnectHub.websocket_handler.payload;

/**
 * All inbound STOMP frame types handled by ChatWebSocketHandler.
 * Matches the WebSocket Payload Types table in PDF Section 4.7.
 */
public enum MessageType {
    CHAT_MESSAGE,       // senderId, roomId, content, type, replyToId
    TYPING_INDICATOR,   // senderId, roomId, isTyping
    READ_RECEIPT,       // readerId, roomId, upToMessageId
    REACTION,           // senderId, messageId, emoji
    PRESENCE_UPDATE,    // userId, status, customMessage
    MESSAGE_EDIT,       // editorId, messageId, newContent
    MESSAGE_DELETE,     // deleterId, messageId
    ROOM_JOIN,          // userId, roomId
    ROOM_LEAVE,         // userId, roomId
    PING                // sessionId — keep-alive from client
}

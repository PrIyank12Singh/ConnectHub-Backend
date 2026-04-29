package com.ConnectHub.notification_service.model;

public enum NotificationType {
    NEW_MESSAGE,    // new message in a room where user is offline
    MENTION,        // @mention in a message
    ROOM_INVITE,    // invited to join a group room
    SYSTEM          // platform-wide broadcast from admin
}

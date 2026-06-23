package com.telegram.storage.dto;

public enum StorageFolder {

    STORIES("stories"),
    CHATS("chats"),
    USERS("users"),
    GROUPS("groups");

    private final String folder;

    StorageFolder(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }
}

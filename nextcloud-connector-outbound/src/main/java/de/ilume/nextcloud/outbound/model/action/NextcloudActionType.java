package de.ilume.nextcloud.outbound.model.action;

public enum NextcloudActionType {
    COPY_FILE(Constants.COPY_FILE),
    CREATE_FOLDER(Constants.CREATE_FOLDER),
    CREATE_NEW_SHARE(Constants.CREATE_NEW_SHARE),
    DELETE_FILE(Constants.DELETE_FILE),
    DOWNLOAD_FILE(Constants.DOWNLOAD_FILE),
    LISTING_FOLDERS(Constants.LISTING_FOLDERS),
    MOVE_FILE(Constants.MOVE_FILE),
    UPLOAD_FILE(Constants.UPLOAD_FILE);

    private final String id;

    NextcloudActionType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static class Constants {
        public static final String COPY_FILE = "copyFile";
        public static final String CREATE_FOLDER = "createFolder";
        public static final String CREATE_NEW_SHARE = "createNewShare";
        public static final String DELETE_FILE = "deleteFile";
        public static final String DOWNLOAD_FILE = "downloadFile";
        public static final String LISTING_FOLDERS = "listingFolders";
        public static final String MOVE_FILE = "moveFile";
        public static final String UPLOAD_FILE = "uploadFile";
    }
}
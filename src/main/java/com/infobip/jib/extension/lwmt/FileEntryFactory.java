package com.infobip.jib.extension.lwmt;

import com.google.cloud.tools.jib.api.buildplan.FileEntry;

import java.time.Instant;

class FileEntryFactory {

    static FileEntry createWithModificationTime(FileEntry entry, Instant modificationTime) {
        return new FileEntry(entry.getSourceFile(),
                             entry.getExtractionPath(),
                             entry.getPermissions(),
                             modificationTime,
                             entry.getOwnership());
    }
}

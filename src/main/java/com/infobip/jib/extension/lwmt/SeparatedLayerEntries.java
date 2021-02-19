package com.infobip.jib.extension.lwmt;

import com.google.cloud.tools.jib.api.buildplan.FileEntry;

import java.util.List;

class SeparatedLayerEntries {

    private final List<FileEntry> retainedOriginalLayerEntries;
    private final List<FileEntry> layerWithModificationTimeEntries;

    SeparatedLayerEntries(List<FileEntry> retainedOriginalLayerEntries,
                          List<FileEntry> layerWithModificationTimeEntries) {
        this.retainedOriginalLayerEntries = retainedOriginalLayerEntries;
        this.layerWithModificationTimeEntries = layerWithModificationTimeEntries;
    }

    List<FileEntry> getRetainedOriginalLayerEntries() {
        return retainedOriginalLayerEntries;
    }

    List<FileEntry> getLayerWithModificationTimeEntries() {
        return layerWithModificationTimeEntries;
    }
}

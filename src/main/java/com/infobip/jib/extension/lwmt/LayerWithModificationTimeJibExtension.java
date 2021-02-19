package com.infobip.jib.extension.lwmt;

import com.google.cloud.tools.jib.api.buildplan.*;
import com.google.cloud.tools.jib.maven.extension.JibMavenPluginExtension;
import com.google.cloud.tools.jib.maven.extension.MavenData;
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class LayerWithModificationTimeJibExtension implements JibMavenPluginExtension<Configuration> {

    static final String LAYER_WITH_MODIFICATION_TIME_NAME = "layerWithModificationTime";

    @Override
    public Optional<Class<Configuration>> getExtraConfigType() {
        return Optional.of(Configuration.class);
    }

    @Override
    public ContainerBuildPlan extendContainerBuildPlan(ContainerBuildPlan containerBuildPlan,
                                                       Map<String, String> properties,
                                                       Optional<Configuration> extraConfig,
                                                       MavenData mavenData,
                                                       ExtensionLogger extensionLogger) {
        return extraConfig.filter(configuration -> !configuration.isEmpty())
                          .map(configuration -> extendContainerBuildPlan(configuration, containerBuildPlan, mavenData))
                          .orElse(containerBuildPlan);
    }

    private ContainerBuildPlan extendContainerBuildPlan(Configuration extraConfig,
                                                        ContainerBuildPlan containerBuildPlan,
                                                        MavenData mavenData) {
        var planBuilder = containerBuildPlan.toBuilder().setLayers(new ArrayList<>());
        var newLayerEntryMatchers = getPathMatchersForNewLayerEntries(extraConfig);
        var originalLayers = (List<FileEntriesLayer>) containerBuildPlan.getLayers();
        adjustAndAddLayersToPlan(originalLayers, mavenData, newLayerEntryMatchers, planBuilder);

        return planBuilder.build();
    }

    private List<PathMatcher> getPathMatchersForNewLayerEntries(Configuration configuration) {
        return configuration
                .getFilters()
                .stream()
                .map(filter -> FileSystems.getDefault().getPathMatcher("glob:" + filter))
                .collect(Collectors.toList());
    }

    private void adjustAndAddLayersToPlan(List<FileEntriesLayer> originalLayers,
                                          MavenData mavenData,
                                          List<PathMatcher> newLayerEntryMatchers,
                                          ContainerBuildPlan.Builder planBuilder) {
        var layerWithModificationTimeEntries = new ArrayList<FileEntry>();

        originalLayers.forEach(layer -> {
            var separatedEntries = separateEntriesFrom(layer, newLayerEntryMatchers);
            addLayerToPlan(layer.getName(), separatedEntries.getRetainedOriginalLayerEntries(), planBuilder);
            layerWithModificationTimeEntries.addAll(separatedEntries.getLayerWithModificationTimeEntries());
        });

        addLayerWithModificationTime(mavenData, layerWithModificationTimeEntries, planBuilder);
    }

    private SeparatedLayerEntries separateEntriesFrom(FileEntriesLayer layer,
                                                      List<PathMatcher> layerWithModificationTimeEntryPathMatchers) {
        var retainedOriginalLayerEntries = new ArrayList<FileEntry>();
        var layerWithModificationTimeEntries = new ArrayList<FileEntry>();

        for (FileEntry fileEntry : layer.getEntries()) {
            if (shouldBeMovedToLayerWithModificationTime(fileEntry, layerWithModificationTimeEntryPathMatchers)) {
                layerWithModificationTimeEntries.add(fileEntry);
            } else {
                retainedOriginalLayerEntries.add(fileEntry);
            }
        }

        return new SeparatedLayerEntries(retainedOriginalLayerEntries, layerWithModificationTimeEntries);
    }

    private boolean shouldBeMovedToLayerWithModificationTime(FileEntry fileEntry,
                                                             List<PathMatcher> modifiedLayerEntryPathMatchers) {
        return modifiedLayerEntryPathMatchers.stream()
                                             .anyMatch(pathMatcher -> pathMatcher.matches(
                                                     Paths.get(fileEntry.getExtractionPath().toString())));
    }

    private void addLayerToPlan(String layerName, List<FileEntry> entries, ContainerBuildPlan.Builder planBuilder) {
        Optional.of(entries)
                .filter(list -> !list.isEmpty())
                .map(list -> FileEntriesLayer.builder()
                                             .setName(layerName)
                                             .setEntries(list)
                                             .build())
                .ifPresent(planBuilder::addLayer);
    }

    private void addLayerWithModificationTime(MavenData mavenData,
                                              List<FileEntry> modifiedLayerEntries,
                                              ContainerBuildPlan.Builder planBuilder) {
        var buildTime = mavenData.getMavenSession().getStartTime().toInstant();
        var entries = modifiedLayerEntries.stream()
                                          .map(entry -> FileEntryFactory.createWithModificationTime(entry, buildTime))
                                          .collect(Collectors.toList());
        addLayerToPlan(LAYER_WITH_MODIFICATION_TIME_NAME, entries, planBuilder);
    }
}

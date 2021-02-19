package com.infobip.jib.extension.lwmt;

import com.google.cloud.tools.jib.api.buildplan.*;
import com.google.cloud.tools.jib.maven.extension.MavenData;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.infobip.jib.extension.lwmt.LayerWithModificationTimeJibExtension.LAYER_WITH_MODIFICATION_TIME_NAME;
import static org.assertj.core.api.BDDAssertions.then;

class LayerWithModificationTimeJibExtensionTest {

    private static final Instant LAST_MODIFICATION_TIME = Instant.ofEpochSecond(9_999_999);

    @Test
    void shouldMoveToModifiedLayer() {
        // given
        var givenConfiguration = givenConfiguration("**/a.*");
        var givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        var givenExtension = new LayerWithModificationTimeJibExtension();
        var givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<>() {{
                    put("layer1", List.of("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
                    put("layer2", List.of("/layer2/d.file", "/layer2/e.file"));
                    put("layer3", List.of("/layer3/f.file"));
                }}
        );

        // when
        var actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             givenMavenData, null);

        // then
        var expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", List.of("/layer1/b.file", "/layer1/c.file"));
            put("layer2", List.of("/layer2/d.file", "/layer2/e.file"));
            put("layer3", List.of("/layer3/f.file"));
        }};
        var expectedModifiedLayerFiles = List.of("/layer1/a.file");
        var expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldMoveMultipleToModifiedLayer() {
        // given
        var givenConfiguration = new Configuration(List.of("**/a.file", "**/d.file"));
        var givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        var givenExtension = new LayerWithModificationTimeJibExtension();
        var givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<>() {{
                    put("layer1", List.of("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
                    put("layer2", List.of("/layer2/d.file", "/layer2/e.file"));
                    put("layer3", List.of("/layer3/f.file"));
                }}
        );

        // when
        var actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             givenMavenData, null);

        // then
        var expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", List.of("/layer1/b.file", "/layer1/c.file"));
            put("layer2", List.of("/layer2/e.file"));
            put("layer3", List.of("/layer3/f.file"));
        }};
        var expectedModifiedLayerFiles = List.of("/layer1/a.file", "/layer2/d.file");
        var expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldRemoveEmptyLayer() {
        // given
        var givenConfiguration = givenConfiguration("**/b.file");
        var givenMavenData = givenMavenData(LAST_MODIFICATION_TIME);
        var givenExtension = new LayerWithModificationTimeJibExtension();
        var givenBuildPlan = createContainerBuildPlan(
                new LinkedHashMap<>() {{
                    put("layer1", List.of("/data/a.file"));
                    put("layer2", List.of("/data/b.file"));
                }}
        );

        // when
        var actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             givenMavenData, null);

        // then
        var expectedLayerMap = new LinkedHashMap<String, List<String>>() {{
            put("layer1", List.of("/data/a.file"));
        }};
        var expectedModifiedLayerFiles = List.of("/data/b.file");
        var expected = createContainerBuildPlan(expectedLayerMap, expectedModifiedLayerFiles);
        then(actual).usingRecursiveComparison()
                    .isEqualTo(expected);
    }

    @Test
    void shouldDoNoModificationsWhenNoConfiguration() {
        // given
        var givenBuildPlan = givenDefaultBuildPlan();
        var givenExtension = new LayerWithModificationTimeJibExtension();

        // when
        var actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.empty(), null, null);

        // then
        then(actual).isEqualTo(givenBuildPlan);
    }

    @Test
    void shouldDoNoModificationsWhenNoFilters() {
        // given
        var givenBuildPlan = ContainerBuildPlan.builder().build();
        var givenExtension = new LayerWithModificationTimeJibExtension();
        var givenConfiguration = new Configuration();

        // when
        var actual = givenExtension.extendContainerBuildPlan(givenBuildPlan, null, Optional.of(givenConfiguration),
                                                             null, null);

        // then
        then(actual).isEqualTo(givenBuildPlan);
    }

    private ContainerBuildPlan givenDefaultBuildPlan() {
        new LinkedHashMap<String, List<String>>() {{
            put("layer1", List.of("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
            put("layer2", List.of("/layer2/d.file", "/layer2/e.file"));
            put("layer3", List.of("/layer3/f.file"));
        }};

        var map = new LinkedHashMap<String, List<String>>();
        map.put("layer1", List.of("/layer1/a.file", "/layer1/b.file", "/layer1/c.file"));
        map.put("layer2", List.of("/layer2/d.file", "/layer2/e.file"));
        map.put("layer3", List.of("/layer3/f.file"));
        return createContainerBuildPlan(map, List.of());
    }

    private ContainerBuildPlan createContainerBuildPlan(LinkedHashMap<String, List<String>> layersToFilePaths) {
        return createContainerBuildPlan(layersToFilePaths, List.of());
    }

    private ContainerBuildPlan createContainerBuildPlan(LinkedHashMap<String, List<String>> layersToFilePaths,
                                                        List<String> modifiedPaths) {
        var containerBuildPlanBuilder = ContainerBuildPlan.builder();

        for (var layerData : layersToFilePaths.entrySet()) {
            var layerBuilder = FileEntriesLayer.builder().setName(layerData.getKey());
            layerData.getValue()
                     .stream()
                     .filter(path -> !modifiedPaths.contains(path))
                     .map(Paths::get)
                     .map(this::givenFileEntry)
                     .forEach(layerBuilder::addEntry);
            containerBuildPlanBuilder.addLayer(layerBuilder.build());
        }

        if (!modifiedPaths.isEmpty()) {
            var modifiedEntries = modifiedPaths.stream()
                                               .map(path -> givenFileEntry(Paths.get(path), LAST_MODIFICATION_TIME))
                                               .collect(Collectors.toList());
            var modifiedLayer = FileEntriesLayer.builder()
                                                .setName(LAYER_WITH_MODIFICATION_TIME_NAME)
                                                .setEntries(modifiedEntries)
                                                .build();
            containerBuildPlanBuilder.addLayer(modifiedLayer);
        }

        return containerBuildPlanBuilder.build();
    }

    private FileEntry givenFileEntry(Path path) {
        return givenFileEntry(path, Instant.EPOCH);
    }

    private FileEntry givenFileEntry(Path path, Instant modificationTime) {
        return new FileEntry(path, AbsoluteUnixPath.fromPath(path), FilePermissions.DEFAULT_FILE_PERMISSIONS,
                             modificationTime, "owner");
    }

    private Configuration givenConfiguration(String filter) {
        return new Configuration(List.of(filter));
    }

    private MavenData givenMavenData(Instant instant) {
        var mavenSession = Mockito.mock(MavenSession.class);
        BDDMockito.given(mavenSession.getStartTime()).willReturn(new Date(instant.toEpochMilli()));
        var mavenData = Mockito.mock(MavenData.class);
        BDDMockito.given(mavenData.getMavenSession()).willReturn(mavenSession);
        return mavenData;
    }
}

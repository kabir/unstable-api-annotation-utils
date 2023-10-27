package org.wildfly.experimental.api.classpath.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.wildfly.experimental.api.classpath.indexer.JarAnnotationIndexer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name="index-experimental-annotations", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ExperimentalAnnotationMojo
        extends AbstractMojo
{

    @Parameter(property = "filters", required = true)
    List<Filter> indexFilters = new ArrayList<>();

    @Parameter(property = "outputFile", required = true)
    File outputFile;

    @Component
    MavenProject mavenProject;

    Set<String> foundClasses = new HashSet<>();

    public void execute() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Running plugin");

            log.info(indexFilters.toString());

            List<Dependency> dependencies = mavenProject.getDependencies();

            Set<String> allGroupIds = new HashSet<>();
            for (Filter indexFilter : indexFilters) {
                allGroupIds.addAll(indexFilter.getGroupIds());
            }

            for (Artifact artifact : mavenProject.getArtifacts()) {
                // log.info(artifact.getGroupId() + ":" + artifact.getArtifactId());
                if (artifact.getType().equals("jar") && allGroupIds.contains(artifact.getGroupId())) {
                    searchExperimentalAnnotation(artifact);
                }
            }

            Path path = Paths.get(outputFile.toURI());
            Files.createDirectories(path.getParent());
            Files.write(path, foundClasses);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void searchExperimentalAnnotation(Artifact artifact) throws IOException {
        for (Filter indexFilter : indexFilters) {
            if (indexFilter.getGroupIds().contains(artifact.getGroupId())) {
                searchExperimentalAnnotation(artifact, indexFilter);
            }
        }
    }

    private void searchExperimentalAnnotation(Artifact artifact, Filter indexFilter) throws IOException {
        JarAnnotationIndexer indexer = new JarAnnotationIndexer(artifact.getFile(), indexFilter.getAnnotation(), indexFilter.getExcludedClasses());
        foundClasses.addAll(indexer.scanForAnnotation().getAnnotatedAnnotations());
    }
}

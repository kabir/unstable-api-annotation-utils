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
import org.wildfly.experimental.api.classpath.index.OverallIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Mojo(name="index-experimental-annotations", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class ExperimentalAnnotationMojo
        extends AbstractMojo
{

    @Parameter(property = "filters", required = true)
    private List<Filter> filters = new ArrayList<>();

    @Parameter(property = "outputFile", required = true)
    private File outputFile;

    @Component
    private MavenProject mavenProject;

    OverallIndex overallIndex;

    public void execute() throws MojoExecutionException {
        try {
            Log log = getLog();
            log.info("Running plugin");
            overallIndex = new OverallIndex();

            log.info(filters.toString());

            List<Dependency> dependencies = mavenProject.getDependencies();

            for (Filter indexFilter : filters) {
                Set<String> allGroupIds = new HashSet<>();
                Set<Pattern> wildcardGroupIds = new HashSet<>();
                for (String id : indexFilter.getGroupIds()) {
                    if (id.contains("*")) {
                        wildcardGroupIds.add(createPattern(id));
                    } else {
                        allGroupIds.add(id);
                    }
                }

                for (Artifact artifact : mavenProject.getArtifacts()) {
                    log.info(artifact.getGroupId() + ":" + artifact.getArtifactId());
                    if (artifact.getType().equals("jar")) {
                        if (allGroupIds.contains(artifact.getGroupId())) {
                            overallIndex.scanJar(artifact.getFile(), indexFilter.getAnnotation(), indexFilter.getExcludedClasses());
                        } else {
                            for (Pattern pattern : wildcardGroupIds) {
                                if (pattern.matcher(artifact.getGroupId()).matches()) {
                                    overallIndex.scanJar(artifact.getFile(), indexFilter.getAnnotation(), indexFilter.getExcludedClasses());
                                }
                            }
                        }
                    }
                }
            }


            Path path = Paths.get(outputFile.toURI());
            overallIndex.save(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pattern createPattern(String s) {
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '.') {
                builder.append('\\');
            } else {
                if (c == '*') {
                    builder.append('.');
                }
            }
            builder.append(c);
        }
        return Pattern.compile(builder.toString());
    }
}

package org.wildfly.experimental.api.classpath.plugin;

import java.util.HashSet;
import java.util.Set;

/**
 * This is API exposed by the plugin
 */
public class Filter {
    // The annotation to search for
    private String annotation;

    // Group ids to look for the annotation inside
    private Set<String> groupIds = new HashSet<>();

    private Set<String> excludedClasses = new HashSet<>();


    public Filter(String annotation, Set<String> groupIds, Set<String> excludedClasses) {
        this.annotation = annotation;
        this.groupIds = groupIds;
        this.excludedClasses = excludedClasses;
    }

    public Filter() {
        this.annotation = annotation;
        this.groupIds = groupIds;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Set<String> groupIds) {
        this.groupIds = groupIds;
    }

    public Set<String> getExcludedClasses() {
        return excludedClasses;
    }

    public void setExcludedClasses(Set<String> excludedClasses) {
        this.excludedClasses = excludedClasses;
    }

    @Override
    public String toString() {
        return "Filter{" +
                "annotation='" + annotation + '\'' +
                ", groupIds=" + groupIds +
                '}';
    }
}
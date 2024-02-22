package org.wildfly.unstable.api.annotation.test.api.unscanned;

import org.wildfly.unstable.api.annotation.test.api.MarkerAnnotation;

/**
 * This class is in a Maven module that does not have a matching group id
 * to what the plugin scans for
 */
@MarkerAnnotation
public class UnscannedClass {
}

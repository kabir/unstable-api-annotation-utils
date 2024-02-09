# Unstable API Annotation Utilities

Some Java libraries use an annotation to pick out parts of their API that is considered 
unstable. This gives a signal to users that the class, method, field etc. annotated 
with this annotation might go away in the future, or have its contract changed significantly.

For further discussion in the README, we will call these annotations 'marker annotations'

The aim of this project is to provide a toolkit to:
* Inspect the classpath of your applications, searching for the marker annotations and create an index of locations of where the annotations are used
* Scan the user (i.e. your ) code to see if you are using something annotated with such an annotation. This could be at runtime, or as part of a compile time code check. We provide the tools, you can choose how to use it! 

## Example
Say there is an API from some external vendor that we want to consume in our application and a marker annotation called `@Experimental`, and then this is used in a few places in our API.

```java
@Experimental
public class APIObjectFactory {

    public APIObjectFactory getInstance() {
        return new APIObjectFactory();
    }
    
    public Person createNewPerson(String name, String address, String postcode, Date dateOfBirth) {
        // ....
    }
}
```
and

```java

public class APIInterface {

    public void processPerson(String name, String address, String postcode, Date dateOfBirth);

    // At some stage someone decided to introduce a Person class, but are not sure this is the correct way.
    @Experimental
    public void processPerson(Person person);
}
```
Now there are two ways to process a person in our code, which consumes this API. The existing and stable way would be something like:

```java
import javax.inject.Inject;

public class MyClass {
    @Inject
    APIInterface api;
    
    public void myPersonMethod(String name, String address, String postcode, Date dateOfBirth) {
        api.processPerson(name, address, postcode, dateOfBirth);
    }
}
```
The above is fine. However, say this is a new project, and we have not used the API before, 
we might see and prefer the 'cleaner' approach where all the information about the Person
is wrapped in the Person class. Code completion in IDEs make it very easy to find classes
and methods, without the developer necessarily checking the code of what they are calling.

This could look like
```java
import javax.inject.Inject;

public class MyOtherClass {
    @Inject
    APIInterface api;

    public void myPersonMethod(String name, String address, String postcode, Date dateOfBirth) {
        Person person = APIObjectFactory.getInstance()
                .createNewPerson(name, address, postcode, dateOfBirth);
        api.processPerson(person);
    }
}
```
To summarise, in the last example we are using two things from the API, annotated with the '@Experimental' marker annotation:
* The `APIObjectFactory` class
* The `APIInterface.processPerson(Person)` method

Next we will look at the indexing of members in the API that have been annotated with marker
annotations, and then we will see how to detect classes in our code that have been annotated with 
it.

## Creating the index

The below code creates the index. You need to specify each jar that is consumed by your application
and then it scans each jar for each annotation we are 

```java
import java.io.File;

public class IndexCreator {
    // Annotation names here should include the package name
    // Different libraries will have different marker annotations
    private static final String[] ANNOTATIONS = new String[]{"@Experimental", "@Unstable"};
    
    public static void main(String[] args) {
        OverallIndex overallIndex = new OverallIndex();
        for (String arg : args) {
            File file = new File(arg);
            for (String annotation : ANNOTATIONS) {
                overallIndex.scanJar(file, annotation);    
            }
        }
        // Save the index to a file
        Path p = Paths.get("target/index/index.txt");
        overallIndex.save(p);
    }
}
```

The index file will be saved to `target/index/index.txt`, and is in plain text in a human-readable format.


### Maven Plugin
Of course having to programmatically list all the jars on the build classpath as in the above example would 
be cumbersome.

We provide a Maven plugin wrapping the above code, so you can generate the index automatically when building your project. In [WildFly](https://github.com/wildfly/wildfly) we do this as part of the build of the full server, and an example of the configuration can be seen here:

```xml
<plugin>
    <groupId>org.wildfly.unstable.api.annotation</groupId>
    <artifactId>unstable-api-annotation-classpath-indexer-plugin</artifactId>
    <configuration>
        <outputFile>${project.build.directory}/index/wildfly-galleon-pack.txt</outputFile>
        <filters>
            <filter>
                <annotation>io.smallrye.common.annotation.Experimental</annotation>
                <groupIds>
                    <groupId>io.smallrye</groupId>
                    <groupId>io.smallrye.*</groupId>
                </groupIds>
                <excludedClasses>
                    <excludedClass>org.eclipse.microprofile.reactive.messaging.Channel</excludedClass>
                    <excludedClass>org.eclipse.microprofile.reactive.messaging.Incoming</excludedClass>
                </excludedClasses>
            </filter>
        </filters>
    </configuration>
</plugin>
```
Essentially this will do the same as the above Java example, but scan every single jar on the classpath of the Maven module. 
In this case we are looking for the `io.smallrye.common.annotation.Experimental` in jars who have a groupId 
starting of `io.smallrye`, or one of its children.

We are excluding the `Channel` and `Incoming` classes since they have some known issues, and outputting 
the index file to `${project.build.directory}/index/wildfly-galleon-pack.txt`.

## Scan the user code

To see if any of the code you have written makes use of API code which in turn has been annotated with 
one of the marker annotations we provide a highly optimised `RuntimeIndex` and `ClassInfoScanner`. 
In WildFly we inspect each class that is the part of the archive a user wants to deploy 
during the deployment process and cross-reference what the classes use/call with the runtime index. 

In the future we might provide a Maven plugin to help do this as a compile-time check.

However, the API is simple enough that you could write your own tooling to do this.

The following example shows an example of how to do these checks.

```java

// Load the index we created earlier
Path p = Paths.get("target/index/index.txt");
RuntimeIndex runtimeIndex = RuntimeIndex.load(p);

// Instantiate the scanner 
ClassInfoScanner scanner = new ClassInfoScanner(runtimeIndex);

// Get InputStreams for each user class somehow. In this example by loading each class file (not shown)
List<File> files = getAllUserClassFiles();


// Scan each class
for (File f : files) {
    try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
        scanner.scanClass(in);
    }
}

//The above will look for method, field, and class/interface references
// To check for use of annotations annotated with a marker annotation
// in our user code we rely on the Jandex Indexer, so we need to populate that
final Indexer indexer = new Indexer();
for (File f : files) {
    try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
        indexer.index(in);
    }
}

// Then this step checks the Jandex index for usage of the annotations we are interested in
inspector.checkAnnotationIndex(annotationName -> index.getAnnotations(annotationName));

// All done!
// The following is inspecting the reported usage
Set<AnnotationUsage> usages = scanner.getUsages();
for (AnnotationUsage usage : usages) {
    if (usage.getType() == AnnotationUsageType.CLASS_USAGE) {
        AnnotatedClassUsage cu = usage.asAnnotatedClassUsage();
        System.err.println(cu.sourceClass + " calls " +
        cu.getReferencedClass() +
        " which has been annotated with " +
        cu.getAnnotations());
    } else if (usage.getType() == AnnotationUsageType.METHOD_REFERENCE) {
        AnnotatedMethodReference mr = usage.asAnnotatedMethodReference();
        System.err.println(mr.getSourceClass() + " calls " +
        mr.getMethodClass() + "." + mr.getMethodName() + mr.getDescriptor() +
        " which has been annotated with " +
        mr.getAnnotations());
    } else {
        // Other cases for children of AnnotationUsage here
    }
}
```
if run on our intial example with the overall index we created in this example, we should see 
output similar to:
```
MyOtherClass calls APIObjectFactory which has been annotated with [Experimental]
MyOtherClass calls APIInterface.processPerson(LPerson;)V which has been annotated with [Experimental]  
```

Please see the child classes of `AnnotationUsage` for the various types of access that can be detected by the annotation scanner.

Known issues/limitations are listed [here](https://github.com/kabir/unstable-api-annotation-utils/issues?q=is%3Aissue+label%3A%22Known+Issue%2FLimitation%22).

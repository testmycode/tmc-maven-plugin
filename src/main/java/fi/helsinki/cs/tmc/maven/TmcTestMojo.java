package fi.helsinki.cs.tmc.maven;

import fi.helsinki.cs.tmc.testrunner.TestCaseList;
import fi.helsinki.cs.tmc.testrunner.TestRunner;
import fi.helsinki.cs.tmc.testscanner.TestScanner;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;

/**
 * Runs tests using the TMC test runner.
 *
 * @goal test
 * @execute phase=test-compile
 * @requiresDirectInvocation
 * @requiresDependencyResolution test
 */
public class TmcTestMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;
    
    /**
     * Test suite timeout.
     * @parameter default-value=60000
     * @required
     */
    private long timeout;
    
    /**
     * Output JSON file.
     * @parameter expression="${tmc.test.test_output_file}" default-value="${project.build.directory}/test_output.txt"
     * @required
     */
    private File outputFile;

    public void execute() throws MojoExecutionException {
        List<String> classPathParts;
        try {
            classPathParts = project.getTestClasspathElements();
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Failed to get project classpath", ex);
        }
        String classPath = StringUtils.join(classPathParts, File.pathSeparatorChar);
        
        TestScanner scanner = new TestScanner();
        for (String testSourceDir : project.getTestCompileSourceRoots()) {
            scanner.addSource(new File(testSourceDir));
        }
        scanner.setClassPath(classPath);
        TestCaseList cases = TestCaseList.fromTestMethods(scanner.findTests());
        
        ClassLoader classLoader;
        try {
            URL[] urls = new URL[classPathParts.size()];
            for (int i = 0; i < urls.length; ++i) {
                urls[i] = new File(classPathParts.get(i)).toURI().toURL();
            }
            classLoader = new URLClassLoader(urls);
        } catch (MalformedURLException ex) {
            throw new MojoExecutionException("Failed to build class loader URL", ex);
        }
        
        TestRunner testRunner = new TestRunner(classLoader);
        getLog().info("Running tests using TMC testrunner.");
        testRunner.runTests(cases, timeout);
        try {
            cases.writeToJsonFile(outputFile);
        } catch (IOException ex) {
            throw new MojoExecutionException("IOException: " + ex.getMessage(), ex);
        }
        getLog().info("Results written to " + outputFile);
    }
    
}

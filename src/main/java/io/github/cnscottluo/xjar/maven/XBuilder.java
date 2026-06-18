package io.github.cnscottluo.xjar.maven;

import io.xjar.XKit;
import io.xjar.boot.XBoot;
import io.xjar.filter.XAllEntryFilter;
import io.xjar.filter.XAnyEntryFilter;
import io.xjar.filter.XMixEntryFilter;
import io.xjar.jar.XJar;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE)
public class XBuilder extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "xjar.algorithm", required = true, defaultValue = "AES/CBC/PKCS5Padding")
    private String algorithm;

    @Parameter(property = "xjar.keySize", required = true, defaultValue = "128")
    private int keySize;

    @Parameter(property = "xjar.ivSize", required = true, defaultValue = "128")
    private int ivSize;

    @Parameter(property = "xjar.password", required = true)
    private String password;

    @Parameter(property = "xjar.sourceDir", required = true, defaultValue = "${project.build.directory}")
    private File sourceDir;

    @Parameter(property = "xjar.sourceJar", required = true, defaultValue = "${project.build.finalName}.jar")
    private String sourceJar;

    @Parameter(property = "xjar.targetDir", required = true, defaultValue = "${project.build.directory}")
    private File targetDir;

    @Parameter(property = "xjar.targetJar", required = true, defaultValue = "${project.build.finalName}.xjar")
    private String targetJar;

    @Parameter(property = "xjar.includes")
    private String[] includes;

    @Parameter(property = "xjar.excludes")
    private String[] excludes;

    @Parameter(property = "xjar.deletes")
    private String[] deletes;

    @Parameter(property = "xjar.allowParentTraversal", defaultValue = "false")
    private boolean allowParentTraversal;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (!"jar".equalsIgnoreCase(project.getPackaging())) {
            log.info("Skip for packaging: " + project.getPackaging());
            return;
        }

        try {
            File src = new File(sourceDir, sourceJar);
            File dest = new File(targetDir, targetJar);
            ensureParentFolder(dest);

            log.info("Building xjar: " + dest + " for jar: " + src);
            XMixEntryFilter<JarArchiveEntry> filter = buildFilter(log);

            if (isSpringBootProject()) {
                assertSpringBootConfigurationSupported();
                XBoot.encrypt(src, dest, XKit.key(algorithm, keySize, ivSize, password), filter);
            } else {
                XJar.encrypt(src, dest, XKit.key(algorithm, keySize, ivSize, password), filter);
            }

            deleteMatchedFiles(project.getFile().getParentFile(), log);
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("could not build xjar", e);
        }
    }

    private void ensureParentFolder(File dest) throws IOException {
        File folder = dest.getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs() && !folder.exists()) {
            throw new IOException("could not make directory: " + folder);
        }
    }

    private XMixEntryFilter<JarArchiveEntry> buildFilter(Log log) {
        if (XArray.isEmpty(includes) && XArray.isEmpty(excludes)) {
            log.info("Including all resources");
            return null;
        }

        XMixEntryFilter<JarArchiveEntry> filter = XKit.all();
        if (!XArray.isEmpty(includes)) {
            XAnyEntryFilter<JarArchiveEntry> including = XKit.any();
            for (String include : includes) {
                including.mix(new XIncludeAntEntryFilter(include));
                log.info("Including " + include);
            }
            filter.mix(including);
        }

        if (!XArray.isEmpty(excludes)) {
            XAllEntryFilter<JarArchiveEntry> excluding = XKit.all();
            for (String exclude : excludes) {
                excluding.mix(new XExcludeAntEntryFilter(exclude));
                log.info("Excluding " + exclude);
            }
            filter.mix(excluding);
        }
        return filter;
    }

    private boolean isSpringBootProject() {
        Build build = project.getBuild();
        if (build == null) {
            return false;
        }
        Map<String, Plugin> plugins = build.getPluginsAsMap();
        return plugins.containsKey("org.springframework.boot:spring-boot-maven-plugin");
    }

    private void assertSpringBootConfigurationSupported() throws MojoFailureException {
        Build build = project.getBuild();
        if (build == null) {
            return;
        }

        Plugin plugin = build.getPluginsAsMap().get("org.springframework.boot:spring-boot-maven-plugin");
        if (plugin == null) {
            return;
        }

        Object configuration = plugin.getConfiguration();
        if (!(configuration instanceof Xpp3Dom dom)) {
            return;
        }

        Xpp3Dom executable = dom.getChild("executable");
        if (executable != null && "true".equalsIgnoreCase(executable.getValue())) {
            throw new MojoFailureException("Unsupported for <executable>true</executable> spring-boot-maven-plugin configuration.");
        }

        Xpp3Dom embeddedLaunchScript = dom.getChild("embeddedLaunchScript");
        if (embeddedLaunchScript != null && embeddedLaunchScript.getValue() != null) {
            throw new MojoFailureException("Unsupported for <embeddedLaunchScript>...</embeddedLaunchScript> spring-boot-maven-plugin configuration.");
        }
    }

    private void deleteMatchedFiles(File root, Log log) throws IOException, MojoFailureException {
        if (XArray.isEmpty(deletes) || root == null) {
            return;
        }

        for (String pattern : deletes) {
            deleteByPattern(root.toPath().toAbsolutePath().normalize(), pattern, log);
        }
    }

    /**
     * Deletes files matched by glob patterns under the project directory.
     * <p>
     * This operation is destructive; please verify delete patterns carefully before production usage.
     * Parent traversal via "../" is blocked by default and can be explicitly enabled via xjar.allowParentTraversal.
     */
    private void deleteByPattern(Path baseRoot, String pattern, Log log) throws IOException, MojoFailureException {
        if (pattern == null || pattern.isBlank()) {
            return;
        }

        String normalizedPattern = pattern.replace('\\', '/').trim();
        Path searchRoot = baseRoot;
        while (normalizedPattern.startsWith("../")) {
            if (!allowParentTraversal) {
                throw new MojoFailureException("Pattern '" + pattern + "' is not allowed because parent traversal is disabled. " +
                        "Set -Dxjar.allowParentTraversal=true to enable it explicitly.");
            }
            searchRoot = searchRoot.getParent() == null ? searchRoot : searchRoot.getParent();
            normalizedPattern = normalizedPattern.substring(3);
        }
        while (normalizedPattern.startsWith("./")) {
            normalizedPattern = normalizedPattern.substring(2);
        }

        if (normalizedPattern.isBlank()) {
            return;
        }

        log.info("Deleting file(s) matching pattern: " + searchRoot + "/" + normalizedPattern);
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + normalizedPattern);
        Path finalSearchRoot = searchRoot;

        try (Stream<Path> stream = Files.walk(finalSearchRoot)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> path.startsWith(finalSearchRoot))
                    .filter(path -> matcher.matches(finalSearchRoot.relativize(path)))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.debug("Deleted file: " + path);
                        } catch (IOException e) {
                            log.warn("Could not delete file: " + path + " because: " + e.getMessage());
                        }
                    });
        }
    }
}

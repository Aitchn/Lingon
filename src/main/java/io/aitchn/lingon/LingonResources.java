package io.aitchn.lingon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Utility class for importing language resources from various sources.
 * This class handles resource extraction from JAR files, directories, and classpath resources.
 */
final class LingonResources {
    private static final String LANGUAGES_DIRECTORY = "languages";

    private LingonResources() {
        // Utility class - prevent instantiation
    }

    /**
     * Import language resources from the owner class's location to the target directory.
     * Supports importing from JAR files, directory structures, and classpath resources.
     *
     * @param ownerClass the class whose resources should be imported
     * @param targetLanguagesDirectory the target directory to copy language files to
     * @throws IllegalStateException if import fails
     */
    static void importFromOwner(Class<?> ownerClass, Path targetLanguagesDirectory) {
        Objects.requireNonNull(ownerClass, "ownerClass cannot be null");
        Objects.requireNonNull(targetLanguagesDirectory, "targetLanguagesDirectory cannot be null");

        try {
            Files.createDirectories(targetLanguagesDirectory);

            URL locationUrl = ownerClass.getProtectionDomain().getCodeSource().getLocation();
            Path classLocation = Paths.get(locationUrl.toURI());

            if (Files.isRegularFile(classLocation) && classLocation.toString().endsWith(".jar")) {
                importFromJarFile(classLocation, targetLanguagesDirectory);
            } else if (Files.isDirectory(classLocation)) {
                importFromDirectory(classLocation, targetLanguagesDirectory, ownerClass);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to import languages from owner resources", exception);
        }
    }

    /**
     * Import language resources from a JAR file.
     *
     * @param jarPath the path to the JAR file
     * @param targetDirectory the target directory to extract files to
     * @throws IOException if extraction fails
     */
    private static void importFromJarFile(Path jarPath, Path targetDirectory) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                if (!isLanguageJsonFile(entryName)) {
                    continue;
                }

                String relativePath = entryName.substring((LANGUAGES_DIRECTORY + "/").length());
                if (containsPathTraversal(relativePath)) {
                    continue;
                }

                Path targetFile = targetDirectory.resolve(relativePath.replace('/', File.separatorChar));
                if (Files.exists(targetFile)) {
                    continue;
                }

                Files.createDirectories(targetFile.getParent());
                try (InputStream inputStream = jarFile.getInputStream(entry)) {
                    Files.copy(inputStream, targetFile);
                }
            }
        }
    }

    /**
     * Import language resources from a directory structure.
     *
     * @param classLocation the location of the class files
     * @param targetDirectory the target directory to copy files to
     * @param ownerClass the owner class for classpath lookups
     * @throws Exception if import fails
     */
    private static void importFromDirectory(Path classLocation, Path targetDirectory, Class<?> ownerClass) throws Exception {
        Path languageDirectory = classLocation.resolve(LANGUAGES_DIRECTORY);

        if (Files.isDirectory(languageDirectory)) {
            copyDirectoryTree(languageDirectory, targetDirectory);
        } else {
            // ClassLoader fallback: typically points to build/resources/**/languages
            tryImportFromClassLoader(ownerClass, targetDirectory);

            // Heuristic: infer build/resources/{test|main}/languages
            tryImportFromBuildDirectory(classLocation, targetDirectory);
        }
    }

    /**
     * Try to import from classpath using the class loader.
     *
     * @param ownerClass the owner class
     * @param targetDirectory the target directory
     * @throws Exception if import fails
     */
    private static void tryImportFromClassLoader(Class<?> ownerClass, Path targetDirectory) throws Exception {
        ClassLoader classLoader = ownerClass.getClassLoader();
        URL resourceUrl = (classLoader.getResource(LANGUAGES_DIRECTORY) != null)
                ? classLoader.getResource(LANGUAGES_DIRECTORY)
                : classLoader.getResource(LANGUAGES_DIRECTORY + "/");

        if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
            Path resourcePath = Paths.get(resourceUrl.toURI());
            if (Files.isDirectory(resourcePath)) {
                copyDirectoryTree(resourcePath, targetDirectory);
            }
        }
    }

    /**
     * Try to import from build directory structure.
     *
     * @param classLocation the class location
     * @param targetDirectory the target directory
     */
    private static void tryImportFromBuildDirectory(Path classLocation, Path targetDirectory) {
        // Navigate up to build directory: .../build
        Path buildDirectory = getParentAtDepth(classLocation, 3);
        if (buildDirectory == null) {
            return;
        }

        Path testResourcesPath = buildDirectory.resolve("resources").resolve("test").resolve(LANGUAGES_DIRECTORY);
        Path mainResourcesPath = buildDirectory.resolve("resources").resolve("main").resolve(LANGUAGES_DIRECTORY);

        if (Files.isDirectory(testResourcesPath)) {
            copyDirectoryTree(testResourcesPath, targetDirectory);
        } else if (Files.isDirectory(mainResourcesPath)) {
            copyDirectoryTree(mainResourcesPath, targetDirectory);
        }
    }

    /**
     * Copy an entire directory tree from source to target.
     *
     * @param sourceRoot the source root directory
     * @param targetDirectory the target directory
     * @throws UncheckedIOException if copy fails
     */
    private static void copyDirectoryTree(Path sourceRoot, Path targetDirectory) {
        try (Stream<Path> pathStream = Files.walk(sourceRoot)) {
            for (Path sourcePath : (Iterable<Path>) pathStream::iterator) {
                if (!Files.isRegularFile(sourcePath)) {
                    continue;
                }
                if (!sourcePath.getFileName().toString().endsWith(".json")) {
                    continue;
                }

                String relativePath = sourceRoot.relativize(sourcePath).toString().replace('\\', '/');
                if (containsPathTraversal(relativePath)) {
                    continue;
                }

                Path targetFile = targetDirectory.resolve(relativePath.replace('/', File.separatorChar));
                if (Files.exists(targetFile)) {
                    continue;
                }

                Files.createDirectories(targetFile.getParent());
                try (InputStream inputStream = Files.newInputStream(sourcePath)) {
                    Files.copy(inputStream, targetFile);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to copy directory tree from " + sourceRoot, exception);
        }
    }

    /**
     * Check if a file name represents a language JSON file.
     *
     * @param fileName the file name to check
     * @return true if it's a language JSON file
     */
    private static boolean isLanguageJsonFile(String fileName) {
        return fileName.startsWith(LANGUAGES_DIRECTORY + "/") && fileName.endsWith(".json");
    }

    /**
     * Check if a path contains path traversal attempts (security check).
     *
     * @param path the path to check
     * @return true if the path contains ".." segments
     */
    private static boolean containsPathTraversal(String path) {
        return path.contains("..");
    }

    /**
     * Get parent directory at specified depth, returning null if not possible.
     *
     * @param path the starting path
     * @param depth the number of parent levels to traverse
     * @return the parent path at specified depth, or null if not available
     */
    private static Path getParentAtDepth(Path path, int depth) {
        Path current = path;
        for (int i = 0; i < depth && current != null; i++) {
            current = current.getParent();
        }
        return current;
    }
}

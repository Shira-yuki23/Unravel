package pixel;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;

/** Launches JavaFX video playback outside the jMonkeyEngine JVM. */
final class EpisodeThreeVideo {

    private Process process;

    synchronized void play(int number, Consumer<String> finished) {
        play(Integer.toString(number), finished);
    }

    synchronized void play(String number, Consumer<String> finished) {
        if (process != null && process.isAlive()) {
            finished.accept("Another video is already playing.");
            return;
        }

        try {
            URL resource = EpisodeThreeVideo.class.getResource(
                    "/assets/Models/" + number + ".mp4"
            );
            if (resource == null) {
                throw new IllegalStateException(
                        "Missing assets/Models/" + number + ".mp4"
                );
            }

            String source;
            if ("file".equals(resource.getProtocol())) {
                source = resource.toExternalForm();
            } else {
                Path temporary = Files.createTempFile(
                        "unravel-video-" + number + "-", ".mp4"
                );
                temporary.toFile().deleteOnExit();
                try (InputStream input = resource.openStream()) {
                    Files.copy(input, temporary,
                            StandardCopyOption.REPLACE_EXISTING);
                }
                source = temporary.toUri().toString();
            }

            Path javaFxDirectory = findJavaFxDirectory();
            String modulePath = List.of(
                    javaFxDirectory.resolve("javafx-base-21.0.8-win.jar"),
                    javaFxDirectory.resolve("javafx-graphics-21.0.8-win.jar"),
                    javaFxDirectory.resolve("javafx-media-21.0.8-win.jar")
            ).stream().map(Path::toString)
                    .reduce((a, b) -> a + File.pathSeparator + b)
                    .orElseThrow();

            Path codeLocation = Path.of(
                    EpisodeThreeVideo.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
            );
            Path javaExecutable = Path.of(
                    System.getProperty("java.home"), "bin", "java.exe"
            );
            Path javaFxCache = Path.of(
                    System.getProperty("java.io.tmpdir"),
                    "unravel-javafx-cache"
            );
            Files.createDirectories(javaFxCache);

            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable.toString(),
                    "-Djavafx.cachedir=" + javaFxCache,
                    "--module-path", modulePath,
                    "--add-modules", "javafx.graphics,javafx.media",
                    "-cp", codeLocation.toString(),
                    "pixel.EpisodeThreeVideoPlayer",
                    source,
                    number
            );
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();

            Process launched = process;
            Thread waiter = new Thread(() -> {
                String error = null;
                try {
                    int exitCode = launched.waitFor();
                    if (exitCode != 0) {
                        error = "Video player exited with code " + exitCode;
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    error = "Video wait interrupted.";
                }

                synchronized (EpisodeThreeVideo.this) {
                    if (process == launched) {
                        process = null;
                    }
                }
                finished.accept(error);
            }, "EpisodeThreeVideoWaiter");
            waiter.setDaemon(true);
            waiter.start();

        } catch (Exception error) {
            process = null;
            finished.accept(error.toString());
        }
    }

    private Path findJavaFxDirectory() throws Exception {
        Path codeLocation = Path.of(
                EpisodeThreeVideo.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()
        );
        Path[] candidates = {
            Path.of("lib", "libs", "javafx").toAbsolutePath(),
            Path.of("libs", "javafx").toAbsolutePath(),
            codeLocation.resolve("..").resolve("..")
                    .resolve("libs").resolve("javafx").normalize()
        };

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve(
                    "javafx-media-21.0.8-win.jar"))) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Cannot find lib/libs/javafx beside the project."
        );
    }

    synchronized void cancel() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    void shutdown() {
        cancel();
    }
}

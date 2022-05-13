package com.trafficparrot.example.testing.framework;

import io.qameta.allure.Attachment;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.codeborne.selenide.Selenide.screenshot;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static java.lang.ThreadLocal.withInitial;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.jcodec.api.SequenceEncoder.createWithFps;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.scale.AWTUtil.fromBufferedImageRGB;
import static org.openqa.selenium.OutputType.BYTES;

public class RecordBrowser {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordBrowser.class);

    private static final ThreadLocal<RecordBrowser> THREAD_LOCAL = withInitial(RecordBrowser::new);

    private static final int FPS = 2;
    private static final Path FRAMES_DIR = Paths.get("target").resolve("test-frames");
    private static final Path VIDEOS_DIR = Paths.get("target").resolve("test-videos");

    private byte[] previous;
    private int count;
    private String testName;

    private RecordBrowser() {
        // thread local construction only
    }

    static RecordBrowser recordBrowser() {
        return THREAD_LOCAL.get();
    }

    void recordFrame() {
        try {
            byte[] screenshot = screenshot(BYTES);
            if (screenshot == null) {
                LOGGER.warn(unableToCaptureUsingWebDriverMessage());
            }
            writeFrameFile(screenshot);
        } catch (WebDriverException | IOException e) {
            LOGGER.warn(unableToCaptureUsingWebDriverMessage(), e);
        }
    }

    void testStarted(String testName) throws Exception {
        this.testName = testName;
        count = 0;
        createDirectories(FRAMES_DIR);
        cleanDirectory(FRAMES_DIR.toFile());
    }

    void testFinished() throws Exception {
        video();
    }

    @Attachment(value = "Test video", fileExtension = "mp4")
    private byte[] video() throws IOException {
        Path testFramesDirectory = FRAMES_DIR.resolve(testName);
        List<File> testFrameFiles = frameFiles(testFramesDirectory);

        createDirectories(VIDEOS_DIR);
        File videoFile = VIDEOS_DIR.resolve(testName + ".mp4").toFile();
        LOGGER.info("Creating video file: " + videoFile.getAbsolutePath());
        try (FileChannelWrapper out = writableChannel(videoFile))  {
            SequenceEncoder sequenceEncoder = createWithFps(out, new Rational(FPS, 1));
            for (File frameFile : testFrameFiles) {
                Picture picture = fromBufferedImageRGB(cropEvenPixels(ImageIO.read(frameFile)));
                sequenceEncoder.encodeNativeFrame(picture);
            }
            sequenceEncoder.finish();
        }
        deleteDirectory(testFramesDirectory.toFile());
        return readAllBytes(videoFile.toPath());
    }

    private BufferedImage cropEvenPixels(BufferedImage image) {
        int adjustHeight = image.getHeight() % 2;
        int adjustWidth = image.getWidth() % 2;
        if (adjustHeight == 0 && adjustWidth == 0) {
            return image;
        } else {
            return image.getSubimage(0, 0, image.getWidth() - adjustWidth, image.getHeight() - adjustHeight);
        }
    }

    private void writeFrameFile(byte[] screenshot) throws IOException {
        if (!Arrays.equals(previous, screenshot)) {
            Path frameFile = frameFile(count++);
            Files.createDirectories(frameFile.getParent());
            Files.write(frameFile, screenshot);
            previous = screenshot;
        }
    }

    private Path frameFile(int count) {
        return FRAMES_DIR
                .resolve(testName)
                .resolve(String.format(frameFileNameFormat(), count));
    }

    private List<File> frameFiles(Path framesDirectory) throws IOException {
        Files.createDirectories(framesDirectory);
        try (Stream<Path> paths =  Files.list(framesDirectory)) {
            return paths.map(Path::toFile).sorted(comparing(File::getName)).collect(toList());
        }
    }

    private String frameFileNameFormat() {
        return testName + "-schedule-%06d.png";
    }

    private static String unableToCaptureUsingWebDriverMessage() {
        return "Unable to capture screenshot using " + getWebDriver().getClass().getSimpleName();
    }
}

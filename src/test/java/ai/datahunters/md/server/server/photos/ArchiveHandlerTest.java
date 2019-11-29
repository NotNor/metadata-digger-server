package ai.datahunters.md.server.server.photos;

import ai.datahunters.md.server.photos.upload.ArchiveHandler;
import ai.datahunters.md.server.photos.upload.ArchiveHandlerException;
import ai.datahunters.md.server.photos.upload.filesystem.FileService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ArchiveHandlerTest {

    @MockBean
    FileService fileService;

    @Autowired
    ArchiveHandler archiveHandler;

    @Test
    public void fileRecognitionTest() throws IOException, ArchiveHandlerException {
        List<String> expected = Arrays.asList("smile.png");

        Assertions.assertThrows(ArchiveHandlerException.class,
                () -> archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/test_file.zip")));
        Assertions.assertThrows(ArchiveHandlerException.class,
                () -> archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/CORRUPTED_ZIP.zip")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/ZIP.zip")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR.tar")));
        Assertions.assertEquals(expected,

                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR_BZIP2.tar.bz2")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR_XZ.tar.xz")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR_GZIP.zip")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR_BZIP2.zip")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/ZIP.tar.gz")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/TAR_GZIP.tar.gz")));
    }

    @Test
    public void multipleFilesExtraction() throws IOException, ArchiveHandlerException {
        List<String> expected = Arrays.asList("smile.png", "happy.png");

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/MZIP.zip")));

        Assertions.assertEquals(expected,
                archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/MTAR.tar.bz2")));
    }

    @Test
    public void filesIntegrityValidation() throws IOException, ArchiveHandlerException {
        Path testDir = createTestDir();
        given(fileService.createDirForExtraction(any(Path.class))).willReturn(testDir);
        archiveHandler.probeContentAndUnarchive(openArchive("uploadendpointtest/MTAR.tar.bz2"));

        Assertions.assertEquals("9e537fd87c06667e5d87679e6300092b3383bf5c3685b96c36639157f776379b",
                DigestUtils.sha256Hex(new FileInputStream(testDir.resolve("happy.png").toFile())));
        Assertions.assertEquals("dbe900e9465c658b9b4a7b4073ac9eac05cc52da6ee90dec80e62c4803c22955",
                DigestUtils.sha256Hex(new FileInputStream(testDir.resolve("smile.png").toFile())));
        Files.deleteIfExists(testDir);
    }

    private Path createTestDir() throws IOException {
        Path testDir = Paths.get(String.valueOf(new Random().nextLong()));
        return Files.createDirectory(testDir);
    }

    private InputStream openArchive(String file) throws IOException {
        return new BufferedInputStream(new ClassPathResource(file).getInputStream());
    }
}

package org.example.dataverse.bagit.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Wrapper around static methods to facilitate testing
 *
 * @author adaybujeda
 */
public class FileUtilWrapper {

    private static final Logger logger = Logger.getLogger(FileUtilWrapper.class.getCanonicalName());

    public InputStream newInputStream(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    public Stream<Path> list(Path path) throws IOException {
        return Files.list(path);
    }

}

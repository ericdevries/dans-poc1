package nl.knaw.dans.poc.bagit;

import org.apache.commons.compress.utils.IOUtils;
import nl.knaw.dans.poc.bagit.data.FileDataProvider;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author adaybujeda
 */
public class FileChecksumValidationJob implements Runnable {

    private static final Logger logger = Logger.getLogger(FileChecksumValidationJob.class.getCanonicalName());

    private final FileDataProvider.InputStreamProvider inputStreamProvider;
    private final Path filePath;
    private final String fileChecksum;
    private final BagChecksumType bagChecksumType;
    private final BagValidation.FileValidationResult result;

    public FileChecksumValidationJob(FileDataProvider.InputStreamProvider inputStreamProvider, Path filePath, String fileChecksum, BagChecksumType bagChecksumType,
        BagValidation.FileValidationResult result) {
        this.inputStreamProvider = inputStreamProvider;
        this.filePath = filePath;
        this.fileChecksum = fileChecksum;
        this.bagChecksumType = bagChecksumType;
        this.result = result;
    }

    public void run() {
        InputStream inputStream = null;
        try {
            inputStream = inputStreamProvider.getInputStream();
            String calculatedChecksum = bagChecksumType.getInputStreamDigester().digest(inputStream);
            if (fileChecksum.equals(calculatedChecksum)) {
                result.setSuccess();
            }
            else {
                // TODO this has parameters from a bundle
                result.setError("Invalid checksum");
            }
        }
        catch (Exception e) {
            // TODO this has parameters from a bundle
            result.setError("Error while calculating checksum");
            logger.log(Level.WARNING, String.format("action=validate-checksum result=error filePath=%s type=%s", filePath, bagChecksumType), e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

}

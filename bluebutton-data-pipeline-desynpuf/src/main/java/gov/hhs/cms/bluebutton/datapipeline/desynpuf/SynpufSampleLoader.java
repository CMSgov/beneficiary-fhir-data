package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can extract {@link SynpufSample}s from the archive resources that they're
 * stored in.
 */
public final class SynpufSampleLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynpufSampleLoader.class);

	/**
	 * @param extractionDir
	 *            the {@link Path} to the directory to extract the files to
	 * @param archiveId
	 *            the {@link SynpufArchive} to extract
	 * @return a {@link SynpufSample}s representing the results of extracting
	 *         the specified {@link SynpufArchive}
	 * @throws SynpufException
	 *             Indicates that a problem was encountered reading the archive
	 *             or writing the extracted files from it.
	 */
	public static SynpufSample extractSynpufFile(Path extractionDir, SynpufArchive archiveId) throws SynpufException {
		try {
			Path sampleDir = extractionDir.resolve(archiveId.name().toLowerCase());
			Files.createDirectories(sampleDir);

			/*
			 * This will be the eventual result object, but if all the files
			 * already exist, we can bail and return it early.
			 */
			SynpufSample sample = new SynpufSample(sampleDir, archiveId);
			if (sample.allFilesExist())
				return sample;

			// Read the archive by chaining TAR and BZIP2 streams.
			try (InputStream synpufStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(archiveId.getResourceName());
					BZip2CompressorInputStream synpufBzipStream = new BZip2CompressorInputStream(synpufStream);
					TarArchiveInputStream synpufTarStream = new TarArchiveInputStream(synpufBzipStream);) {

				/*
				 * Loop over each entry in the decompressed TAR archive, write
				 * it out.
				 */
				TarArchiveEntry synpufTarEntry;
				while ((synpufTarEntry = synpufTarStream.getNextTarEntry()) != null) {
					Path synpufFileOutPath = sampleDir.resolve(synpufTarEntry.getName());

					// Open a stream to write out the current entry.
					LOGGER.info("Extracting '{}'...", synpufTarEntry.getName());
					try (FileOutputStream synpufFileOutStream = new FileOutputStream(synpufFileOutPath.toFile());) {
						// Copy from the bounded input stream out to the file.
						byte[] buffer = new byte[1024 * 1024 * 10];
						int bufferedBytes;
						while ((bufferedBytes = synpufTarStream.read(buffer, 0, buffer.length)) != -1) {
							synpufFileOutStream.write(buffer, 0, bufferedBytes);
						}
					}
					LOGGER.info("Extracted '{}'...", synpufTarEntry.getName());
				}
			}

			// Sanity check:
			if (!sample.allFilesExist())
				throw new SynpufException("Missing DE-SynPUF files.");

			return sample;
		} catch (IOException e) {
			throw new SynpufException(e);
		}
	}
}

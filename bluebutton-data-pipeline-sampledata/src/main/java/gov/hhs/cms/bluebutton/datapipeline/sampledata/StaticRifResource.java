package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.csv.CSVParser;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.parse.RifParsingUtils;

/**
 * Enumerates the sample RIF resources available on the classpath.
 */
public enum StaticRifResource {
	SAMPLE_A_BENES(resourceUrl("rif-static-samples/sample-a-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

	SAMPLE_A_CARRIER(resourceUrl("rif-static-samples/sample-a-bcarrier.txt"), RifFileType.CARRIER, 1),

	SAMPLE_A_INPATIENT(resourceUrl("rif-static-samples/sample-a-inpatient.txt"), RifFileType.INPATIENT, 1),

	SAMPLE_A_OUTPATIENT(resourceUrl("rif-static-samples/sample-a-outpatient.txt"), RifFileType.OUTPATIENT, 1),

	SAMPLE_A_SNF(resourceUrl("rif-static-samples/sample-a-snf.txt"), RifFileType.SNF, 1),

	SAMPLE_A_HOSPICE(resourceUrl("rif-static-samples/sample-a-hospice.txt"), RifFileType.HOSPICE, 1),

	SAMPLE_A_HHA(resourceUrl("rif-static-samples/sample-a-hha.txt"), RifFileType.HHA, 1),

	SAMPLE_A_DME(resourceUrl("rif-static-samples/sample-a-dme.txt"), RifFileType.DME, 1),

	SAMPLE_A_PDE(resourceUrl("rif-static-samples/sample-a-pde.txt"), RifFileType.PDE, 1),

	SAMPLE_B_BENES(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "beneficiary_test.rif"),
			RifFileType.BENEFICIARY, 100),

	SAMPLE_B_CARRIER(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "carrier_test.rif"), RifFileType.CARRIER,
			2537),

	SAMPLE_B_INPATIENT(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "inpatient_test.rif"),
			RifFileType.INPATIENT, 27),

	SAMPLE_B_OUTPATIENT(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "outpatient_test.rif"),
			RifFileType.OUTPATIENT, 544),

	SAMPLE_B_SNF(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "snf_test.rif"), RifFileType.SNF, 20),

	SAMPLE_B_HOSPICE(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "hospice_test.rif"), RifFileType.HOSPICE,
			29),

	SAMPLE_B_HHA(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "hha_test.rif"), RifFileType.HHA, 19),

	SAMPLE_B_DME(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "dme_test.rif"), RifFileType.DME, 137),

	SAMPLE_B_PDE(localCopyOfS3Data(TestDataSetLocation.SAMPLE_B_LOCATION, "pde_test.rif"), RifFileType.PDE, 5916),

	SAMPLE_C_BENES(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "beneficiary_test.rif"), RifFileType.BENEFICIARY,
			1000000),

	SAMPLE_C_CARRIER(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "carrier_test.rif"), RifFileType.CARRIER,
			32943217),

	SAMPLE_C_DME(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "dme_test.rif"), RifFileType.DME, 2320363),

	SAMPLE_C_HHA(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "hha_test.rif"), RifFileType.HHA, 228623),

	SAMPLE_C_HOSPICE(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "hospice_test.rif"), RifFileType.HOSPICE,
			106462),

	SAMPLE_C_INPATIENT(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "inpatient_test.rif"), RifFileType.INPATIENT,
			384616),

	SAMPLE_C_OUTPATIENT(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "outpatient_test.rif"),
			RifFileType.OUTPATIENT, 6195549),

	SAMPLE_C_PDE(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "pde_test.rif"), RifFileType.PDE, 8774963),

	SAMPLE_C_SNF(remoteS3Data(TestDataSetLocation.SAMPLE_C_LOCATION, "snf_test.rif"), RifFileType.SNF, 169175);

	private final Supplier<URL> resourceUrlSupplier;
	private final RifFileType rifFileType;
	private final int recordCount;

	private URL resourceUrl;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resourceUrlSupplier
	 *            the value to use for {@link #getResourceSupplier()}
	 * @param rifFileType
	 *            the value to use for {@link #getRifFileType()}
	 * @param recordCount
	 *            the value to use for {@link #getRecordCount()}
	 */
	private StaticRifResource(Supplier<URL> resourceUrlSupplier, RifFileType rifFileType, int recordCount) {
		this.resourceUrlSupplier = resourceUrlSupplier;
		this.rifFileType = rifFileType;
		this.recordCount = recordCount;
	}

	/**
	 * @return the {@link URL} to the resource's contents
	 */
	public synchronized URL getResourceUrl() {
		if (resourceUrl == null)
			resourceUrl = resourceUrlSupplier.get();

		return resourceUrl;
	}

	/**
	 * @return the {@link RifFileType} of the RIF file
	 */
	public RifFileType getRifFileType() {
		return rifFileType;
	}

	/**
	 * @return the number of beneficiaries/claims/drug events in the RIF file
	 */
	public int getRecordCount() {
		return recordCount;
	}

	/**
	 * @return a {@link RifFile} based on this {@link StaticRifResource}
	 */
	public RifFile toRifFile() {
		return new StaticRifFile(this);
	}

	/**
	 * @param resourceName
	 *            the name of the resource on the classpath (as might be passed
	 *            to {@link ClassLoader#getResource(String)})
	 * @return a {@link Supplier} for the {@link URL} to the resource's contents
	 */
	private static Supplier<URL> resourceUrl(String resourceName) {
		return () -> {
			URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
			if (resource == null)
				throw new IllegalArgumentException("Unable to find resource: " + resourceName);

			return resource;
		};
	}

	/**
	 * @param dataSetLocation
	 *            the {@link TestDataSetLocation} of the file to get a local
	 *            copy of
	 * @param fileName
	 *            the name of the specific file in the specified
	 *            {@link TestDataSetLocation} to get a local copy of, e.g.
	 *            "beneficiaries.rif"
	 * @return a {@link URL} to a local copy of the specified test data file
	 *         from S3
	 */
	private static Supplier<URL> localCopyOfS3Data(TestDataSetLocation dataSetLocation, String fileName) {
		return () -> {
			// Find the build output `target` directory.
			Path targetDir = Paths.get(".", "bluebutton-data-pipeline-sampledata", "target");
			if (!Files.exists(targetDir))
				targetDir = Paths.get("..", "bluebutton-data-pipeline-sampledata", "target");
			if (!Files.exists(targetDir))
				throw new IllegalStateException();

			// Build the path that the resources will be downloaded to.
			Path resourceDir = targetDir.resolve("test-data-from-s3").resolve(dataSetLocation.getS3BucketName())
					.resolve(dataSetLocation.getS3KeyPrefix());
			Path resourceLocalCopy = resourceDir.resolve(fileName);

			/*
			 * Implementation note: we have to carefully leverage
			 * synchronization to ensure that we don't end up with multiple
			 * copies of the same file. To avoid pegging dev systems, it's also
			 * best to ensure that we're only grabbing one file at a time.
			 * Locking on the static class object accomplishes these goals.
			 */
			synchronized (StaticRifResource.class) {
				// Ensure the directory exists.
				if (!Files.exists(resourceDir)) {
					try {
						Files.createDirectories(resourceDir);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}

				// Download the file, if needed.
				if (!Files.exists(resourceLocalCopy)) {
					downloadFromS3(dataSetLocation, fileName, resourceLocalCopy);
				}
			}

			// We now know the file exists, so return it.
			try {
				return resourceLocalCopy.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new BadCodeMonkeyException(e);
			}
		};
	}

	/**
	 * Downloads the specified S3 object to the specified local path.
	 * 
	 * @param dataSetLocation
	 *            the {@link TestDataSetLocation} of the S3 object to download
	 * @param fileName
	 *            the name of the specific object/file to be downloaded
	 * @param downloadPath
	 *            the {@link Path} to download the S3 object to
	 */
	private static void downloadFromS3(TestDataSetLocation dataSetLocation, String fileName, Path downloadPath) {
		/*
		 * To avoid dragging in the S3 client libraries, we require here that
		 * the test data files be available publicly via HTTP.
		 */

		URL s3DownloadUrl = remoteS3Data(dataSetLocation, fileName).get();
		download(s3DownloadUrl, downloadPath);
	}

	/**
	 * Copies the contents of the specified {@link URL} to the specified local
	 * {@link Path}.
	 * 
	 * @param url
	 *            the {@link URL} to download the contents of
	 * @param localPath
	 *            the local {@link Path} to write to
	 */
	private static void download(URL url, Path localPath) {
		FileOutputStream outputStream = null;
		try {
			ReadableByteChannel channel = Channels.newChannel(url.openStream());
			outputStream = new FileOutputStream(localPath.toFile());
			outputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
	}

	/**
	 * @param dataSetLocation
	 *            the {@link TestDataSetLocation} of the file to get a local
	 *            copy of
	 * @param fileName
	 *            the name of the specific file in the specified
	 *            {@link TestDataSetLocation} to get a local copy of, e.g.
	 *            "beneficiaries.rif"
	 * @return a {@link URL} that can be used to download/stream the specified
	 *         test data file from S3
	 */
	private static Supplier<URL> remoteS3Data(TestDataSetLocation dataSetLocation, String fileName) {
		return () -> {
			try {
				return new URL(String.format("http://%s.s3.amazonaws.com/%s/%s", dataSetLocation.getS3BucketName(),
						dataSetLocation.getS3KeyPrefix(), fileName));
			} catch (MalformedURLException e) {
				throw new BadCodeMonkeyException(e);
			}
		};
	}

	/**
	 * A simple app driver that can be run to verify the record counts for each
	 * {@link StaticRifResource}.
	 * 
	 * @param args
	 *            (not used)
	 * @throws Exception
	 *             Any {@link Exception}s encountered will cause this mini-app
	 *             to terminate.
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * Note: Because of the SAMPLE_C files' large size, this will take HOURS
		 * to run.
		 */

		for (StaticRifResource resource : StaticRifResource.values()) {
			Set<String> uniqueIds = new HashSet<>();
			Path tempDownloadPath = null;
			InputStream tempDownloadStream = null;
			try {
				tempDownloadPath = Files.createTempFile("bluebutton-test-data-", ".rif");
				download(resource.getResourceUrl(), tempDownloadPath);

				tempDownloadStream = new BufferedInputStream(new FileInputStream(tempDownloadPath.toFile()));
				CSVParser parser = RifParsingUtils.createCsvParser(RifParsingUtils.CSV_FORMAT, tempDownloadStream,
						StandardCharsets.UTF_8);
				parser.forEach(r -> uniqueIds.add(r.get(resource.getRifFileType().getIdColumn())));
			} finally {
				if (tempDownloadPath != null)
					Files.deleteIfExists(tempDownloadPath);
				if (tempDownloadStream != null)
					tempDownloadStream.close();
			}
			System.out.println(String.format("%s: %d", resource.name(), uniqueIds.size()));
		}
	}
}

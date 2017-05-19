package gov.hhs.cms.bluebutton.datapipeline.rif.extract.subset;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.justdavis.karl.misc.exceptions.unchecked.UncheckedJaxbException;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;

/**
 * <p>
 * Given some RIF data sets, extracts a random subset of beneficiaries and their
 * claims. This is useful for testing purposes: creating smaller subsets of
 * larger data sets.
 * </p>
 * <p>
 * Note to future maintainers: this was written in haste and isn't very
 * flexible. My apologies.
 * </p>
 */
public final class DataSetSubsetter {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSetSubsetter.class);

	static final String DATA_SET_ID_DUMMY_1000000 = "1000000-beneficiaries-2017-04-18T05:03:30Z";
	static final String DATA_SET_ID_DUMMY_100000 = "100000-beneficiaries-2017-05-16T19:36:23.445Z";
	static final String DATA_SET_ID_DUMMY_10000 = "10000-beneficiaries-2017-05-18T17:58:44.370Z";
	static final String DATA_SET_ID_DUMMY_1000 = "1000-beneficiaries-2017-05-18T18:14:12.019Z";
	static final String DATA_SET_ID_DUMMY_100 = "100-beneficiaries-2017-05-18T18:23:16.941Z";
	static final String DATA_SET_ID_DUMMY_10 = "10-beneficiaries-2017-05-18T18:25:12.909Z";
	static final String DATA_SET_ID_DUMMY_1 = "1-beneficiaries-2017-05-18T18:26:30.504Z";

	/**
	 * Creates a subset of the specified input {@link RifFile}s, writing out the
	 * results via the {@link CSVPrinter}s provided by the specified
	 * {@link IDataSetWriter}.
	 * 
	 * @param output
	 *            the {@link IDataSetWriter} to get the needed
	 *            {@link CSVPrinter}s from
	 * @param beneficiaryCount
	 *            the target beneficiary count of the copy/subset to create
	 * @param rifFiles
	 *            the input {@link RifFile}s to be subsetted
	 * @throws IOException
	 *             Any {@link IOException}s encountered will be bubbled up.
	 */
	public static void createSubset(IDataSetWriter output, int beneficiaryCount, List<RifFile> rifFiles)
			throws IOException {
		LOGGER.info("Scanning beneficiary IDs...");
		List<RifFile> beneficiaryFiles = rifFiles.stream().filter(f -> f.getFileType() == RifFileType.BENEFICIARY)
				.collect(Collectors.toList());
		List<String> beneficiaryIds = new ArrayList<>();
		for (RifFile beneficiaryFile : beneficiaryFiles) {
			CSVParser parser = RifFilesProcessor.createCsvParser(beneficiaryFile);
			parser.forEach(r -> {
				String beneficiaryId = r.get(BeneficiaryRow.Column.BENE_ID);
				if (beneficiaryIds.contains(beneficiaryId))
					throw new IllegalStateException();
				beneficiaryIds.add(beneficiaryId);
			});
			parser.close();
		}
		LOGGER.info("Scanned beneficiary IDs.");

		Set<String> selectedBeneficiaryIds = new HashSet<>(beneficiaryCount);
		Collections.shuffle(beneficiaryIds);
		for (int i = 0; i < beneficiaryCount; i++)
			selectedBeneficiaryIds.add(beneficiaryIds.get(i));
		LOGGER.info("Selected '{}' random beneficiary IDs.", beneficiaryCount);

		Map<RifFileType, Enum<?>> beneficiaryColumnByFileType = new HashMap<>();
		beneficiaryColumnByFileType.put(RifFileType.BENEFICIARY, BeneficiaryRow.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.CARRIER, CarrierClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.DME, DMEClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.HHA, HHAClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.HOSPICE, HospiceClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.INPATIENT, InpatientClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.OUTPATIENT, OutpatientClaimGroup.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.PDE, PartDEventRow.Column.BENE_ID);
		beneficiaryColumnByFileType.put(RifFileType.SNF, SNFClaimGroup.Column.BENE_ID);

		for (RifFile rifFile : rifFiles) {
			LOGGER.info("Subsetting RIF file: '{}'...", rifFile.getDisplayName());
			CSVPrinter rifFilePrinter = output.getPrinter(rifFile.getFileType());
			CSVParser parser = RifFilesProcessor.createCsvParser(rifFile);
			parser.forEach(r -> {
				String beneficiaryId = r.get(beneficiaryColumnByFileType.get(rifFile.getFileType()));
				if (selectedBeneficiaryIds.contains(beneficiaryId))
					try {
						rifFilePrinter.printRecord(r);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
			});
		}
		LOGGER.info("Subsetted all RIF files.");
	}

	interface IDataSetWriter extends AutoCloseable {
		CSVPrinter getPrinter(RifFileType fileType) throws IOException;
	}

	/**
	 * This {@link IDataSetWriter} outputs data sets to a directory on the local
	 * system.
	 */
	private static final class LocalDataSetWriter implements IDataSetWriter {
		private final Path outputDirectory;
		private final Instant timestamp;
		private final Map<RifFileType, CSVPrinter> printers;

		/**
		 * Constructs a new {@link LocalDataSetWriter}.
		 * 
		 * @param outputDirectory
		 *            the {@link Path} of the directory to write the output to
		 *            (must already exist)
		 * @param timestamp
		 *            the timestamp of the dataset to output
		 * @throws IOException
		 *             Any {@link IOException}s encountered will be passed
		 *             along.
		 */
		public LocalDataSetWriter(Path outputDirectory, Instant timestamp) throws IOException {
			if (!Files.isDirectory(outputDirectory))
				throw new IllegalArgumentException();

			Path dataSetDirectory = outputDirectory.resolve(DateTimeFormatter.ISO_INSTANT.format(timestamp));
			Files.createDirectory(dataSetDirectory);

			this.outputDirectory = dataSetDirectory;
			this.timestamp = timestamp;
			this.printers = new HashMap<>();
		}

		/**
		 * @see gov.hhs.cms.bluebutton.datapipeline.rif.extract.subset.DataSetSubsetter.IDataSetWriter#getPrinter(gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType)
		 */
		@Override
		public CSVPrinter getPrinter(RifFileType fileType) throws IOException {
			if (!printers.containsKey(fileType)) {
				FileWriter writer = new FileWriter(outputDirectory.resolve(computeRifFileName(fileType)).toFile());
				CSVFormat csvFormat = RifFilesProcessor.CSV_FORMAT.withHeader(computeColumnNames(fileType));
				CSVPrinter printer = new CSVPrinter(writer, csvFormat);
				printers.put(fileType, printer);
			}

			return printers.get(fileType);
		}

		/**
		 * @param fileType
		 *            the {@link RifFileType} to compute the column names for
		 * @return an array of the column names in the specified
		 *         {@link RifFileType}
		 */
		private String[] computeColumnNames(RifFileType fileType) {
			if (fileType == RifFileType.BENEFICIARY) {
				return BeneficiaryRow.Column.getColumnNames();
			} else if (fileType == RifFileType.CARRIER) {
				return CarrierClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.DME) {
				return DMEClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.HHA) {
				return HHAClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.HOSPICE) {
				return HospiceClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.INPATIENT) {
				return InpatientClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.OUTPATIENT) {
				return OutpatientClaimGroup.Column.getColumnNames();
			} else if (fileType == RifFileType.PDE) {
				return PartDEventRow.Column.getColumnNames();
			} else if (fileType == RifFileType.SNF) {
				return SNFClaimGroup.Column.getColumnNames();
			}

			throw new IllegalArgumentException();
		}

		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		public void close() throws IOException, JAXBException {
			List<DataSetManifestEntry> entries = new ArrayList<>(printers.size());
			for (Entry<RifFileType, CSVPrinter> printerEntry : printers.entrySet()) {
				entries.add(new DataSetManifestEntry(computeRifFileName(printerEntry.getKey()), printerEntry.getKey()));
				printerEntry.getValue().close();
			}

			DataSetManifest manifest = new DataSetManifest(timestamp, 1, entries);
			JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			FileWriter writer = new FileWriter(outputDirectory.resolve("manifest.xml").toFile());
			jaxbMarshaller.marshal(manifest, writer);
			writer.close();

			printers.clear();
		}

		/**
		 * @param fileType
		 *            the {@link RifFileType} to compute a name for
		 * @return the name to use for RIF files of the specified
		 *         {@link RifFileType}
		 */
		private static String computeRifFileName(RifFileType fileType) {
			return String.format("%s.rif", fileType.name().toLowerCase());
		}
	}

	/**
	 * The application entry point that can be used to run the
	 * {@link DataSetSubsetter}.
	 * 
	 * @param args
	 *            (not used)
	 * @throws Exception
	 *             Any exceptions thrown will be bubbled up, terminating the
	 *             app.
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * These two variables can be adjusted to specify the data set to start
		 * from and the desired size of the copy/subset being created. From the
		 * original 1M beneficiary dummy sample data set, subsets were created
		 * going down in size by powers of ten. This gives test authors lots of
		 * good options for how much data to test against.
		 */
		String originalDataSetId = DATA_SET_ID_DUMMY_10;
		int targetBeneficiaryCountForSubset = 1;

		Path outputDirectory = Paths.get(".", "DummyData");
		Files.createDirectories(outputDirectory);
		Path downloadDirectory = outputDirectory.resolve(originalDataSetId);
		Files.createDirectories(downloadDirectory);

		ExtractionOptions options = new ExtractionOptions("gov-hhs-cms-bluebutton-sandbox-etl-staging");
		try (IDataSetWriter output = new LocalDataSetWriter(outputDirectory, Instant.now());) {
			List<RifFile> rifFiles = downloadDataSet(options, originalDataSetId, downloadDirectory);
			DataSetSubsetter.createSubset(output, targetBeneficiaryCountForSubset, rifFiles);
		}
	}

	/**
	 * @param options
	 *            the {@link ExtractionOptions} to use
	 * @param dataSetS3KeyPrefix
	 *            the S3 key prefix (i.e. directory) of the data set to download
	 * @param downloadDirectory
	 *            the Path to the directory to download the RIF files locally to
	 * @return the {@link S3RifFile}s that comprise the full 1M beneficiary
	 *         dummy data set
	 */
	private static List<RifFile> downloadDataSet(ExtractionOptions options, String dataSetS3KeyPrefix,
			Path downloadDirectory) {
		AmazonS3 s3Client = S3Utilities.createS3Client(options);
		TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();

		String dataSetPrefix = "DummyData/" + dataSetS3KeyPrefix;
		String manifestSuffix = "manifest.xml";

		Path manifestDownloadPath = downloadDirectory.resolve(manifestSuffix);
		if (!Files.exists(manifestDownloadPath)) {
			String manifestKey = String.format("%s/%s", dataSetPrefix, manifestSuffix);
			Download manifestDownload = transferManager.download(options.getS3BucketName(), manifestKey,
					manifestDownloadPath.toFile());
			try {
				manifestDownload.waitForCompletion();
			} catch (AmazonClientException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		LOGGER.info("Manifest downloaded.");

		DataSetManifest dummyDataSetManifest;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			dummyDataSetManifest = (DataSetManifest) jaxbUnmarshaller
					.unmarshal(manifestDownloadPath.toFile());
		} catch (JAXBException e) {
			throw new UncheckedJaxbException(e);
		}

		List<RifFile> rifFiles = new ArrayList<>();
		for (DataSetManifestEntry manifestEntry : dummyDataSetManifest.getEntries()) {
			String dataSetFileKey = String.format("%s/%s", dataSetPrefix, manifestEntry.getName());
			Path dataSetFileDownloadPath = downloadDirectory.resolve(manifestEntry.getName());

			if (!Files.exists(dataSetFileDownloadPath)) {
				Download dataSetFileDownload = transferManager.download(options.getS3BucketName(), dataSetFileKey,
						dataSetFileDownloadPath.toFile());
				LOGGER.info("Downloading RIF file: '{}'...", manifestEntry.getName());
				try {
					dataSetFileDownload.waitForCompletion();
				} catch (AmazonClientException | InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			RifFile dataSetFile = new LocalRifFile(dataSetFileDownloadPath, manifestEntry.getType());
			rifFiles.add(dataSetFile);
		}

		transferManager.shutdownNow();
		LOGGER.info("Original RIF files ready.");
		return rifFiles;
	}
}

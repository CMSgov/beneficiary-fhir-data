#!/usr/bin/env groovy

/*
 * This simple Groovy script reads in response durations (in milliseconds, one
 * value per line) and outputs an HGRAM of their distribution, using the most
 * excellent <https://github.com/HdrHistogram/HdrHistogram> library.
 *
 * When combined with our SQLite DB of access log entries, it can be run as
 * follows:
 *
 *     $ /usr/local/opt/sqlite3/bin/sqlite3 -csv -noheader \
 *         ./output/access_logs.sqlite \
 *         "SELECT duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all' AND timestamp_month = '2019-04'" \
 *         | ./hgram_ifier.groovy \
 *         > ./output/access_log_durations_eob_by_patient_id_all_201904.hgram
 *
 * Or, to create separate HGRAMs for many months all in one go, use a Bash
 * `for` loop like this:
 *
 *     $ time for month in $(/usr/local/opt/sqlite3/bin/sqlite3 -csv -noheader ./output/access_logs.sqlite "SELECT DISTINCT timestamp_month FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all'"); do /usr/local/opt/sqlite3/bin/sqlite3 -csv -noheader ./output/access_logs.sqlite "SELECT duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all' AND timestamp_month = '${month}'" | ./hgram_ifier.groovy > "./output/access_log_durations_eob_by_patient_id_all_${month}.hgram"; done
 *
 * For easy visualization, take the resulting `.hgram` file and drop it here:
 * <http://hdrhistogram.github.io/HdrHistogram/plotFiles.html>.
 */

@Grab(group='org.hdrhistogram', module='HdrHistogram', version='2.1.11')
import org.HdrHistogram.Histogram;

def cli = new CliBuilder(usage: "${this.class.getSimpleName()}")
cli.i(required: false, 'input file (defaults to STDIN)')
cli.o(required: false, 'output file (defaults to STDOUT)')
cli.h(required: false, longOpt: 'help', 'display this help/usage message')

def options = cli.parse(args)
if (!options || options.h) {
	if (!options)
		System.err << 'Error while parsing command-line options.\n'
	cli.usage()
	if (!options)
		System.exit 1
}

def inputStream;
def outputStream;
try {
	// Configure input stream.
	if (options.i) {
		def inputPath = Paths.get(options.i)
		if (!Files.isReadable(inputPath)) {
			System.err << "Unable to read specified input file: '${options.i}'."
			System.exit 2
		}
	
		inputStream = inputPath.toFile().newInputStream()
	} else {
		inputStream = System.in
	}
	
	// Configure output stream.
	if (options.o) {
		def outputPath = Paths.get(options.o)
		if (!Files.isWriteable(outputPath)) {
			System.err << "Unable to write specified output file: '${options.o}'."
			System.exit 3
		}
	
		outputStream = outputPath.toFile().newOutputStream()
	} else {
		outputStream = System.out
	}

	// Initialize Histogram: track values from 1 ms to 1 hr (but auto-resizable), at max precision.
	Histogram histogram = new Histogram(1, 3600000, 5);
	histogram.setAutoResize(true)

	// Read input line by line, adding each value to histogram.
	def lineCount = 0;
	inputStream.eachLine { line ->
		lineCount++
		
		if (!line.isInteger()) {
			System.err << "Invalid number on line '${lineCount}': '${line.trim}'."
			System.exit 4
		}
		
        histogram.recordValue(line.toInteger());
    }

	// Write output HGRAM.
	histogram.outputPercentileDistribution(outputStream, 1.0);
} finally {
	if (inputStream != null)
		inputStream.close()
	if (outputStream != null)
		outputStream.close()
}
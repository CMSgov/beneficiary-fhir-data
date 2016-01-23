package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import gov.hhs.cms.bluebutton.texttofhir.parsing.antlr.MyMedicare2BlueButtonTextLexer;
import gov.hhs.cms.bluebutton.texttofhir.parsing.antlr.MyMedicare2BlueButtonTextParser;
import gov.hhs.cms.bluebutton.texttofhir.parsing.antlr.MyMedicare2BlueButtonTextParser.BbTextFileContext;
import gov.hhs.cms.bluebutton.texttofhir.parsing.antlr.MyMedicare2BlueButtonTextParser.SectionContext;

/**
 * Contains utility code for parsing CMS/MyMedicare.gov BlueButton text files
 * into {@link TextFile} objects.
 */
public final class TextFileProcessor {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a");

	/**
	 * @param textFileStream
	 *            an {@link InputStream} containing the CMS/MyMedicare.gov
	 *            BlueButton text file to be parsed
	 * @return the {@link TextFile} model object that was parsed from the
	 *         specified {@link BufferedReader}
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} may be thrown if the file
	 *             could not be read or properly parsed.
	 */
	public static TextFile parse(InputStream textFileStream) throws TextFileParseException {
		try {
			// BB text files are always in ASCII.
			InputStreamReader textFileReader = new InputStreamReader(textFileStream, StandardCharsets.US_ASCII);

			/*
			 * Parse the incoming text file with the ANTLR4 grammar defined in
			 * ''. This grammar is used at build time to automatically generate
			 * the 'gov.hhs.cms.bluebutton.texttofhir.parsing' classes (and all
			 * of their nested types).
			 */
			BbTextFileContext parsedBlueButtonTextFile = parseWithAntlr(textFileReader);

			/*
			 * The ANTLR parsing results in model objects that are close to what
			 * we want, but still a bit "messy". This is probably mostly due to
			 * this being my first time using it. To try and insulate the rest
			 * of the code from that messiness -- and any changes I'll end up
			 * making to fix my mistakes with the grammar -- we convert the
			 * ANTLR model objects into the slightly cleaner model classes
			 * defined in this package.
			 */
			TextFile textFileModelObject = convertAntlrModel(parsedBlueButtonTextFile);
			return textFileModelObject;
		} catch (IOException e) {
			throw new TextFileParseException(e);
		}
	}

	/**
	 * @param textFileReader
	 *            the {@link InputStreamReader} for the text file to be parsed
	 * @return the {@link BbTextFileContext} object that was produced via the
	 *         ANTLR parsing
	 * @throws IOException
	 *             Any {@link IOException}s encountered trying to read the file
	 *             will be bubbled up.
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if errors are
	 *             encountered while parsing the specified file.
	 */
	private static BbTextFileContext parseWithAntlr(InputStreamReader textFileReader)
			throws IOException, TextFileParseException {
		/*
		 * Build the ANTLR lexer, which chops the input up into the tokens
		 * defined in the grammar.
		 */
		CharStream antlrInputStream = new ANTLRInputStream(textFileReader);
		MyMedicare2BlueButtonTextLexer lexer = new MyMedicare2BlueButtonTextLexer(antlrInputStream);
//		for (Token token : lexer.getAllTokens())
//			System.out.println(token);
		TokenStream lexerTokenStream = new CommonTokenStream(lexer);

		/*
		 * Create and run the parser, which applies the parsing rules from the
		 * grammar to do some first-pass processing/grouping on the tokens. The
		 * "*Context" objects result from this parsing.
		 */
		MyMedicare2BlueButtonTextParser parser = new MyMedicare2BlueButtonTextParser(lexerTokenStream);
		parser.removeErrorListeners();
		SyntaxErrorCollector errorCollector = new SyntaxErrorCollector();
		parser.addErrorListener(errorCollector);
		BbTextFileContext parsedBlueButtonTextFile = parser.bbTextFile();
		if (!errorCollector.getErrors().isEmpty())
			throw new TextFileParseException(
					"Errors encountered while parsing input file: " + errorCollector.getErrors());

		return parsedBlueButtonTextFile;
	}

	/**
	 * @param antlrTextFile
	 *            the ANTLR-produced {@link BbTextFileContext} model object to
	 *            be converted
	 * @return a {@link TextFile} model object matching the specified
	 *         {@link BbTextFileContext}
	 */
	private static TextFile convertAntlrModel(BbTextFileContext antlrTextFile) {
		/*
		 * We assume here that the generated timestamps (which don't include a
		 * TZ) are generated somewhere on the US East Coast.
		 */
		String timestampText = antlrTextFile.fileHeader().timestamp.getText().trim();
		LocalDateTime unzonedTimestamp = LocalDateTime.parse(timestampText, TIMESTAMP_FORMATTER);
		ZonedDateTime zonedTimestamp = ZonedDateTime.of(unzonedTimestamp,
				TimeZone.getTimeZone("US/Eastern").toZoneId());

		List<Section> sections = new ArrayList<>();
		long sectionIndex = 0;
		for (SectionContext antlrSection : antlrTextFile.section()) {
			Section section = convertAntlrModel(antlrSection, sectionIndex);
			sections.add(section);
			sectionIndex++;
		}

		TextFile textFileModelObject = new TextFile(zonedTimestamp, sections);
		return textFileModelObject;
	}

	/**
	 * @param antlrSection
	 *            the ANTLR-produced {@link SectionContext} model object to be
	 *            converted
	 * @param sectionIndex
	 *            the (zero-indexed) position of the specified
	 *            {@link SectionContext} in the file (relative to the other
	 *            {@link SectionContext}s)
	 * @return a {@link TextFile} model object matching the specified
	 *         {@link SectionContext}
	 */
	private static Section convertAntlrModel(SectionContext antlrSection, long sectionIndex) {
		String headerName = null;
		if (antlrSection.sectionHeader().entry != null) {
			Token headerEntry = antlrSection.sectionHeader().entry;
			headerName = headerEntry.getText().trim();
		}

		List<Entry> entries = new ArrayList<>();
		long entryIndex = 0;
		for (TerminalNode antlrEntry : antlrSection.ENTRY()) {
			String entryText = antlrEntry.getText().trim();
			int separatorIndex = entryText.indexOf(':');
			String entryName = entryText.substring(0, separatorIndex).trim();
			String entryValue = entryText.substring(separatorIndex + 1).trim();
			Entry entry = new Entry(entryIndex, entryName, entryValue);
			entries.add(entry);
			entryIndex++;
		}

		Section section = new Section(sectionIndex, headerName, entries);
		return section;
	}

	/**
	 * This {@link ANTLRErrorListener} collects all of the
	 * {@link ANTLRErrorListener#syntaxError(Recognizer, Object, int, int, String, RecognitionException)}
	 * events that are fired for later analysis.
	 */
	private static final class SyntaxErrorCollector extends BaseErrorListener {
		private List<SyntaxErrorEvent> errors;

		/**
		 * Constructs a new {@link SyntaxErrorCollector} instance.
		 */
		public SyntaxErrorCollector() {
			this.errors = new ArrayList<>();
		}

		/**
		 * @see org.antlr.v4.runtime.BaseErrorListener#syntaxError(org.antlr.v4.runtime.Recognizer,
		 *      java.lang.Object, int, int, java.lang.String,
		 *      org.antlr.v4.runtime.RecognitionException)
		 */
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			SyntaxErrorEvent error = new SyntaxErrorEvent(line, charPositionInLine, msg);
			this.errors.add(error);
		}

		/**
		 * @return the {@link List} of {@link SyntaxErrorEvent}s encountered
		 */
		public List<SyntaxErrorEvent> getErrors() {
			return Collections.unmodifiableList(errors);
		}
	}

	/**
	 * Models an
	 * {@link ANTLRErrorListener#syntaxError(Recognizer, Object, int, int, String, RecognitionException)}
	 * event.
	 */
	private static final class SyntaxErrorEvent {
		private final int line;
		private final int charPositionInLine;
		private final String message;

		/**
		 * Constructs a new {@link SyntaxErrorEvent} instance.
		 * 
		 * @param line
		 *            see
		 *            {@link ANTLRErrorListener#syntaxError(Recognizer, Object, int, int, String, RecognitionException)}
		 * @param charPositionInLine
		 *            see
		 *            {@link ANTLRErrorListener#syntaxError(Recognizer, Object, int, int, String, RecognitionException)}
		 * @param message
		 *            see
		 *            {@link ANTLRErrorListener#syntaxError(Recognizer, Object, int, int, String, RecognitionException)}
		 */
		public SyntaxErrorEvent(int line, int charPositionInLine, String message) {
			this.line = line;
			this.charPositionInLine = charPositionInLine;
			this.message = message;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("Error in file at line %d, char %d: %s", line, charPositionInLine, message);
		}
	}
}

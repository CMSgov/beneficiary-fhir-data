/**
 * This is an ANTLR4 grammar for the CMS/MyMedicare.gov BlueButton v2 Text File
 * Format. It parses those text files into an ordered list of sections, each
 * which contains an ordered list of elements, which contain a mix of metadata
 * and key-value pairs.
 *
 * This grammar is converted via `antlr4-maven-plugin` during the build into a
 * set of generated Java classes that can be used to parse such files.
 */
grammar MyMedicare2BlueButtonText;

options {
    // antlr will generate java lexer and parser
    language = Java;
}

bbTextFile    : fileHeader section+
                EOF;
fileHeader    : SECTION_SEPARATOR
                FILE_TITLE
                SECTION_SEPARATOR
                CONFIDENTIAL
                FORMAT_SPEC
                timestamp=TIMESTAMP
                NL*;
sectionHeader : SECTION_SEPARATOR
                entry=ENTRY? NL*
                SECTION_SEPARATOR
                NL*;
section       : sectionHeader (entry=ENTRY NL*)+;

SECTION_SEPARATOR
    : '--------------------------------' NL;
FILE_TITLE
    : 'MYMEDICARE.GOV PERSONAL HEALTH INFORMATION' NL;
CONFIDENTIAL
    : '**********CONFIDENTIAL***********' NL;
FORMAT_SPEC
    : 'Produced by the Blue Button (v2.0)' NL;
TIMESTAMP
    // Match format: '02/04/2015 9:18 AM'
    : ('0'..'9')+ '/' ('0'..'9')+ '/' ('0'..'9')+ ' ' ('0'..'9')+ ':' ('0'..'9')+ ' ' ('AM'|'PM') NL;
ENTRY
    : ~('\r'|'\n')+;
NL
    : '\r' '\r'? '\n';

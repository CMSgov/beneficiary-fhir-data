import unittest

from matching.parser import normalize_address


class TestProjectUsatParser(unittest.TestCase):
    def test_basic_street_address(self) -> None:
        address = "123 North Main Street Apt 4B\nNew York, NY 10001"
        expected = "123 N MAIN ST APT 4B\nNEW YORK NY 10001"
        assert normalize_address(address) == expected

        # Compound directional street name
        address2 = "200 EAST AND WEST RD\nWASHINGTON DC 20001"
        expected2 = "200 EAST AND WEST RD\nWASHINGTON DC 20001"
        assert normalize_address(address2) == expected2

        # False-positive Recipient (e.g., OFC OFC)
        address3 = "312 2ND ST NW OFC OFC\nWASHINGTON DC 20001"
        expected3 = "312 2ND ST NW OFC OFC\nWASHINGTON DC 20001"
        assert normalize_address(address3) == expected3

        # Avoid state-prefix abbreviation on general street layouts (e.g., Washington St)
        address4 = "111 N Washington St\nFalls Church VA 22046"
        expected4 = "111 N WASHINGTON ST\nFALLS CHURCH VA 22046"
        assert normalize_address(address4) == expected4

        # Avoid Interstate expansion for secondary unit identifiers (e.g., APT I1)
        address5 = "123 MAIN ST APT I1\nWASHINGTON DC 20001"
        expected5 = "123 MAIN ST APT I1\nWASHINGTON DC 20001"
        assert normalize_address(address5) == expected5

        # Avoid false-positive USPSBoxType causing addresses drops (e.g., SPC 204)
        address6 = "1080 W 1ST ST SPC 204\nWASHINGTON DC 20001"
        expected6 = "1080 W 1ST ST SPC 204\nWASHINGTON DC 20001"
        assert normalize_address(address6) == expected6

        # Physical address with box (Puerto Rico layout)
        address7 = "320 CALLE 1 BOX 43\nSAN JUAN PR 00901"
        expected7 = "320 CALLE 1 BOX 43\nSAN JUAN PR 00901"
        assert normalize_address(address7) == expected7

        # Smart swap for address ending with digit
        address8 = "PERIDOT 27\nWASHINGTON DC 20001"
        expected8 = "27 PERIDOT\nWASHINGTON DC 20001"
        assert normalize_address(address8) == expected8
        
        # Duplicate line 1 and 2
        address9 = "45 MALL DR\n45 MALL DR\nSAN JUAN PR 00901"
        expected9 = "45 MALL DR\nSAN JUAN PR 00901"
        assert normalize_address(address9) == expected9

    def test_diacritics_and_punctuation(self) -> None:
        address = "123 Nórth Main St., Apt. #4-B\nNew York, NY (10001)"
        expected = "123 N MAIN ST APT 4-B\nNEW YORK NY 10001"
        assert normalize_address(address) == expected

    def test_po_box(self) -> None:
        address = "POST OFFICE BOX 11890\nOLD SAN JUAN STA\nSAN JUAN, PR 00902-1190"
        # PO Box could place 'OLD SAN JUAN STA' differently
        # We just assert it formats the PO BOX and state
        res = normalize_address(address)
        assert "PO BOX 11890" in res
        assert "SAN JUAN PR 00902-1190" in res

    def test_rural_route(self) -> None:
        cases = [
            (
                "RURAL ROUTE 91 BOX A7\nSOMEPLACE TX 77001",
                "RR 91 BOX A7\nSOMEPLACE TX 77001",
            ),
            ("RFD 82 BOX 12\nSOMEPLACE TX 77001", "RR 82 BOX 12\nSOMEPLACE TX 77001"),
            ("RD 51 # 25\nSOMEPLACE TX 77001", "RR 51 BOX 25\nSOMEPLACE TX 77001"),
            (
                "RFD Route 4 #87a\nSOMEPLACE TX 77001",
                "RR 4 BOX 87A\nSOMEPLACE TX 77001",
            ),
            (
                "RR 2 BOX 18 Bryan Dairy Rd\nSOMEPLACE TX 77001",
                "RR 2 BOX 18\nBRYAN DAIRY RD\nSOMEPLACE TX 77001",
            ),
            (
                "RURAL ROUTE 091 BOX A1\nSOMEPLACE TX 77001",
                "RR 91 BOX A1\nSOMEPLACE TX 77001",
            ),
            (
                "RR 1 BX 217\nSOMEPLACE TX 77001",
                "RR 1 BOX 217\nSOMEPLACE TX 77001",
            ),
            (
                "RR4 BOX 10007\nSOMEPLACE PR 00901",
                "RR 4 BOX 10007\nSOMEPLACE PR 00901",
            ),
            (
                "RUTA RURAL 4 BUZON 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
            (
                "RURAL ROUTE 4 BOX 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
            (
                "RFD 4 BOX 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
            (
                "RD 4 BOX 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
            (
                "RT 4 BOX 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
            (
                "RR 04 BOX 12\nSOMEPLACE PR 00901",
                "RR 4 BOX 12\nSOMEPLACE PR 00901",
            ),
        ]
        for address, expected in cases:
            with self.subTest(address=address):
                assert normalize_address(address) == expected

    def test_highway(self) -> None:
        address = "COUNTY HIGHWAY 140\nSOMEPLACE TX 77001"
        expected = "COUNTY HIGHWAY 140\nSOMEPLACE TX 77001"
        assert normalize_address(address) == expected

    def test_puerto_rico_urbanization(self) -> None:
        address = "URBANIZATION HIGHLAND GDNS\n123 CALLE MAIN\nSAN JUAN PR 00961"
        expected = "URB HIGHLAND GDNS\n123 CALLE MAIN\nSAN JUAN PR 00961"
        assert normalize_address(address) == expected

    def test_military(self) -> None:
        address = "UNIT 2050 BOX 4190\nAPO AP 96278-2050"
        expected = "UNIT 2050 BOX 4190\nAPO AP 96278-2050"
        assert normalize_address(address) == expected

    # note, US Address is us-specific, and may have issues with non-US addresses.
    def test_canada(self) -> None:
        address = "1010 CLEAR STREET\nOTTAWA ON K1A OB1"
        expected = "1010 CLEAR ST\nOTTAWA ON K1A OB1"
        assert normalize_address(address) == expected

    def test_diacritics_complex(self) -> None:
        address = "Âbčdëf N Gâté Blvd\nSan José, CA 95112"
        # removes diacritics, normalizes
        res = normalize_address(address)
        assert "ABCDEF N GATE BLVD" in res
        assert "SAN JOSE CA 95112" in res

    def test_department_of_state(self) -> None:
        address = "UNIT 9900 BOX 0500\nDPO AE 09701-0500"
        expected = "UNIT 9900 BOX 500\nDPO AE 09701-0500"  # box removes leading zeros
        assert normalize_address(address) == expected

    def test_alphanumeric_ranges(self) -> None:
        address = "N6W23001 BLUEMOUND ROAD\nSOMEPLACE TX 77001"
        expected = "N6W23001 BLUEMOUND RD\nSOMEPLACE TX 77001"
        assert normalize_address(address) == expected

    def test_hyphenated_address_ranges(self) -> None:
        address = "112-10 BRONX RD\nNEW YORK NY 10001"
        expected = "112-10 BRONX RD\nNEW YORK NY 10001"
        assert normalize_address(address) == expected

        # with spaces around hyphen should compress
        address2 = "112 - 10 BRONX RD\nNEW YORK NY 10001"
        assert normalize_address(address2) == expected

    def test_grid_style_addresses(self) -> None:
        # 1. Decimal addresses should preserve decimal period
        address1 = "39.2 RD\nSOMEPLACE CO 81501"
        expected1 = "39.2 RD\nSOMEPLACE CO 81501"
        assert normalize_address(address1) == expected1

        # 2. Grid styling Salt Lake City (double directionals)
        address2 = "842 E 1700 S\nSALT LAKE CITY UT 84105"
        expected2 = "842 E 1700 S\nSALT LAKE CITY UT 84105"
        assert normalize_address(address2) == expected2

    def test_fractional_addresses(self) -> None:
        address = "123 1/2 MAIN STREET\nSOMEPLACE TX 77001"
        # The spaces aren't typically removed by usaddress suffix tokenizer, they are just appended
        res = normalize_address(address)
        assert "123 1/2" in res
        assert "MAIN ST" in res

    def test_puerto_rico_exceptions_positive(self) -> None:
        # Exception 1: known as extensiones... MUST NOT place the abbreviation URB prior
        address1 = "URB EXT VISTA BELLA\n123 CALLE MAIN\nPONCE PR 00731"
        assert normalize_address(address1) == "EXT VISTA BELLA\n123 CALLE MAIN\nPONCE PR 00731"

        # Exception 2: "The following urbanization names stand alone"
        address2 = "URB JARDINES FAGOTA\n123 CALLE MAIN\nPONCE PR 00731"
        assert "JARD FAGOTA" in normalize_address(address2)

        # Exception 3: Stripping hyphens from Alphanumeric house numbers
        address3 = "A-17 CALLE AMAPOLA\nPONCE PR 00731"
        assert normalize_address(address3) == "A17 CALLE AMAPOLA\nPONCE PR 00731"

        address4 = "B-17A CALLE 1\nPONCE PR 00731"
        assert normalize_address(address4) == "B17A CALLE 1\nPONCE PR 00731"

        # Exception 4: stand alone format substitution (A17 URB JARDINES FAGOTA)
        address5 = "A17 URB JARDINES FAGOTA\nPONCE PR 00731"
        assert normalize_address(address5) == "A17 JARD FAGOTA\nPONCE PR 00731"

    def test_puerto_rico_station_reorder(self) -> None:
        # Station line is below PO Box line - SHOULD be reordered above it
        address = "PO BOX 1190\nOLD SAN JUAN STA\nSAN JUAN PR 00902-1190"
        expected = "OLD SAN JUAN STA\nPO BOX 1190\nSAN JUAN PR 00902-1190"
        assert normalize_address(address) == expected

    def test_puerto_rico_exceptions_negative(self) -> None:
        # A normal URB that doesn't have an exception name following it
        address = "URBANIZATION ROYAL OAKS\n123 CALLE MAIN\nBAYAMON PR 00961"
        expected = "URB ROYAL OAKS\n123 CALLE MAIN\nBAYAMON PR 00961"
        assert normalize_address(address) == expected

        # A line containing PR, but inside a word (e.g., POTATOPR)
        # Should not trigger is_pr behavior, meaning Station reordering is skipped
        address2 = "PO BOX 1190\nOLD SAN JUAN STA\n123 POTATOPR ST\nNEW YORK NY 10001"
        res = normalize_address(address2)
        # Verify it stays in original PO Box / Sta order because is_pr is False!
        assert res.startswith("PO BOX 1190")

    def test_private_mailbox(self) -> None:
        address1 = "123 MAIN STREET PMB 4545\nHERNDON VA 22071-2716"
        expected1 = "123 MAIN ST PMB 4545\nHERNDON VA 22071-2716"
        assert normalize_address(address1) == expected1

        address2 = "10 MAIN ST STE 11 PMB 234\nHERNDON VA 22071"
        expected2 = "10 MAIN ST STE 11 PMB 234\nHERNDON VA 22071"
        assert normalize_address(address2) == expected2

    def test_appendix_c_highways(self) -> None:
        cases = [
            (
                "COUNTY HIGHWAY 140\nHERNDON VA 22071",
                "COUNTY HIGHWAY 140\nHERNDON VA 22071",
            ),
            (
                "COUNTY HWY 60E\nHERNDON VA 22071",
                "COUNTY HIGHWAY 60E\nHERNDON VA 22071",
            ),
            ("CNTY HWY 20\nHERNDON VA 22071", "COUNTY HIGHWAY 20\nHERNDON VA 22071"),
            ("COUNTY RD 441\nHERNDON VA 22071", "COUNTY ROAD 441\nHERNDON VA 22071"),
            ("CR 1185\nHERNDON VA 22071", "COUNTY ROAD 1185\nHERNDON VA 22071"),
            ("CNTY RD 33\nHERNDON VA 22071", "COUNTY ROAD 33\nHERNDON VA 22071"),
            (
                "CA COUNTY RD 150\nHERNDON VA 22071",
                "CA COUNTY ROAD 150\nHERNDON VA 22071",
            ),
            (
                "CALIFORNIA COUNTY ROAD 555\nHERNDON VA 22071",
                "CA COUNTY ROAD 555\nHERNDON VA 22071",
            ),
            ("EXPRESSWAY 55\nHERNDON VA 22071", "EXPRESSWAY 55\nHERNDON VA 22071"),
            ("FARM to MARKET 1200\nHERNDON VA 22071", "FM 1200\nHERNDON VA 22071"),
            ("FM 187\nHERNDON VA 22071", "FM 187\nHERNDON VA 22071"),
            ("HWY FM 1320\nHERNDON VA 22071", "FM 1320\nHERNDON VA 22071"),
            ("HWY 64\nHERNDON VA 22071", "HIGHWAY 64\nHERNDON VA 22071"),
            ("HWY 11 BYPASS\nHERNDON VA 22071", "HIGHWAY 11 BYP\nHERNDON VA 22071"),
            (
                "HWY 66 FRONTAGE ROAD\nHERNDON VA 22071",
                "HIGHWAY 66 FRONTAGE RD\nHERNDON VA 22071",
            ),
            (
                "HIGHWAY 3 BYP ROAD\nHERNDON VA 22071",
                "HIGHWAY 3 BYPASS RD\nHERNDON VA 22071",
            ),
            ("I10\nHERNDON VA 22071", "INTERSTATE 10\nHERNDON VA 22071"),
            ("IH280\nHERNDON VA 22071", "INTERSTATE 280\nHERNDON VA 22071"),
            (
                "INTERSTATE HWY 680\nHERNDON VA 22071",
                "INTERSTATE 680\nHERNDON VA 22071",
            ),
            ("I 55 BYPASS\nHERNDON VA 22071", "INTERSTATE 55 BYP\nHERNDON VA 22071"),
            (
                "I 26 BYP ROAD\nHERNDON VA 22071",
                "INTERSTATE 26 BYPASS RD\nHERNDON VA 22071",
            ),
            (
                "I 44 FRONTAGE ROAD\nHERNDON VA 22071",
                "INTERSTATE 44 FRONTAGE RD\nHERNDON VA 22071",
            ),
            ("RD 5A\nHERNDON VA 22071", "ROAD 5A\nHERNDON VA 22071"),
            ("RT 88\nHERNDON VA 22071", "ROUTE 88\nHERNDON VA 22071"),
            ("RTE 95\nHERNDON VA 22071", "ROUTE 95\nHERNDON VA 22071"),
            ("RANCH RD 620\nHERNDON VA 22071", "RANCH ROAD 620\nHERNDON VA 22071"),
            ("ST HIGHWAY 303\nHERNDON VA 22071", "STATE HIGHWAY 303\nHERNDON VA 22071"),
            ("STATE HWY 60\nHERNDON VA 22071", "STATE HIGHWAY 60\nHERNDON VA 22071"),
            ("SR 220\nHERNDON VA 22071", "STATE ROAD 220\nHERNDON VA 22071"),
            ("ST RD 86\nHERNDON VA 22071", "STATE ROAD 86\nHERNDON VA 22071"),
            ("SR MM\nHERNDON VA 22071", "STATE ROUTE MM\nHERNDON VA 22071"),
            ("ST RT 175\nHERNDON VA 22071", "STATE ROUTE 175\nHERNDON VA 22071"),
            ("STATE RTE 260\nHERNDON VA 22071", "STATE ROUTE 260\nHERNDON VA 22071"),
            ("TOWNSHIP RD 20\nHERNDON VA 22071", "TOWNSHIP ROAD 20\nHERNDON VA 22071"),
            ("TSR 45\nHERNDON VA 22071", "TOWNSHIP ROAD 45\nHERNDON VA 22071"),
            ("US 41 SW\nHERNDON VA 22071", "US HIGHWAY 41 SW\nHERNDON VA 22071"),
            ("US HWY 44\nHERNDON VA 22071", "US HIGHWAY 44\nHERNDON VA 22071"),
            ("KENTUCKY 440\nHERNDON VA 22071", "KY HIGHWAY 440\nHERNDON VA 22071"),
            (
                "KENTUCKY HIGHWAY 189\nHERNDON VA 22071",
                "KY HIGHWAY 189\nHERNDON VA 22071",
            ),
            ("KY 1207\nHERNDON VA 22071", "KY HIGHWAY 1207\nHERNDON VA 22071"),
            ("KY HWY 75\nHERNDON VA 22071", "KY HIGHWAY 75\nHERNDON VA 22071"),
            ("KY ST HWY 1\nHERNDON VA 22071", "KY STATE HIGHWAY 1\nHERNDON VA 22071"),
            (
                "KENTUCKY STATE HIGHWAY 625\nHERNDON VA 22071",
                "KY STATE HIGHWAY 625\nHERNDON VA 22071",
            ),
            ("1984 US 70 HWY\nHERNDON VA 22071", "1984 US HIGHWAY 70\nHERNDON VA 22071"),
        ]
        for address, expected in cases:
            with self.subTest(address=address):
                assert normalize_address(address) == expected

    def test_all_diacritics(self) -> None:
        from matching.constants import DIACRITICS

        # Test a concatenated string of all diacritic characters
        diacritic_str = "".join(DIACRITICS.keys())
        expected_str = "".join(DIACRITICS.values())
        res = normalize_address(f"123 {diacritic_str} STREET\nWASHINGTON DC 20001")
        # Address split/format might uppercase and normalize
        # We check the street name is fully converted to the expected mapped ascii values
        assert expected_str in res


if __name__ == "__main__":
    unittest.main()

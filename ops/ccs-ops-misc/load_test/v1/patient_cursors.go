package main

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"time"
)

var (
	combinedClientCertKey = requireEnv("CLIENT_CERT")
	contractID            = requireEnv("CONTRACT_ID")
	contractMonth         = requireEnv("CONTRACT_MONTH")
	port                  = requireEnv("BFD_PORT")
	host                  = requireEnv("BFD_HOST")
	endpoint              = fmt.Sprintf("https://%s:%s/v1/fhir/Patient?", host, port)
)

type BFDClient struct {
	*http.Client
	LogFile *os.File
}

type PatientsByContract struct {
	Link []Link `json:"link"`
}

type Link struct {
	Relation string `json:"relation"`
	URL      string `json:"url"`
}

func (c *BFDClient) getPatientsByContract(cursor string) (PatientsByContract, error) {
	contract := fmt.Sprintf("https://bluebutton.cms.gov/resources/variables/ptdcntrct%s|%s", contractMonth, contractID)
	var pbc PatientsByContract
	v := url.Values{}
	v.Set("_has:Coverage.extension", contract)
	v.Set("_count", "500")
	v.Set("_format", "json")
	if cursor != "" {
		v.Set("cursor", cursor)
	}

	route := endpoint + v.Encode()

	res, err := c.Get(route)
	if err != nil {
		return pbc, err
	}

	err = json.NewDecoder(res.Body).Decode(&pbc)
	if err != nil {
		return pbc, err
	}
	defer res.Body.Close()

	return pbc, nil
}

func includesNext(links []Link) bool {
	var exists bool
	for _, link := range links {
		if link.Relation == "next" {
			exists = true
		}
	}

	return exists
}

func (c *BFDClient) GetAllCursors() error {
	cursor := ""
	var cursors []string
	var nextExists bool

	for {
		fmt.Print(".")
		pbc, err := c.getPatientsByContract(cursor)
		if err != nil {
			return err
		}

		nextExists = includesNext(pbc.Link)

		for _, link := range pbc.Link {
			if link.Relation == "next" {
				params, err := url.ParseQuery(link.URL)
				if err != nil {
					return err
				}

				cursor = params["cursor"][0]
				if cursor != "" {
					cursors = append(cursors, cursor)
					c.LogFile.Write([]byte(cursor + "\n"))
				}
			}
		}

		if nextExists == false {
			return nil
		}
	}
}

func requireEnv(name string) string {
	val := os.Getenv(name)
	if val == "" {
		log.Fatalf("did not find required env var: %s\n", name)
	}

	return val
}

func main() {
	// create log file where cursors will be written
	t := time.Now()
	formatted := t.Format("20060102150405")
	fileName := fmt.Sprintf("cursors-%s.txt", formatted)
	f, err := os.Create(fileName)
	if err != nil {
		log.Fatal("failed creating cursor log: ", err)
	}
	defer f.Close()

	// load client cert
	cert, err := tls.LoadX509KeyPair(combinedClientCertKey, combinedClientCertKey)
	if err != nil {
		log.Fatal("failed loading client cert: ", err)
	}

	client := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{
				Certificates:       []tls.Certificate{cert},
				InsecureSkipVerify: true,
			},
		},
	}

	// the real action
	bfd := BFDClient{client, f}

	err = bfd.GetAllCursors()
	if err != nil {
		log.Fatal("failed fetching cursors: ", err)
	}

	fmt.Printf("\ncompleted! wrote to %s\n", fileName)
}

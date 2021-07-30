package main

import (
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"sort"
	"strings"
	"sync"
	"time"
)

const (
	numberOfTries = 5
)

var (
	combinedClientCertKey = requireEnv("CLIENT_CERT")
	baseURL               = requireEnv("BASE_URL")
	endpointsFilename     = requireEnv("ENDPOINTS")
)

type Endpoint struct {
	Label   string   `json:"label"`
	Path    string   `json:"path"`
	Headers []string `json:"headers"`
}

type EndpointResult struct {
	AvgDuration int
	Path        string
}

type BFDClient struct {
	*http.Client
}

func (bfd *BFDClient) getDurationForEndpoint(endpoint Endpoint) time.Duration {
	req, err := http.NewRequest("GET", baseURL+endpoint.Path, nil)
	if err != nil {
		fmt.Println(baseURL + endpoint.Path)
		log.Fatalf("error creating request: %s", endpoint.Path)
	}

	if len(endpoint.Headers) > 0 {
		for _, header := range endpoint.Headers {
			splitHeader := strings.Split(header, ":")
			if len(splitHeader) != 2 {
				log.Fatalf("parsed invalid header: '%s'; must be in format Name: value", header)
			}
			req.Header.Set(splitHeader[0], splitHeader[1])
		}
	}
	req.Header.Set("BULK-CLIENTID", "db-smoketest")

	start := time.Now()
	res, err := bfd.Do(req)
	if err != nil {
		log.Fatalf("call failed for endpoint: '%s'\n", endpoint.Path)
	}

	if res.StatusCode > 399 {
		log.Fatalf("unsuccessful request with status code: '%d' for endpoint: '%s'", res.StatusCode, endpoint)
	}

	return time.Since(start)
}

func requireEnv(name string) string {
	val := os.Getenv(name)
	if val == "" {
		log.Fatalf("did not find required env var: %s\n", name)
	}

	return val
}

func loadEndpoints(filename string) ([]Endpoint, error) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	var body struct {
		Endpoints []Endpoint `json:"endpoints"`
	}

	err = json.Unmarshal(data, &body)
	if err != nil {
		return nil, err
	}

	return body.Endpoints, nil
}

func (bfd *BFDClient) logAvgTimeForEndpoint(wg *sync.WaitGroup, results chan<- EndpointResult, endpoint Endpoint) {
	defer wg.Done()
	var sumMilliseconds int64

	for i := 0; i < numberOfTries; i++ {
		duration := bfd.getDurationForEndpoint(endpoint)
		sumMilliseconds += duration.Milliseconds()
	}

	avgMilliseconds := int(sumMilliseconds) / numberOfTries

	results <- EndpointResult{AvgDuration: avgMilliseconds, Path: endpoint.Path}
	fmt.Print(".")
}

func sortResults(results []EndpointResult) []EndpointResult {
	sort.Slice(results, func(i, j int) bool {
		return results[i].Path < results[j].Path
	})

	return results
}

func formatResults(results []EndpointResult) string {
	output := "Avg(ms)\tEndpoint\n"
	for _, result := range results {
		line := fmt.Sprintf("%d\t%s\n", result.AvgDuration, result.Path)
		output += line
	}

	return output
}

func main() {
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

	endpoints, err := loadEndpoints(endpointsFilename)
	if err != nil {
		log.Fatal("failed loading endpoints file: ", err)
	}

	bfd := BFDClient{client}

	var wg sync.WaitGroup
	resultChan := make(chan EndpointResult, len(endpoints))
	for _, e := range endpoints {
		wg.Add(1)
		go bfd.logAvgTimeForEndpoint(&wg, resultChan, e)
	}

	wg.Wait()

	var results []EndpointResult
	for i := 0; i < len(endpoints); i++ {
		r := <-resultChan
		results = append(results, r)
	}

	fmt.Print("\n\n")

	sorted := sortResults(results)
	formatted := formatResults(sorted)

	fmt.Println(formatted)
}

package gov.hhs.cms.bluebutton.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.hl7.fhir.dstu21.model.Patient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.GenericClient;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

/**
 * Provides utilities for the tests.
 */
public final class FhirClientHelper {
	public static final String FHIR_URL_API_OPEN = "https://api.bbonfhir.com/api/v1/open";
	public static final String FHIR_URL_API_BASIC_AUTH = "https://api.bbonfhir.com/fhir";

	private static final int CLIENT_TIMEOUT = 600 * 1000;

	private static List<ApiUser> apiUsers = loadApiUsers();

	/**
	 * @param fhirServerUrlText
	 *            a {@link String} for the URL of the FHIR server to create a
	 *            client for
	 * @return a new FHIR {@link IGenericClient} instance
	 */
	public static IGenericClient createFhirClient(String fhirServerUrlText) {
		return createFhirClient(fhirServerUrlText, (HttpClient) null);
	}

	/**
	 * @param fhirServerUrlText
	 *            a {@link String} for the URL of the FHIR server to create a
	 *            client for
	 * @param basicAuthCrendentials
	 *            the HTTP "Basic Auth" credentials to use
	 * @return a new FHIR {@link IGenericClient} instance
	 */
	public static IGenericClient createFhirClient(String fhirServerUrlText,
			UsernamePasswordCredentials basicAuthCrendentials) {
		return createFhirClient(fhirServerUrlText, createHttpClient(basicAuthCrendentials));
	}

	/**
	 * @param fhirServerUrlText
	 *            a {@link String} for the URL of the FHIR server to create a
	 *            client for
	 * @param httpClient
	 *            the already-configured Apache {@link HttpClient} to use, which
	 *            allows for authentication to be configured (amongst other
	 *            things), or <code>null</code> to just use an
	 *            {@link HttpClient} with the default configuration
	 * @return a new FHIR {@link IGenericClient} instance
	 */
	private static IGenericClient createFhirClient(String fhirServerUrlText, HttpClient httpClient) {
		FhirContext fhirContext = FhirContext.forDstu2_1();

		if (httpClient != null)
			fhirContext.getRestfulClientFactory().setHttpClient(httpClient);
		else
			fhirContext.getRestfulClientFactory().setSocketTimeout(CLIENT_TIMEOUT);
		GenericClient client = (GenericClient) fhirContext.newRestfulGenericClient(fhirServerUrlText);

		/*
		 * FIXME This is a hack to workaround a bug in the BlueButton frontend.
		 */
		client.setDontValidateConformance(true);

		LoggingInterceptor fhirClientLogging = new LoggingInterceptor();
		fhirClientLogging.setLogRequestBody(false);
		fhirClientLogging.setLogResponseBody(false);
		client.registerInterceptor(fhirClientLogging);

		return client;
	}

	/**
	 * (This is largely a copy-paste of
	 * {@link RestfulClientFactory#getHttpClient()}, but allows for basic auth
	 * to be configured.)
	 * 
	 * @param basicAuthCredentials
	 *            the HTTP "Basic Auth" credentials for the new
	 *            {@link HttpClient} to use
	 * @return a new {@link HttpClient} instance
	 */
	private static CloseableHttpClient createHttpClient(Credentials basicAuthCredentials) {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(5000,
				TimeUnit.MILLISECONDS);
		@SuppressWarnings("deprecation")
		RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(CLIENT_TIMEOUT)
				.setConnectTimeout(RestfulClientFactory.DEFAULT_CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(RestfulClientFactory.DEFAULT_CONNECTION_REQUEST_TIMEOUT)
				.setStaleConnectionCheckEnabled(true).build();

		HttpClientBuilder builder = HttpClients.custom().setConnectionManager(connectionManager)
				.setDefaultRequestConfig(defaultRequestConfig).disableCookieManagement();

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, basicAuthCredentials);
		builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
		builder.setDefaultCredentialsProvider(credsProvider);
		CloseableHttpClient myHttpClient = builder.build();
		return myHttpClient;
	}

	/**
	 * @return the {@link ApiUser}s that can be used to test the API
	 */
	public static List<ApiUser> getApiUsers() {
		return apiUsers;
	}

	/**
	 * @return the {@link ApiUser}s available to test against
	 */
	private static List<ApiUser> loadApiUsers() {
		InputStream userDataStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("bbonfhir-api-logins.json");
		ObjectMapper mapper = new ObjectMapper();
		try {
			ApiUsers userData = mapper.readValue(userDataStream, ApiUsers.class);
			return userData.getUserEntries();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Models the <code>src/main/resources/bbonfhir-api-logins.json</code>
	 * resource, for parsing via Jackson data binding.
	 */
	public static final class ApiUsers {
		@JsonProperty("userlist")
		private List<ApiUser> userEntries;

		/**
		 * Jackson data binding requires this class to have a default
		 * constructor.
		 */
		public ApiUsers() {
			this.userEntries = null;
		}

		/**
		 * @return the {@link ApiUser}s in this {@link ApiUsers} object
		 */
		public List<ApiUser> getUserEntries() {
			return userEntries;
		}
	}

	/**
	 * Models <code>api.bbonfhir.com</code> beneficiary/user accounts.
	 */
	@JsonIgnoreProperties({ "eob_count" })
	public static final class ApiUser {
		@JsonProperty("user")
		private final String username;

		@JsonProperty("password")
		private final String password;

		@JsonProperty("fhir_url_id")
		private final String patientId;

		/**
		 * Constructs a new {@link ApiUser} instance.
		 * 
		 * @param username
		 *            the value to use for {@link #getUsername()}
		 * @param password
		 *            the value to use for {@link #getPassword()}
		 * @param patientId
		 *            the value to use for {@link #getPatientId()}
		 */
		public ApiUser(String username, String password, String patientId) {
			this.username = username;
			this.password = password;
			this.patientId = patientId;
		}

		/**
		 * Jackson data binding requires this class to have a default
		 * constructor.
		 */
		public ApiUser() {
			this(null, null, null);
		}

		/**
		 * @return the authorization username
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * @return the authorization password
		 */
		public String getPassword() {
			return password;
		}

		/**
		 * @return the {@link Patient#getId()} of the resource associated with
		 *         the beneficiary
		 */
		public String getPatientId() {
			return patientId;
		}
	}
}

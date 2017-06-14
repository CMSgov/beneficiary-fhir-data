package gov.hhs.cms.bluebutton.server.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

/**
 * <p>
 * The primary {@link Servlet} for this web application. Uses the
 * <a href="http://hapifhir.io/">HAPI FHIR</a> framework to provide a fully
 * functional FHIR API server that queries stored RIF data from the CCW and
 * converts it to the proper FHIR format "on the fly".
 * </p>
 */
public class BlueButtonStu3Server extends RestfulServer {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@link BlueButtonStu3Server} instance.
	 */
	public BlueButtonStu3Server() {
		super(FhirContext.forDstu3());
		setServerAddressStrategy(ApacheProxyAddressStrategy.forHttp());
	}

	/**
	 * @see ca.uhn.fhir.rest.server.RestfulServer#initialize()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		/*
		 * Grab the application's Spring WebApplicationContext from the web
		 * container. We can use this to retrieve beans (and anything that needs
		 * Spring injection/autowiring, e.g. anything that accesses the DB, must
		 * be a bean).
		 */
		WebApplicationContext springContext = ContextLoaderListener.getCurrentWebApplicationContext();

		// Each IResourceProvider adds support for a specific FHIR resource.
		List<IResourceProvider> resourceProviders = springContext
				.getBean(SpringConfiguration.BLUEBUTTON_STU3_RESOURCE_PROVIDERS, List.class);
		setResourceProviders(resourceProviders);

		/*
		 * Each "plain" provider has one or more annotated methods that provides
		 * support for non-resource-type methods, such as transaction, and
		 * global history.
		 */
		List<Object> plainProviders = new ArrayList<>();
		setPlainProviders(plainProviders);

		/*
		 * Register the HAPI server interceptors that have been configured in
		 * Spring.
		 */
		Collection<IServerInterceptor> hapiInterceptors = springContext.getBeansOfType(IServerInterceptor.class)
				.values();
		for (IServerInterceptor hapiInterceptor : hapiInterceptors) {
			this.registerInterceptor(hapiInterceptor);
		}
		/*
		 * Enable CORS.
		 */
		CorsConfiguration config = new CorsConfiguration();
		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("Content-Type");
		config.addAllowedOrigin("*");
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		registerInterceptor(corsInterceptor);

		// Enable ETag Support (this is already the default)
		setETagSupport(ETagSupportEnum.ENABLED);

		// Default to XML and pretty printing.
		setDefaultResponseEncoding(EncodingEnum.XML);
		setDefaultPrettyPrint(true);
	}
}

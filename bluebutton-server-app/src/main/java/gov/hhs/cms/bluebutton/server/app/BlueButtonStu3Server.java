package gov.hhs.cms.bluebutton.server.app;

import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import com.codahale.metrics.MetricRegistry;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

/**
 * The primary {@link Servlet} for this web application. Uses the
 * <a href="http://hapifhir.io/">HAPI FHIR</a> framework to provide a fully
 * functional FHIR API server that queries stored RIF data from the CCW and
 * converts it to the proper FHIR format "on the fly".
 * 
 * @see BlueButtonStu3ServerConfig
 */
public class BlueButtonStu3Server extends RestfulServer {
	private static final long serialVersionUID = 1L;

	static final Logger LOGGER = LoggerFactory.getLogger(BlueButtonStu3Server.class);

	/**
	 * Constructs a new {@link BlueButtonStu3Server} instance.
	 */
	public BlueButtonStu3Server() {
		setServerAddressStrategy(ApacheProxyAddressStrategy.forHttp());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		// This servlet provides STU3 resources.
		FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU3;
		setFhirContext(new FhirContext(fhirVersion));

		/*
		 * Get the spring context from the web container (it's declared in
		 * web.xml).
		 */
		WebApplicationContext webAppContext = ContextLoaderListener.getCurrentWebApplicationContext();

		/*
		 * There should be 1 resource provider per supported FHIR resource type,
		 * e.g. Patient, ExplanationOfBenefit, etc.
		 */
		setResourceProviders(webAppContext.getBean("fhirStu3ResourceProviders", List.class));

		/*
		 * The system provider implements non-resource-type methods, such as
		 * transaction, and global history.
		 */
		setPlainProviders(webAppContext.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class));

		IFhirSystemDao<org.hl7.fhir.dstu3.model.Bundle, Meta> systemDao = myAppCtx.getBean("mySystemDaoDstu3",
				IFhirSystemDao.class);
		JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao,
				myAppCtx.getBean(DaoConfig.class));
		confProvider.setImplementationDescription("Example Server");
		setServerConformanceProvider(confProvider);

		// Enable ETag Support (this is already the default)
		setETagSupport(ETagSupportEnum.ENABLED);

		// Default to XML and pretty printing
		setDefaultPrettyPrint(true);
		setDefaultResponseEncoding(EncodingEnum.XML);

		/*
		 * Stick with the old FIFO memory-based paging approach, as FHIR server
		 * RAM is more scalable for us than DB capacity.
		 */
		setPagingProvider(webAppContext.getBean(IPagingProvider.class));

		/*
		 * Load interceptors for the server from Spring (these are defined in
		 * BlueButtonStu3ServerConfig.java)
		 */
		Collection<IServerInterceptor> interceptorBeans = webAppContext.getBeansOfType(IServerInterceptor.class)
				.values();
		for (IServerInterceptor interceptor : interceptorBeans) {
			this.registerInterceptor(interceptor);
		}

		/*
		 * Bind the MetricRegistry, so that `InstrumentedFilter` (configured in
		 * web.xml) can work.
		 */
		this.getServletContext().setAttribute("com.codahale.metrics.servlet.InstrumentedFilter.registry",
				webAppContext.getBean(MetricRegistry.class));
	}
}

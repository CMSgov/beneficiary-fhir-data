package gov.hhs.cms.bluebutton.server.app;

import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.hl7.fhir.dstu3.model.Meta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import com.codahale.metrics.MetricRegistry;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu1;
import ca.uhn.fhir.jpa.provider.JpaConformanceProviderDstu2;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu1;
import ca.uhn.fhir.jpa.provider.JpaSystemProviderDstu2;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.provider.dstu3.JpaSystemProviderDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.MetaDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;

/**
 * The primary {@link Servlet} for this web application. Uses the
 * <a href="http://hapifhir.io/">HAPI FHIR</a> framework to provide a fully
 * functional and persistent FHIR API server.
 * 
 * @see FhirServerConfig
 */
public class BlueButtonServer extends RestfulServer {
	private static final long serialVersionUID = 1L;

	static final Logger LOGGER = LoggerFactory.getLogger(BlueButtonServer.class);

	private WebApplicationContext myAppCtx;

	/**
	 * Constructs a new {@link BlueButtonServer} instance.
	 */
	public BlueButtonServer() {
		super();

		setServerAddressStrategy(ApacheProxyAddressStrategy.forHttp());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initialize() throws ServletException {
		super.initialize();

		/*
		 * We want to support FHIR DSTU3 format. This means that the server will
		 * use the DSTU3 bundle format and other DSTU3 encoding changes.
		 *
		 * If you want to use DSTU1 instead, change the following line, and
		 * change the 2 occurrences of dstu3 in web.xml to dstu1
		 */
		FhirVersionEnum fhirVersion = FhirVersionEnum.DSTU3;
		setFhirContext(new FhirContext(fhirVersion));

		// Get the spring context from the web container (it's declared in
		// web.xml)
		myAppCtx = ContextLoaderListener.getCurrentWebApplicationContext();

		/*
		 * The hapi-fhir-server-resourceproviders-dev.xml file is a spring
		 * configuration file which is automatically generated as a part of
		 * hapi-fhir-jpaserver-base and contains bean definitions for a resource
		 * provider for each resource type
		 */
		String resourceProviderBeanName;
		if (fhirVersion == FhirVersionEnum.DSTU1) {
			resourceProviderBeanName = "myResourceProvidersDstu1";
		} else if (fhirVersion == FhirVersionEnum.DSTU2) {
			resourceProviderBeanName = "myResourceProvidersDstu2";
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {
			resourceProviderBeanName = "myResourceProvidersDstu3";
		} else {
			throw new IllegalStateException();
		}
		List<IResourceProvider> beans = myAppCtx.getBean(resourceProviderBeanName, List.class);
		setResourceProviders(beans);

		/*
		 * The system provider implements non-resource-type methods, such as
		 * transaction, and global history.
		 */
		Object systemProvider;
		if (fhirVersion == FhirVersionEnum.DSTU1) {
			systemProvider = myAppCtx.getBean("mySystemProviderDstu1", JpaSystemProviderDstu1.class);
		} else if (fhirVersion == FhirVersionEnum.DSTU2) {
			systemProvider = myAppCtx.getBean("mySystemProviderDstu2", JpaSystemProviderDstu2.class);
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {
			systemProvider = myAppCtx.getBean("mySystemProviderDstu3", JpaSystemProviderDstu3.class);
		} else {
			throw new IllegalStateException();
		}
		setPlainProviders(systemProvider);

		/*
		 * The conformance provider exports the supported resources, search
		 * parameters, etc for this server. The JPA version adds resource counts
		 * to the exported statement, so it is a nice addition.
		 */
		if (fhirVersion == FhirVersionEnum.DSTU1) {
			IFhirSystemDao<List<IResource>, MetaDt> systemDao = myAppCtx.getBean("mySystemDaoDstu1",
					IFhirSystemDao.class);
			JpaConformanceProviderDstu1 confProvider = new JpaConformanceProviderDstu1(this, systemDao);
			confProvider.setImplementationDescription("Example Server");
			setServerConformanceProvider(confProvider);
		} else if (fhirVersion == FhirVersionEnum.DSTU2) {
			IFhirSystemDao<Bundle, MetaDt> systemDao = myAppCtx.getBean("mySystemDaoDstu2", IFhirSystemDao.class);
			JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(this, systemDao,
					myAppCtx.getBean(DaoConfig.class));
			confProvider.setImplementationDescription("Example Server");
			setServerConformanceProvider(confProvider);
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {
			IFhirSystemDao<org.hl7.fhir.dstu3.model.Bundle, Meta> systemDao = myAppCtx.getBean("mySystemDaoDstu3",
					IFhirSystemDao.class);
			JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(this, systemDao,
					myAppCtx.getBean(DaoConfig.class));
			confProvider.setImplementationDescription("Example Server");
			setServerConformanceProvider(confProvider);
		} else {
			throw new IllegalStateException();
		}

		/*
		 * Enable ETag Support (this is already the default)
		 */
		setETagSupport(ETagSupportEnum.ENABLED);

		/*
		 * Default to XML and pretty printing
		 */
		setDefaultPrettyPrint(true);
		setDefaultResponseEncoding(EncodingEnum.XML);

		/*
		 * -- New in HAPI FHIR 1.5 -- This configures the server to page search
		 * results to and from the database, instead of only paging them to
		 * memory. This may mean a performance hit when performing searches that
		 * return lots of results, but makes the server much more scalable.
		 */
		setPagingProvider(myAppCtx.getBean(DatabaseBackedPagingProvider.class));

		/*
		 * Load interceptors for the server from Spring (these are defined in
		 * FhirServerConfig.java)
		 */
		Collection<IServerInterceptor> interceptorBeans = myAppCtx.getBeansOfType(IServerInterceptor.class).values();
		for (IServerInterceptor interceptor : interceptorBeans) {
			this.registerInterceptor(interceptor);
		}

		/*
		 * Bind the MetricRegistry, so that `InstrumentedFilter` (configured in
		 * web.xml) can work.
		 */
		this.getServletContext().setAttribute("com.codahale.metrics.servlet.InstrumentedFilter.registry",
				myAppCtx.getBean(MetricRegistry.class));
	}
}

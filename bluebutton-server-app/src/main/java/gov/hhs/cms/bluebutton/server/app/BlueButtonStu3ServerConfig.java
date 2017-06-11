package gov.hhs.cms.bluebutton.server.app;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * Provides the configuration for {@link BlueButtonStu3Server} (and is activated
 * via Spring classpath scanning).
 */
@Configuration
public class BlueButtonStu3ServerConfig extends BaseJavaConfigDstu3 {
	/**
	 * @see ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3#resourceProvidersDstu3()
	 */
	@Bean(name = "fhirStu3ResourceProviders")
	@Override
	public List<IResourceProvider> resourceProvidersDstu3() {
		/*
		 * This is overridden to reduce the default "surface area" of our FHIR
		 * server: only support the resource types that are actually needed.
		 */

		List<IResourceProvider> retVal = new ArrayList<IResourceProvider>();
		retVal.add(rpBundleDstu3());
		retVal.add(rpCapabilityStatementDstu3());
		retVal.add(rpCoverageDstu3());
		retVal.add(rpExplanationOfBenefitDstu3());
		retVal.add(rpOrganizationDstu3());
		retVal.add(rpPatientDstu3());
		retVal.add(rpPractitionerDstu3());
		return retVal;
	}

	/**
	 * @see ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3#resourceDaosDstu3()
	 */
	@Bean(name = "myResourceDaosDstu3")
	@Override
	public List<IFhirResourceDao<?>> resourceDaosDstu3() {
		/*
		 * This is overridden to reduce the default "surface area" of our FHIR
		 * server: only support the resource types that are actually needed.
		 */

		List<IFhirResourceDao<?>> retVal = new ArrayList<IFhirResourceDao<?>>();
		retVal.add(daoBundleDstu3());
		retVal.add(daoCapabilityStatementDstu3());
		retVal.add(daoCoverageDstu3());
		retVal.add(daoExplanationOfBenefitDstu3());
		retVal.add(daoOrganizationDstu3());
		retVal.add(daoPatientDstu3());
		retVal.add(daoPractitionerDstu3());
		return retVal;
	}

	/**
	 * @return the {@link MetricRegistry} for the application, which can be used
	 *         to collect statistics on the application's performance
	 */
	@Bean
	public MetricRegistry metricRegistry() {
		MetricRegistry metricRegistry = new MetricRegistry();
		metricRegistry.registerAll(new MemoryUsageGaugeSet());
		metricRegistry.registerAll(new GarbageCollectorMetricSet());

		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();

		return metricRegistry;
	}

	/**
	 * @return the {@link IPagingProvider} for the application, which ensures
	 *         that results can be paged
	 */
	@Bean
	public IPagingProvider pagingProvider() {
		// As of 2017-04-12, this number was a COMPLETE guess.
		// TODO measure average search size in memory
		int averageSearchMemoryBytes = (int) Math.pow(1000, 2); // 1 MB

		/*
		 * The maximum number of searches that will be kept in memory. As the
		 * number of searches exceeds this count, the oldest searches will be
		 * evicted from memory, and will no longer be able to be paged. What
		 * we're going for here is "half the memory not used by the app itself
		 * (but never less than 20 searches), which leaves the other half for
		 * normal request handling."
		 */
		long reservedMemoryBytes = (long) (4 * Math.pow(1000, 3)); // 4 GB
		long availableMemoryBytes = Runtime.getRuntime().maxMemory() - reservedMemoryBytes;
		long searchMemoryBytesGoal = availableMemoryBytes / 2;
		int maximumLiveSearches = (int) Math.max(searchMemoryBytesGoal / averageSearchMemoryBytes, 20);

		FifoMemoryPagingProvider pagingProvider = new FifoMemoryPagingProvider(maximumLiveSearches);

		/*
		 * The "page size" is the maximum number of resources that will be
		 * included in a single response page. For example, if a search, returns
		 * 2000 EOBs and the page size is 1000, the result will be spread across
		 * 2 separate pages. In our case, we'd far rather reduce the number of
		 * requests than the response size, so we're setting this way higher
		 * than the default page size of 10.
		 */
		pagingProvider.setDefaultPageSize(1000);
		pagingProvider.setMaximumPageSize(1000);
	}
}

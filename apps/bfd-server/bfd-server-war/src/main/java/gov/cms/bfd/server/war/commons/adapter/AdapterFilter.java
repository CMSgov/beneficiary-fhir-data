package gov.cms.bfd.server.war.commons.adapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AdapterFilter implements InvocationHandler {

  private Set<String> filters;
  private Object target;

  private AdapterFilter(Object claim, String... filters) {
    this.target = claim;
    this.filters = new HashSet<>(Arrays.asList(filters));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // Filter objects
    if (filters.contains(method.getName())) {
      return Optional.empty();
    } else if ("getLines".equals(method.getName())) {
      return ((List<ClaimLineAdaptor>) method.invoke(target, args))
          .stream()
              .map(line -> AdapterFilter.create(line, filters.toArray(new String[] {})))
              .collect(Collectors.toList());
    } else {
      return method.invoke(target, args);
    }
  }

  public static ClaimAdaptorInterface create(ClaimAdaptor claim, String... filters) {
    return (ClaimAdaptorInterface)
        Proxy.newProxyInstance(
            AdapterFilter.class.getClassLoader(),
            new Class[] {ClaimAdaptorInterface.class},
            new AdapterFilter(claim, filters));
  }

  private static ClaimLineAdaptorInterface create(ClaimLineAdaptor line, String... filters) {
    return (ClaimLineAdaptorInterface)
        Proxy.newProxyInstance(
            AdapterFilter.class.getClassLoader(),
            new Class[] {ClaimLineAdaptorInterface.class},
            new AdapterFilter(line, filters));
  }
}

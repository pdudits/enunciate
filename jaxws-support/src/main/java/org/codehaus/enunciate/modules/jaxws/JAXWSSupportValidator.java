/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.modules.jaxws;

import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import org.codehaus.enunciate.contract.jaxws.*;
import org.codehaus.enunciate.contract.validation.BaseValidator;
import org.codehaus.enunciate.contract.validation.ValidationResult;
import org.codehaus.enunciate.util.TypeDeclarationComparator;
import org.codehaus.enunciate.util.MapType;
import net.sf.jelly.apt.Context;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validator for the xml module.
 *
 * @author Ryan Heaton
 */
public class JAXWSSupportValidator extends BaseValidator {
  
  private final HashSet<String> jaxwsBeans = new HashSet<String>();
  private final TreeSet<WebFault> faultSet = new TreeSet<WebFault>(new TypeDeclarationComparator());

  @Override
  public ValidationResult validateEndpointInterface(EndpointInterface ei) {
    ValidationResult result = super.validateEndpointInterface(ei);
    TreeSet<WebFault> unvisitedFaults = new TreeSet<WebFault>(new TypeDeclarationComparator());
    for (WebMethod webMethod : ei.getWebMethods()) {
      for (WebMessage webMessage : webMethod.getMessages()) {
        if (webMessage instanceof RequestWrapper) {
          result.aggregate(validateRequestWrapper((RequestWrapper) webMessage, jaxwsBeans));
        }
        else if (webMessage instanceof ResponseWrapper) {
          result.aggregate(validateResponseWrapper((ResponseWrapper) webMessage, jaxwsBeans));
        }
        else if (webMessage instanceof WebFault) {
          WebFault webFault = (WebFault) webMessage;
          if (faultSet.add(webFault)) {
            unvisitedFaults.add(webFault);
          }
        }
      }

      for (WebParam webParam : webMethod.getWebParameters()) {
        if (webParam.getType() instanceof MapType) {
          result.addError(webParam, "There's a bug in JAXB ruining support for maps in return values or in parameters.  For more information, see " +
            "https://jaxb.dev.java.net/issues/show_bug.cgi?id=268 and http://forums.java.net/jive/thread.jspa?messageID=361990");
        }
      }

      if (webMethod.getWebResult().getType() instanceof MapType) {
        result.addError(webMethod, "There's a bug in JAXB ruining support for maps in return values or in parameters.  For more information, see " +
          "https://jaxb.dev.java.net/issues/show_bug.cgi?id=268 and http://forums.java.net/jive/thread.jspa?messageID=361990");
      }
    }

    for (WebFault webFault : unvisitedFaults) {
      result.aggregate(validateWebFault(webFault, jaxwsBeans));
    }

    return result;
  }

  /**
   * Validates a request wrapper.
   *
   * @param wrapper        The wrapper.
   * @param alreadyVisited The list of bean names already visited.
   * @return The validation result.
   */
  public ValidationResult validateRequestWrapper(RequestWrapper wrapper, Set<String> alreadyVisited) {
    AnnotationProcessorEnvironment ape = Context.getCurrentEnvironment();
    ValidationResult result = new ValidationResult();

    String requestBeanName = wrapper.getRequestBeanName();
    if (!alreadyVisited.add(requestBeanName)) {
      result.addError(wrapper.getWebMethod(), requestBeanName + " conflicts with another generated bean name.  Please use the @RequestWrapper " +
        "annotation to customize the bean name.");
    }

    return result;
  }

  /**
   * Validates a response wrapper.
   *
   * @param wrapper        The wrapper.
   * @param alreadyVisited The list of bean names already visited.
   * @return The validation result.
   */
  public ValidationResult validateResponseWrapper(ResponseWrapper wrapper, Set<String> alreadyVisited) {
    AnnotationProcessorEnvironment ape = Context.getCurrentEnvironment();
    ValidationResult result = new ValidationResult();

    String responseBeanName = wrapper.getResponseBeanName();
    if (!alreadyVisited.add(responseBeanName)) {
      result.addError(wrapper.getWebMethod(), responseBeanName + " conflicts with another generated bean name.  Please use the @ResponseWrapper " +
        "annotation to customize the bean name.");
    }

    return result;
  }

  /**
   * Validates a web fault.
   *
   * @param webFault       The web fault to validate.
   * @param alreadyVisited The bean names that have alrady been visited.
   * @return The validation result.
   */
  public ValidationResult validateWebFault(WebFault webFault, Set<String> alreadyVisited) {
    AnnotationProcessorEnvironment ape = Context.getCurrentEnvironment();
    ValidationResult result = new ValidationResult();

    if (webFault.isImplicitSchemaElement()) {
      String faultBeanFQN = webFault.getImplicitFaultBeanQualifiedName();
      if (!alreadyVisited.add(faultBeanFQN)) {
        result.addError(webFault, faultBeanFQN + " conflicts with another generated bean name.  Please use the @WebFault annotation " +
          "to customize the fault bean name.");
      }
    }

    return result;
  }

}

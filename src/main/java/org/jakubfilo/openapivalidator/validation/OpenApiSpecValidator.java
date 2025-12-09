package org.jakubfilo.openapivalidator.validation;

import static org.jakubfilo.openapivalidator.validation.rules.RulesFacade.validateEnumsAsUpperSnakeCase;
import static org.jakubfilo.openapivalidator.validation.rules.RulesFacade.validateLowerCamelCaseSchemaProperties;
import static org.jakubfilo.openapivalidator.validation.rules.RulesFacade.validateNoGenericIdParameterNames;
import static org.jakubfilo.openapivalidator.validation.rules.RulesFacade.validateOperationDescriptions;
import static org.jakubfilo.openapivalidator.validation.rules.RulesFacade.validatePostEndpointsUseCreated;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiSpecValidator {

	public List<ValidationError> validate(OpenAPI openAPI) {
		List<ValidationError> errors = new ArrayList<>();
		validateOperationDescriptions(openAPI, errors);
		validateLowerCamelCaseSchemaProperties(openAPI, errors);
		validatePostEndpointsUseCreated(openAPI, errors);
		validateNoGenericIdParameterNames(openAPI, errors);
		validateEnumsAsUpperSnakeCase(openAPI, errors);
		return errors;
	}
}

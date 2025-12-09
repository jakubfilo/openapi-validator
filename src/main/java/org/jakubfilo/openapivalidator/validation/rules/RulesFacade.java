package org.jakubfilo.openapivalidator.validation.rules;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jakubfilo.openapivalidator.validation.ValidationError;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

public class RulesFacade {

	private static final Pattern LOWER_CAMEL = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
	private static final Pattern UPPER_SNAKE = Pattern.compile("^[A-Z0-9]+(?:_[A-Z0-9]+)*$");


	/**
	 * Rule: every operation (each HTTP verb under each path) must have a non-blank description.
	 */
	public static void validateOperationDescriptions(OpenAPI openAPI, List<ValidationError> errors) {
		if (openAPI == null || openAPI.getPaths() == null) {
			return;
		}

		openAPI.getPaths().forEach((path, pathItem) -> {
			if (pathItem == null) {
				return;
			}
			Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
			if (operations == null) {
				return;
			}

			operations.forEach((method, operation) -> {
				if (operation == null) {
					return;
				}
				String description = operation.getDescription();
				if (description == null || description.isBlank()) {
					String location = method + " " + path;
					errors.add(new ValidationError(
							"MISSING_OPERATION_DESCRIPTION",
							location,
							"Operation must have a non-blank description"
					));
				}
			});
		});
	}

	/**
	 * Rule: all schema property names must be lowerCamelCase.
	 * This example checks only component schemas (openAPI.components.schemas).
	 */
	public static void validateLowerCamelCaseSchemaProperties(OpenAPI openAPI, List<ValidationError> errors) {
		if (openAPI == null ||
				openAPI.getComponents() == null ||
				openAPI.getComponents().getSchemas() == null) {
			return;
		}

		Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
		for (Map.Entry<String, Schema> schemaEntry : schemas.entrySet()) {
			String schemaName = schemaEntry.getKey();
			Schema<?> schema = schemaEntry.getValue();
			if (schema == null || schema.getProperties() == null) {
				continue;
			}

			Map<String, Schema> properties = schema.getProperties();
			for (String propName : properties.keySet()) {
				if (propName == null) {
					continue;
				}
				if (!LOWER_CAMEL.matcher(propName).matches()) {
					String location = "schema " + schemaName + ".properties." + propName;
					errors.add(new ValidationError(
							"INVALID_PROPERTY_NAME_CASE",
							location,
							"Property name must be lowerCamelCase"
					));
				}
			}
		}
	}

	public static void validatePostEndpointsUseCreated(OpenAPI openAPI, List<ValidationError> errors) {
		if (openAPI == null || openAPI.getPaths() == null) {
			return;
		}

		openAPI.getPaths().forEach((path, pathItem) -> {
			if (pathItem == null) return;

			Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
			if (operations == null) return;

			operations.forEach((method, operation) -> {
				if (method == PathItem.HttpMethod.POST) {
					if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
						errors.add(new ValidationError(
								"POST_MISSING_RESPONSES",
								"POST " + path,
								"POST operation must define a 201 Created response"
						));
						return;
					}

					boolean has201 = operation.getResponses().containsKey("201");

					// Violation: POST uses 200 instead of 201
					if (!has201) {
						errors.add(new ValidationError(
								"POST_SHOULD_RETURN_201",
								"POST " + path,
								"POST endpoints must return 201 Created instead of 200 OK"
						));
					}
				}
			});
		});
	}

	/**
	 * Rule:
	 *  - Any path or query parameter named exactly "id" (case-insensitive) is invalid.
	 *  - Callers should use more specific names, e.g. userId, orderId, personId, resourceId, etc.
	 */
	public static void validateNoGenericIdParameterNames(OpenAPI openAPI, List<ValidationError> errors) {
		if (openAPI == null || openAPI.getPaths() == null) {
			return;
		}

		openAPI.getPaths().forEach((path, pathItem) -> {
			if (pathItem == null) {
				return;
			}

			// Path-level parameters
			validateParameters(pathItem.getParameters(), path, null, errors);

			// Operation-level parameters
			Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
			if (operations == null) {
				return;
			}

			operations.forEach((method, operation) -> {
				if (operation == null) {
					return;
				}
				validateParameters(operation.getParameters(), path, method, errors);
			});
		});
	}

	private static void validateParameters(List<io.swagger.v3.oas.models.parameters.Parameter> params,
			String path,
			PathItem.HttpMethod method,
			List<ValidationError> errors) {
		if (params == null || params.isEmpty()) {
			return;
		}

		for (Parameter p : params) {
			if (p == null) {
				continue;
			}
			String in = p.getIn();
			String name = p.getName();
			if (in == null || name == null) {
				continue;
			}

			// We only care about path/query parameters
			if (!"path".equals(in) && !"query".equals(in)) {
				continue;
			}

			if ("id".equalsIgnoreCase(name.trim())) {
				String opPrefix = (method == null) ? "" : (method + " ");
				String location = opPrefix + path + " param '" + name + "' in " + in;
				errors.add(new ValidationError(
						"GENERIC_ID_PARAMETER_NAME",
						location,
						"Path and query parameters representing identifiers must be specific, "
								+ "e.g. userId or resourceId instead of just 'id'"
				));
			}
		}
	}

	/**
	 * Validates that all enum values across component schemas follow UPPER_SNAKE_CASE.
	 */
	public static void validateEnumsAsUpperSnakeCase(OpenAPI openAPI, List<ValidationError> errors) {
		if (openAPI == null ||
				openAPI.getComponents() == null ||
				openAPI.getComponents().getSchemas() == null) {
			return;
		}

		openAPI.getComponents().getSchemas().forEach((schemaName, schema) -> {
			if (schema == null) return;

			// If the schema itself has enums
			validateEnumList(schema.getEnum(), schemaName, null, errors);

			// If the schema is an object → check its properties for enums
			if (schema.getProperties() != null) {
				((Map<String, Schema<?>>) schema.getProperties()).forEach((propName, propSchema) -> {
					if (propSchema != null) {
						validateEnumList(propSchema.getEnum(), schemaName, propName, errors);
					}
				});
			}
		});
	}

	private static void validateEnumList(List<?> enumValues,
			String schemaName,
			String propertyName,
			List<ValidationError> errors) {
		if (enumValues == null || enumValues.isEmpty()) return;

		for (Object value : enumValues) {
			if (!(value instanceof String)) continue;

			String enumVal = (String) value;

			if (!UPPER_SNAKE.matcher(enumVal).matches()) {
				String location = (propertyName == null)
						? "schema " + schemaName
						: "schema " + schemaName + ".properties." + propertyName;

				errors.add(new ValidationError(
						"ENUM_NOT_UPPER_SNAKE_CASE",
						location,
						"Enum value '" + enumVal + "' must use UPPER_SNAKE_CASE"
				));
			}
		}
	}
}

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jakubfilo.openapivalidator.OpenApiParserUtil;
import org.jakubfilo.openapivalidator.validation.OpenApiSpecValidator;
import org.jakubfilo.openapivalidator.validation.ValidationError;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiSpecValidatorTest {

	private final OpenApiSpecValidator validator = new OpenApiSpecValidator();

	@Test
	void everyOperationMustHaveDescription() {
		// Note: GET /users has no "description" -> should produce an error
		String specWithoutDescriptions = """
				{
				  "openapi": "3.0.0",
				  "info": { "title": "Demo API", "version": "1.0.0" },
				  "paths": {
				    "/users": {
				      "get": {
				        "responses": {
				          "200": {
				            "description": "OK"
				          }
				        }
				      }
				    }
				  }
				}
				""";

		OpenAPI openAPI = OpenApiParserUtil.parseJson(specWithoutDescriptions);
		List<ValidationError> errors = validator.validate(openAPI);

		assertFalse(errors.isEmpty(), "Expected validation errors");

		boolean hasMissingDescriptionError = errors.stream()
				.anyMatch(e -> "MISSING_OPERATION_DESCRIPTION".equals(e.getCode())
						&& "GET /users".equals(e.getLocation()));

		assertTrue(hasMissingDescriptionError,
				"Expected MISSING_OPERATION_DESCRIPTION for GET /users, but got: " + errors);
	}

	@Test
	void schemaPropertyNamesMustBeLowerCamelCase() {
		// Note: "First_name" is not lowerCamelCase -> should produce an error
		String specWithBadPropertyName = """
				{
				  "openapi": "3.0.0",
				  "info": { "title": "Demo API", "version": "1.0.0" },
				  "paths": {
				    "/users": {
				      "get": {
				        "description": "Get users",
				        "responses": {
				          "200": {
				            "description": "OK"
				          }
				        }
				      }
				    }
				  },
				  "components": {
				    "schemas": {
				      "User": {
				        "type": "object",
				        "properties": {
				          "firstName": { "type": "string" },
				          "First_name": { "type": "string" }
				        }
				      }
				    }
				  }
				}
				""";

		OpenAPI openAPI = OpenApiParserUtil.parseJson(specWithBadPropertyName);
		List<ValidationError> errors = validator.validate(openAPI);

		boolean hasCaseError = errors.stream()
				.anyMatch(e -> "INVALID_PROPERTY_NAME_CASE".equals(e.getCode())
						&& e.getLocation().equals("schema User.properties.First_name"));

		assertTrue(hasCaseError,
				"Expected INVALID_PROPERTY_NAME_CASE for User.properties.First_name, but got: " + errors);
	}

	@Test
	void onlyOperationsWithoutDescriptionShouldFail() {
		String spec = """
        {
          "openapi": "3.0.0",
          "info": { "title": "Demo API", "version": "1.0.0" },
          "paths": {
            "/users": {
              "get": {
                "description": "Returns all users",
                "responses": {
                  "200": { "description": "OK" }
                }
              },
              "post": {
                "responses": {
                  "201": { "description": "Created" }
                }
              }
            }
          }
        }
        """;

		OpenAPI openAPI = OpenApiParserUtil.parseJson(spec);
		List<ValidationError> errors = validator.validate(openAPI);

		// --- ensure exactly ONE violation exists ---
		assertEquals(1, errors.size(), "Only one endpoint should fail validation");

		ValidationError error = errors.getFirst();

		// --- ensure the correct error is reported ---
		assertEquals("MISSING_OPERATION_DESCRIPTION", error.getCode());
		assertEquals("POST /users", error.getLocation());

		// --- ensure GET /users (which has a description) is NOT reported ---
		boolean getReported = errors.stream()
				.anyMatch(e -> "GET /users".equals(e.getLocation()));
		assertFalse(getReported, "GET /users should not be reported because it has a description");
	}

	@Test
	void postEndpointsMustUse201Created() {
		String spec = """
        {
          "openapi": "3.0.0",
          "info": { "title": "Test API", "version": "1.0.0" },
          "paths": {
            "/users": {
              "post": {
                "description": "Bad POST – returns 200 instead of 201",
                "responses": {
                  "200": { "description": "OK" }
                }
              }
            },
            "/groups": {
              "post": {
                "description": "Proper POST – returns 201",
                "responses": {
                  "201": { "description": "Created" }
                }
              }
            }
          }
        }
        """;

		OpenAPI openAPI = OpenApiParserUtil.parseJson(spec);
		List<ValidationError> errors = validator.validate(openAPI);

		// Check that only POST /users fails
		assertEquals(1, errors.size(), "Only one POST endpoint should fail validation");

		ValidationError error = errors.getFirst();

		assertEquals("POST_SHOULD_RETURN_201", error.getCode());
		assertEquals("POST /users", error.getLocation());

		// ensure POST /groups is NOT reported
		boolean groupsReported = errors.stream()
				.anyMatch(e -> "POST /groups".equals(e.getLocation()));

		assertFalse(groupsReported, "POST /groups should not fail because it uses 201 Created");
	}

	@Test
	void idParametersMustBeMoreSpecificThanJustId() {
		String spec = """
        {
          "openapi": "3.0.0",
          "info": { "title": "ID Param Test API", "version": "1.0.0" },
          "paths": {
            "/users/{id}": {
              "parameters": [
                {
                  "name": "id",
                  "in": "path",
                  "required": true,
                  "schema": { "type": "string" }
                }
              ],
              "get": {
                "description": "Get user by generic id (bad)",
                "parameters": [
                  {
                    "name": "id",
                    "in": "query",
                    "required": false,
                    "schema": { "type": "string" }
                  }
                ],
                "responses": {
                  "200": { "description": "OK" }
                }
              }
            },
            "/people/{personId}": {
              "parameters": [
                {
                  "name": "personId",
                  "in": "path",
                  "required": true,
                  "schema": { "type": "string" }
                }
              ],
              "get": {
                "description": "Get person by personId (good)",
                "parameters": [
                  {
                    "name": "departmentId",
                    "in": "query",
                    "required": false,
                    "schema": { "type": "string" }
                  }
                ],
                "responses": {
                  "200": { "description": "OK" }
                }
              }
            }
          }
        }
        """;

		OpenAPI openAPI = OpenApiParserUtil.parseJson(spec);
		List<ValidationError> errors = validator.validate(openAPI);

		// Expect exactly two errors:
		//  - path param {id} on /users/{id}
		//  - query param 'id' on GET /users/{id}
		assertEquals(2, errors.size(), "Expected two invalid 'id' parameters");

		boolean hasPathIdError = errors.stream().anyMatch(e ->
				"GENERIC_ID_PARAMETER_NAME".equals(e.getCode()) &&
						e.getLocation().contains("/users/{id}") &&
						e.getLocation().contains("param 'id' in path")
		);

		boolean hasQueryIdError = errors.stream().anyMatch(e ->
				"GENERIC_ID_PARAMETER_NAME".equals(e.getCode()) &&
						e.getLocation().contains("GET /users/{id}") &&
						e.getLocation().contains("param 'id' in query")
		);

		assertTrue(hasPathIdError, "Expected generic path param 'id' on /users/{id} to be invalid");
		assertTrue(hasQueryIdError, "Expected generic query param 'id' on GET /users/{id} to be invalid");

		// Ensure nothing under /people/{personId} is flagged
		boolean mentionsPersonIdPath = errors.stream()
				.anyMatch(e -> e.getLocation().contains("/people/{personId}"));
		assertFalse(mentionsPersonIdPath,
				"Parameters 'personId' and 'departmentId' should be valid and not reported");
	}

	@Test
	void enumValuesMustBeUpperSnakeCase() {
		String spec = """
        {
          "openapi": "3.0.0",
          "info": { "title": "Enum API", "version": "1.0.0" },
          "paths": {},
          "components": {
            "schemas": {
              "Color": {
                "type": "string",
                "enum": ["red", "Blue", "DARK_GREEN", "dark_green"]
              },
              "Status": {
                "type": "string",
                "enum": ["ACTIVE", "IN_PROGRESS"]
              },
              "User": {
                "type": "object",
                "properties": {
                  "role": {
                    "type": "string",
                    "enum": ["admin", "SUPER_USER"]
                  }
                }
              }
            }
          }
        }
        """;

		OpenAPI openAPI = OpenApiParserUtil.parseJson(spec);
		List<ValidationError> errors = validator.validate(openAPI);

		// Collect violations for easier assertions
		List<String> reportedMessages = errors.stream()
				.filter(e -> e.getCode().equals("ENUM_NOT_UPPER_SNAKE_CASE"))
				.map(ValidationError::getMessage)
				.toList();

		// Expected invalid enum values:
		assertTrue(reportedMessages.stream().anyMatch(m -> m.contains("'red'")));
		assertTrue(reportedMessages.stream().anyMatch(m -> m.contains("'Blue'")));
		assertTrue(reportedMessages.stream().anyMatch(m -> m.contains("'dark_green'")));
		assertTrue(reportedMessages.stream().anyMatch(m -> m.contains("'admin'")));

		// Ensure VALID UPPER_SNAKE_CASE enums are NOT flagged
		assertFalse(reportedMessages.stream().anyMatch(m -> m.contains("DARK_GREEN")),
				"DARK_GREEN is already valid UPPER_SNAKE_CASE");
		assertFalse(reportedMessages.stream().anyMatch(m -> m.contains("SUPER_USER")));
		assertFalse(reportedMessages.stream().anyMatch(m -> m.contains("ACTIVE")));
		assertFalse(reportedMessages.stream().anyMatch(m -> m.contains("IN_PROGRESS")));

		// Ensure number of violations matches expected invalid values
		assertEquals(4, reportedMessages.size(),
				"Expected 4 invalid enum values (red, Blue, dark_green, admin)");
	}
}

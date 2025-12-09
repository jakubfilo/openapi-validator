package org.jakubfilo.openapivalidator;

import java.util.List;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

public final class OpenApiParserUtil {

	private OpenApiParserUtil() {
	}

	/**
	 * Parse an OpenAPI 3.0.x JSON string into an OpenAPI model object.
	 * All fields are nullable; no additional validation is done here.
	 */
	public static OpenAPI parseJson(String json) {
		SwaggerParseResult result =
				new OpenAPIParser().readContents(json, null, null);

		List<String> messages = result.getMessages();
		if (messages != null && !messages.isEmpty()) {
			throw new IllegalArgumentException(
					"OpenAPI parsing errors: " + String.join("; ", messages));
		}

		OpenAPI openAPI = result.getOpenAPI();
		if (openAPI == null) {
			throw new IllegalArgumentException("Parsed OpenAPI is null");
		}
		return openAPI;
	}
}
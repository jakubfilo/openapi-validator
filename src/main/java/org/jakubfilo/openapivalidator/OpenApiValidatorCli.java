package org.jakubfilo.openapivalidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jakubfilo.openapivalidator.validation.OpenApiSpecValidator;
import org.jakubfilo.openapivalidator.validation.ValidationError;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiValidatorCli {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: java ... OpenApiValidatorCli <openapi-file>");
			System.exit(1);
		}

		String json = Files.readString(Path.of(args[0]));
		OpenAPI openAPI = OpenApiParserUtil.parseJson(json);

		OpenApiSpecValidator validator = new OpenApiSpecValidator();
		List<ValidationError> errors = validator.validate(openAPI);

		if (!errors.isEmpty()) {
			System.err.println("OpenAPI validation failed:");
			errors.forEach(e -> System.err.println("  - " + e));
			System.exit(1);
		}

		System.out.println("OpenAPI validation passed.");
	}
}

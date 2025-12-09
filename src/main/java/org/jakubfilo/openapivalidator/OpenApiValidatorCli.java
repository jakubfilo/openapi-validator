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
			errors.forEach(e -> {
				String msg = e.getCode() + " - " + e.getLocation() + " - " + e.getMessage();
				System.out.println("::error title=OpenAPI validation::" + escapeGithubMessage(msg));
			});
			System.exit(1);
		}

		System.out.println("OpenAPI validation passed.");
	}

	private static String escapeGithubMessage(String msg) {
		// Minimal escaping for ::error command
		// https://docs.github.com/en/actions/using-workflows/workflow-commands-for-github-actions
		return msg
				.replace("%", "%25")
				.replace("\r", "%0D")
				.replace("\n", "%0A");
	}
}

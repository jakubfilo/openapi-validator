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
			writeGithubSummary(errors, args[0]);
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

	private static void writeGithubSummary(List<ValidationError> errors, String specPath) {
		String summaryPath = System.getenv("GITHUB_STEP_SUMMARY");
		if (summaryPath == null || summaryPath.isBlank()) {
			return; // Not running inside GitHub Actions
		}

		StringBuilder md = new StringBuilder();
		md.append("## OpenAPI validation failed\n\n");
		md.append("Spec file: `").append(specPath).append("`\n\n");
		md.append("| Code | Location | Message |\n");
		md.append("|------|----------|---------|\n");

		for (ValidationError e : errors) {
			md.append("| ")
					.append(escapePipe(e.getCode())).append(" | ")
					.append(escapePipe(e.getLocation())).append(" | ")
					.append(escapePipe(e.getMessage())).append(" |\n");
		}
		md.append("\n");

		try (var out = java.nio.file.Files.newBufferedWriter(
				java.nio.file.Path.of(summaryPath),
				java.nio.file.StandardOpenOption.CREATE,
				java.nio.file.StandardOpenOption.APPEND
		)) {
			out.write(md.toString());
		} catch (Exception ex) {
			// Swallow or log to stderr, but don't override exit code
			System.err.println("Failed to write GitHub summary: " + ex.getMessage());
		}
	}

	private static String escapePipe(String s) {
		return s.replace("|", "\\|");
	}
}

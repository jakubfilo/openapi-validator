import org.jakubfilo.openapivalidator.OpenApiParserUtil;
import org.jakubfilo.openapivalidator.validation.OpenApiSpecValidator;
import org.jakubfilo.openapivalidator.validation.ValidationError;

import io.swagger.v3.oas.models.OpenAPI;

void main(String[] args) throws Exception {
	if (args.length != 1) {
		System.err.println("Usage: openapi-validator <openapi-json-file>");
		System.exit(1);
	}

	String filePath = args[0];
	String json = Files.readString(Path.of(filePath));

	OpenAPI openAPI = OpenApiParserUtil.parseJson(json);
	OpenApiSpecValidator validator = new OpenApiSpecValidator();

	List<ValidationError> errors = validator.validate(openAPI);

	if (!errors.isEmpty()) {
		System.err.println("OpenAPI validation failed:");
		errors.forEach(e -> System.err.println("  - " + e));
		System.exit(1);
	}

	IO.println("OpenAPI validation passed.");
}

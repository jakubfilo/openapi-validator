package org.jakubfilo.openapivalidator.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationError {

	private final String code;
	private final String location; // Human-readable location, e.g. "GET /users" or "schema User.properties.lastName"
	private final String message;

	@Override
	public String toString() {
		return code + " at " + location + ": " + message;
	}
}

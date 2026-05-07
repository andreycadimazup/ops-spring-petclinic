/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner.rest.exceptions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.samples.petclinic.owner.rest.controllers.OwnerRestController;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackageClasses = OwnerRestController.class)
class OwnerRestExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		exception.getBindingResult()
			.getFieldErrors()
			.forEach((fieldError) -> errors
				.computeIfAbsent(fieldError.getField(), (field) -> new java.util.ArrayList<>())
				.add(fieldError.getDefaultMessage()));
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed", errors);
	}

	@ExceptionHandler(ApiValidationException.class)
	ProblemDetail handleApiValidation(ApiValidationException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", exception.getMessage(), exception.getErrors());
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			HandlerMethodValidationException.class })
	ProblemDetail handleBadRequest(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "Bad request", "Request could not be parsed or validated");
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
	}

	@ExceptionHandler(BusinessConflictException.class)
	ProblemDetail handleBusinessConflict(BusinessConflictException exception) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
	}

	@ExceptionHandler({ HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
	ProblemDetail handleHttpRequestException(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleException(Exception exception) {
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected internal error");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail, Map<String, List<String>> errors) {
		ProblemDetail problem = problem(status, title, detail);
		problem.setProperty("errors", errors);
		return problem;
	}

}

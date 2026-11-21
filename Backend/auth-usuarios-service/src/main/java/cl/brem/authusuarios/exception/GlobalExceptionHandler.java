package cl.brem.authusuarios.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> manejarResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> manejarConflict(ConflictException ex, HttpServletRequest request) {
		return construir(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> manejarIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String detalle = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return construir(HttpStatus.BAD_REQUEST, detalle, request);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> manejarBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> manejarAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return construir(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> manejarGenerico(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
		return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
	}

	private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, HttpServletRequest request) {
		ErrorResponse cuerpo = ErrorResponse.builder()
				.status(status.value())
				.error(status.getReasonPhrase())
				.mensaje(mensaje)
				.timestamp(LocalDateTime.now())
				.build();
		return ResponseEntity.status(status).body(cuerpo);
	}

}

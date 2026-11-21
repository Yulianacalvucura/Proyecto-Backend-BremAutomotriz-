package cl.brem.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String SERVICIO_NO_DISPONIBLE = "Servicio no disponible, intenta más tarde";

	private static final String ERROR_INTERNO = "Error interno del servidor";

	@ExceptionHandler(RestClientException.class)
	public ResponseEntity<ErrorResponse> manejarServicioNoDisponible(RestClientException ex) {
		HttpStatus estado = esTimeout(ex) ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
		log.warn("Fallo de comunicación con un servicio de destino: {}", ex.getClass().getSimpleName());
		return construir(estado, SERVICIO_NO_DISPONIBLE);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> manejarGenerico(Exception ex) {
		log.error("Error interno no controlado en el gateway", ex);
		return construir(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_INTERNO);
	}

	private boolean esTimeout(Throwable ex) {
		Throwable actual = ex;
		while (actual != null) {
			if (actual instanceof SocketTimeoutException
					|| actual instanceof HttpTimeoutException
					|| actual instanceof TimeoutException) {
				return true;
			}
			actual = actual.getCause();
		}
		return false;
	}

	private ResponseEntity<ErrorResponse> construir(HttpStatus estado, String mensaje) {
		ErrorResponse cuerpo = ErrorResponse.builder()
				.status(estado.value())
				.error(estado.getReasonPhrase())
				.mensaje(mensaje)
				.build();
		return ResponseEntity.status(estado).body(cuerpo);
	}

}

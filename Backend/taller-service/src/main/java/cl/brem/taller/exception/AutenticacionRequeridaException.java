package cl.brem.taller.exception;

/**
 * Se lanza cuando falta el header X-User-Id al crear una orden de trabajo.
 * Se mapea a HTTP 401 en GlobalExceptionHandler.
 */
public class AutenticacionRequeridaException extends RuntimeException {

	public AutenticacionRequeridaException(String mensaje) {
		super(mensaje);
	}

}

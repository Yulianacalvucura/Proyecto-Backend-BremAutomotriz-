package cl.brem.taller.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String mensaje) {
		super(mensaje);
	}

}

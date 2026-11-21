package cl.brem.apigateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Elimina siempre los headers X-User-* que pueda enviar el cliente y expone
 * únicamente los valores verificados por el gateway a partir del JWT.
 */
public class HttpServletRequestWrapperWithHeaders extends HttpServletRequestWrapper {

	private static final Set<String> HEADERS_ANTI_FALSIFICACION = Set.of(
			"x-user-username", "x-user-roles", "x-user-id", "x-user-nombre");

	private final Map<String, String[]> cabeceras = new LinkedHashMap<>();

	public HttpServletRequestWrapperWithHeaders(HttpServletRequest request) {
		super(request);
		Enumeration<String> nombres = request.getHeaderNames();
		while (nombres != null && nombres.hasMoreElements()) {
			String nombre = nombres.nextElement();
			if (!HEADERS_ANTI_FALSIFICACION.contains(nombre.toLowerCase(Locale.ROOT))) {
				cabeceras.put(nombre, Collections.list(request.getHeaders(nombre)).toArray(new String[0]));
			}
		}
	}

	public void agregarCabeceraVerificada(String nombre, String valor) {
		if (valor == null) {
			return;
		}
		String clave = buscarClave(nombre);
		if (clave != null) {
			cabeceras.remove(clave);
		}
		cabeceras.put(nombre, new String[] { valor });
	}

	@Override
	public String getHeader(String nombre) {
		String[] valores = obtenerValores(nombre);
		return valores.length == 0 ? null : valores[0];
	}

	@Override
	public Enumeration<String> getHeaders(String nombre) {
		return Collections.enumeration(List.of(obtenerValores(nombre)));
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(cabeceras.keySet());
	}

	private String[] obtenerValores(String nombre) {
		String clave = buscarClave(nombre);
		return clave == null ? new String[0] : cabeceras.get(clave);
	}

	private String buscarClave(String nombre) {
		if (nombre == null) {
			return null;
		}
		return cabeceras.keySet().stream()
				.filter(clave -> clave.equalsIgnoreCase(nombre))
				.findFirst()
				.orElse(null);
	}

}

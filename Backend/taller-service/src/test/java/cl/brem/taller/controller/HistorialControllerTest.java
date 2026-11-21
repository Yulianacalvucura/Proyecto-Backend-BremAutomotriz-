package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.HistorialResponse;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.service.OrdenTrabajoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HistorialController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class HistorialControllerTest {

	private static final String USUARIO = "jefe@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrdenTrabajoService service;

	@Test
	void listarHistorialDevuelve200() throws Exception {
		when(service.listarHistorial(1L)).thenReturn(List.of(
				HistorialResponse.builder().id(1L).otId(1L)
						.estadoAnterior(null).estadoNuevo("PENDIENTE").build(),
				HistorialResponse.builder().id(2L).otId(1L)
						.estadoAnterior("PENDIENTE").estadoNuevo("EN_PROCESO")
						.observacion("Iniciado").build()));

		mockMvc.perform(get("/ordenes/1/historial")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].estadoAnterior").value("PENDIENTE"))
				.andExpect(jsonPath("$[1].estadoNuevo").value("EN_PROCESO"));

		verify(service).listarHistorial(1L);
	}

	@Test
	void listarHistorialDeOrdenInexistenteDevuelve404() throws Exception {
		when(service.listarHistorial(99L))
				.thenThrow(new ResourceNotFoundException("Orden de trabajo no encontrada con id: 99"));

		mockMvc.perform(get("/ordenes/99/historial")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje", containsString("Orden de trabajo no encontrada")));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/ordenes/1/historial"))
				.andExpect(status().isUnauthorized());

		verify(service, never()).listarHistorial(any(Long.class));
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/ordenes/1/historial")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listarHistorial(any(Long.class));
	}

}

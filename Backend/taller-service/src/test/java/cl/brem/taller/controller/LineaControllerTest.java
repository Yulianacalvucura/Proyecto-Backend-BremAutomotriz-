package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.LineaRequest;
import cl.brem.taller.dto.LineaResponse;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.TipoLinea;
import cl.brem.taller.service.OrdenTrabajoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LineaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LineaControllerTest {

	private static final String USUARIO = "jefe@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrdenTrabajoService service;

	@Test
	void listarLineasDevuelve200() throws Exception {
		when(service.listarLineas(1L)).thenReturn(List.of(LineaResponse.builder()
				.id(1L).otId(1L).tipo("OM").cobrado(30000).build()));

		mockMvc.perform(get("/ordenes/1/lineas")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cobrado").value(30000));

		verify(service).listarLineas(1L);
	}

	@Test
	void listarLineasDeOrdenInexistenteDevuelve404() throws Exception {
		when(service.listarLineas(99L)).thenThrow(new ResourceNotFoundException("Orden de trabajo no encontrada con id: 99"));

		mockMvc.perform(get("/ordenes/99/lineas")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje", containsString("Orden de trabajo no encontrada")));
	}

	@Test
	void agregarLineaDevuelve201() throws Exception {
		when(service.agregarLinea(any(Long.class), any(LineaRequest.class)))
				.thenReturn(LineaResponse.builder().id(2L).otId(1L).cobrado(18200).build());

		mockMvc.perform(post("/ordenes/1/lineas")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cobrado").value(18200));
	}

	@Test
	void agregarLineaConFactorMenorAUnoDevuelve400() throws Exception {
		LineaRequest invalida = request();
		invalida.setFactor(new BigDecimal("0.90"));

		mockMvc.perform(post("/ordenes/1/lineas")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalida)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("factor")));

		verify(service, never()).agregarLinea(any(Long.class), any(LineaRequest.class));
	}

	@Test
	void actualizarLineaDevuelve200() throws Exception {
		when(service.actualizarLinea(any(Long.class), any(Long.class), any(LineaRequest.class)))
				.thenReturn(LineaResponse.builder().id(2L).otId(1L).descripcion("Actualizada").build());

		mockMvc.perform(put("/ordenes/1/lineas/2")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Actualizada"));
	}

	@Test
	void eliminarLineaDevuelve204() throws Exception {
		mockMvc.perform(delete("/ordenes/1/lineas/2")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isNoContent());

		verify(service).eliminarLinea(1L, 2L);
	}

	@Test
	void eliminarLineaDeOrdenCerradaDevuelve400() throws Exception {
		org.mockito.Mockito.doThrow(new IllegalArgumentException(
				"No se pueden modificar líneas de una OT en estado ENTREGADO"))
				.when(service).eliminarLinea(1L, 2L);

		mockMvc.perform(delete("/ordenes/1/lineas/2")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("No se pueden modificar líneas")));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/ordenes/1/lineas"))
				.andExpect(status().isUnauthorized());

		verify(service, never()).listarLineas(any(Long.class));
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/ordenes/1/lineas")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listarLineas(any(Long.class));
	}

	private LineaRequest request() {
		return LineaRequest.builder()
				.tipo(TipoLinea.OMEX)
				.descripcion("Trabajo externo")
				.costo(14000)
				.build();
	}

}

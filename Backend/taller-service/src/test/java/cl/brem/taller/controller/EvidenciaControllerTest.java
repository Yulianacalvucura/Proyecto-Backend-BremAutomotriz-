package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.EvidenciaRequest;
import cl.brem.taller.dto.EvidenciaResponse;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.TipoEvidencia;
import cl.brem.taller.service.OrdenTrabajoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EvidenciaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EvidenciaControllerTest {

	private static final String USUARIO = "jefe@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrdenTrabajoService service;

	@Test
	void listarEvidenciasDevuelve200() throws Exception {
		when(service.listarEvidencias(1L)).thenReturn(List.of(EvidenciaResponse.builder()
				.id(1L).otId(1L).tipo("VEHICULO").ruta("/fotos/1.jpg").build()));

		mockMvc.perform(get("/ordenes/1/evidencias")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ruta").value("/fotos/1.jpg"));

		verify(service).listarEvidencias(1L);
	}

	@Test
	void agregarEvidenciaGuardaSoloLaRutaYDevuelve201() throws Exception {
		when(service.agregarEvidencia(any(Long.class), any(EvidenciaRequest.class)))
				.thenReturn(EvidenciaResponse.builder()
						.id(2L).otId(1L).tipo("VEHICULO").ruta("/fotos/2.jpg").build());

		mockMvc.perform(post("/ordenes/1/evidencias")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(EvidenciaRequest.builder()
								.ruta("/fotos/2.jpg").build())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ruta").value("/fotos/2.jpg"))
				.andExpect(jsonPath("$.tipo").value("VEHICULO"));
	}

	@Test
	void agregarEvidenciaEnOrdenCerradaSePermite() throws Exception {
		when(service.agregarEvidencia(any(Long.class), any(EvidenciaRequest.class)))
				.thenReturn(EvidenciaResponse.builder()
						.id(3L).otId(1L).tipo("DOCUMENTO").ruta("/docs/entrega.pdf").build());

		mockMvc.perform(post("/ordenes/1/evidencias")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(EvidenciaRequest.builder()
								.ruta("/docs/entrega.pdf").tipo(TipoEvidencia.DOCUMENTO).build())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("DOCUMENTO"));
	}

	@Test
	void agregarEvidenciaSinRutaDevuelve400() throws Exception {
		mockMvc.perform(post("/ordenes/1/evidencias")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(EvidenciaRequest.builder()
								.ruta("   ").build())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("ruta")));

		verify(service, never()).agregarEvidencia(any(Long.class), any(EvidenciaRequest.class));
	}

	@Test
	void listarEvidenciasDeOrdenInexistenteDevuelve404() throws Exception {
		when(service.listarEvidencias(99L))
				.thenThrow(new ResourceNotFoundException("Orden de trabajo no encontrada con id: 99"));

		mockMvc.perform(get("/ordenes/99/evidencias")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje", containsString("Orden de trabajo no encontrada")));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/ordenes/1/evidencias"))
				.andExpect(status().isUnauthorized());

		verify(service, never()).listarEvidencias(any(Long.class));
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/ordenes/1/evidencias")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listarEvidencias(any(Long.class));
	}

}

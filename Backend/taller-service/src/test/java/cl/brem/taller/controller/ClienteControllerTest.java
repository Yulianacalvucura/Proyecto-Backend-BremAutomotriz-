package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.ClienteRequest;
import cl.brem.taller.dto.ClienteResponse;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.service.ClienteService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClienteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClienteControllerTest {

	private static final String USUARIO = "admin1@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ClienteService service;

	@Test
	void listarConRolJefeDevuelve200() throws Exception {
		when(service.listar()).thenReturn(List.of(ClienteResponse.builder()
				.id(1L)
				.nombre("Cliente Ejemplo Uno")
				.run("11.111.111-1")
				.build()));

		mockMvc.perform(get("/clientes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Cliente Ejemplo Uno"));

		verify(service).listar();
	}

	@Test
	void buscarPorIdExistenteDevuelve200() throws Exception {
		when(service.buscarPorId(1L)).thenReturn(ClienteResponse.builder()
				.id(1L)
				.nombre("Cliente Ejemplo Uno")
				.build());

		mockMvc.perform(get("/clientes/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nombre").value("Cliente Ejemplo Uno"));
	}

	@Test
	void buscarPorIdInexistenteDevuelve404ConCuerpoDeError() throws Exception {
		when(service.buscarPorId(99L))
				.thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));

		mockMvc.perform(get("/clientes/99")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.mensaje", containsString("Cliente no encontrado")))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void crearValidoDevuelve201() throws Exception {
		when(service.crear(any(ClienteRequest.class))).thenReturn(ClienteResponse.builder()
				.id(3L)
				.nombre("Cliente Nuevo")
				.run("33.333.333-3")
				.build());
		String cuerpo = objectMapper.writeValueAsString(ClienteRequest.builder()
				.nombre("Cliente Nuevo")
				.run("33.333.333-3")
				.telefono("+56 9 3333 3333")
				.correo("cliente3@ejemplo.cl")
				.build());

		mockMvc.perform(post("/clientes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.run").value("33.333.333-3"));
	}

	@Test
	void crearConDatosInvalidosDevuelve400ConDetallePorCampo() throws Exception {
		String cuerpo = objectMapper.writeValueAsString(ClienteRequest.builder()
				.nombre("")
				.run("12345678901234")
				.build());

		mockMvc.perform(post("/clientes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensaje", containsString("nombre")))
				.andExpect(jsonPath("$.mensaje", containsString("run")));

		verify(service, never()).crear(any(ClienteRequest.class));
	}

	@Test
	void crearConRunDuplicadoDevuelve400() throws Exception {
		when(service.crear(any(ClienteRequest.class)))
				.thenThrow(new IllegalArgumentException("El RUN ya está registrado: 11.111.111-1"));
		String cuerpo = objectMapper.writeValueAsString(ClienteRequest.builder()
				.nombre("Cliente Duplicado")
				.run("11.111.111-1")
				.build());

		mockMvc.perform(post("/clientes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("RUN ya está registrado")));
	}

	@Test
	void actualizarValidoDevuelve200() throws Exception {
		when(service.actualizar(any(Long.class), any(ClienteRequest.class)))
				.thenReturn(ClienteResponse.builder()
						.id(1L)
						.nombre("Cliente Actualizado")
						.build());
		String cuerpo = objectMapper.writeValueAsString(ClienteRequest.builder()
				.nombre("Cliente Actualizado")
				.build());

		mockMvc.perform(put("/clientes/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Cliente Actualizado"));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/clientes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensaje").value("Autenticación requerida"));

		verify(service, never()).listar();
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/clientes")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listar();
	}

}

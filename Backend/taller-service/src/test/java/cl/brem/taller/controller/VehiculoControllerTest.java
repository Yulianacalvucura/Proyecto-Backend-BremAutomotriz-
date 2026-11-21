package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.VehiculoDetalleResponse;
import cl.brem.taller.dto.VehiculoRequest;
import cl.brem.taller.dto.VehiculoResponse;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.service.VehiculoService;
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

@WebMvcTest(controllers = VehiculoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VehiculoControllerTest {

	private static final String USUARIO = "jefe@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private VehiculoService service;

	@Test
	void listarConRolJefeDevuelve200() throws Exception {
		when(service.listar()).thenReturn(List.of(VehiculoResponse.builder()
				.id(1L)
				.patente("ABCD12")
				.marca("Toyota")
				.clienteId(1L)
				.build()));

		mockMvc.perform(get("/vehiculos")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].patente").value("ABCD12"));

		verify(service).listar();
	}

	@Test
	void buscarPorIdInexistenteDevuelve404() throws Exception {
		when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Vehículo no encontrado con id: 99"));

		mockMvc.perform(get("/vehiculos/99")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.mensaje", containsString("Vehículo no encontrado")));
	}

	@Test
	void listarPorClienteDevuelve200() throws Exception {
		when(service.listarPorCliente(1L)).thenReturn(List.of(
				VehiculoResponse.builder().id(1L).patente("ABCD12").clienteId(1L).build(),
				VehiculoResponse.builder().id(2L).patente("WXYZ34").clienteId(1L).build()));

		mockMvc.perform(get("/vehiculos/cliente/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		verify(service).listarPorCliente(1L);
	}

	@Test
	void buscarPorPatenteDevuelveVehiculoYCliente() throws Exception {
		when(service.buscarPorPatente("abcd12")).thenReturn(VehiculoDetalleResponse.builder()
				.id(1L)
				.patente("ABCD12")
				.marca("Toyota")
				.modelo("Yaris")
				.anio(2018)
				.clienteId(1L)
				.clienteNombre("Cliente Ejemplo Uno")
				.clienteRun("11.111.111-1")
				.build());

		mockMvc.perform(get("/vehiculos/patente/abcd12")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.patente").value("ABCD12"))
				.andExpect(jsonPath("$.clienteNombre").value("Cliente Ejemplo Uno"));

		verify(service).buscarPorPatente("abcd12");
	}

	@Test
	void buscarPorPatenteInexistenteDevuelve404() throws Exception {
		when(service.buscarPorPatente("ZZZZ99"))
				.thenThrow(new ResourceNotFoundException("Vehículo no encontrado con patente: ZZZZ99"));

		mockMvc.perform(get("/vehiculos/patente/ZZZZ99")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje", containsString("Vehículo no encontrado")));
	}

	@Test
	void crearValidoDevuelve201() throws Exception {
		when(service.crear(any(VehiculoRequest.class))).thenReturn(VehiculoResponse.builder()
				.id(3L)
				.patente("ABCD12")
				.marca("Toyota")
				.clienteId(1L)
				.build());
		String cuerpo = objectMapper.writeValueAsString(request(2018, 1L));

		mockMvc.perform(post("/vehiculos")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.patente").value("ABCD12"));
	}

	@Test
	void crearConAnioFueraDeRangoDevuelve400ConDetallePorCampo() throws Exception {
		String cuerpo = objectMapper.writeValueAsString(request(1949, 1L));

		mockMvc.perform(post("/vehiculos")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensaje", containsString("anio")));

		verify(service, never()).crear(any(VehiculoRequest.class));
	}

	@Test
	void crearConClienteInexistenteDevuelve404() throws Exception {
		when(service.crear(any(VehiculoRequest.class)))
				.thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 99"));
		String cuerpo = objectMapper.writeValueAsString(request(2018, 99L));

		mockMvc.perform(post("/vehiculos")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje", containsString("Cliente no encontrado")));
	}

	@Test
	void crearConPatenteDuplicadaDevuelve400() throws Exception {
		when(service.crear(any(VehiculoRequest.class)))
				.thenThrow(new IllegalArgumentException("La patente ya está registrada: ABCD12"));
		String cuerpo = objectMapper.writeValueAsString(request(2018, 1L));

		mockMvc.perform(post("/vehiculos")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("patente ya está registrada")));
	}

	@Test
	void actualizarValidoDevuelve200() throws Exception {
		when(service.actualizar(any(Long.class), any(VehiculoRequest.class)))
				.thenReturn(VehiculoResponse.builder()
						.id(1L)
						.patente("ABCD12")
						.marca("Toyota Actualizado")
						.clienteId(1L)
						.build());
		String cuerpo = objectMapper.writeValueAsString(request(2019, 1L));

		mockMvc.perform(put("/vehiculos/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.marca").value("Toyota Actualizado"));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/vehiculos"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verify(service, never()).listar();
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/vehiculos")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listar();
	}

	private VehiculoRequest request(Integer anio, Long clienteId) {
		return VehiculoRequest.builder()
				.patente(" abcd12 ")
				.marca("Toyota")
				.modelo("Yaris")
				.anio(anio)
				.tipo("Auto")
				.clienteId(clienteId)
				.build();
	}

}

package cl.brem.taller.controller;

import cl.brem.taller.config.SecurityConfig;
import cl.brem.taller.dto.CambioEstadoRequest;
import cl.brem.taller.dto.LineaResponse;
import cl.brem.taller.dto.ObservacionRequest;
import cl.brem.taller.dto.OrdenDetalleResponse;
import cl.brem.taller.dto.OrdenRequest;
import cl.brem.taller.dto.OrdenResponse;
import cl.brem.taller.dto.VistaOrdenResponse;
import cl.brem.taller.exception.AutenticacionRequeridaException;
import cl.brem.taller.exception.GlobalExceptionHandler;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.EstadoOt;
import cl.brem.taller.model.TipoLinea;
import cl.brem.taller.service.OrdenTrabajoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrdenController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class OrdenControllerTest {

	private static final String USUARIO = "jefe@brem.cl";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrdenTrabajoService service;

	@Test
	void listarConRolJefeDevuelve200() throws Exception {
		when(service.listar()).thenReturn(List.of(OrdenResponse.builder()
				.id(1L).patente("ABCD12").estado("PENDIENTE").total(112574).build()));

		mockMvc.perform(get("/ordenes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].patente").value("ABCD12"))
				.andExpect(jsonPath("$[0].total").value(112574));

		verify(service).listar();
	}

	@Test
	void buscarPorPatenteUsuarioONumeroDevuelve200DesdeLaVista() throws Exception {
		when(service.buscar("abcd")).thenReturn(List.of(VistaOrdenResponse.builder()
				.otId(1L).patente("ABCD12").nombreCliente("Cliente Ejemplo Uno")
				.estado("PENDIENTE").total(112574).build()));

		mockMvc.perform(get("/ordenes/buscar").param("q", "abcd")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].otId").value(1))
				.andExpect(jsonPath("$[0].nombreCliente").value("Cliente Ejemplo Uno"));

		verify(service).buscar("abcd");
	}

	@Test
	void buscarPorIdDevuelveOrdenConLineasEvidenciasEHistorial() throws Exception {
		when(service.buscarPorId(1L)).thenReturn(OrdenDetalleResponse.builder()
				.orden(OrdenResponse.builder().id(1L).neto(94600).iva(17974).total(112574).build())
				.lineas(List.of(LineaResponse.builder().id(1L).cobrado(30000).build()))
				.build());

		mockMvc.perform(get("/ordenes/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orden.neto").value(94600))
				.andExpect(jsonPath("$.orden.iva").value(17974))
				.andExpect(jsonPath("$.orden.total").value(112574))
				.andExpect(jsonPath("$.lineas[0].cobrado").value(30000));
	}

	@Test
	void buscarPorIdInexistenteDevuelve404() throws Exception {
		when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Orden de trabajo no encontrada con id: 99"));

		mockMvc.perform(get("/ordenes/99")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.mensaje", containsString("Orden de trabajo no encontrada")))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void crearValidoConXUserIdDevuelve201() throws Exception {
		when(service.crear(any(OrdenRequest.class), anyString())).thenReturn(detalle());
		String cuerpo = objectMapper.writeValueAsString(request());

		mockMvc.perform(post("/ordenes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.header("X-User-Id", "7")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orden.id").value(1))
				.andExpect(jsonPath("$.orden.total").value(112574));

		verify(service).crear(any(OrdenRequest.class), anyString());
	}

	@Test
	void crearSinXUserIdDevuelve401() throws Exception {
		when(service.crear(any(OrdenRequest.class), any()))
				.thenThrow(new AutenticacionRequeridaException("X-User-Id requerido"));
		String cuerpo = objectMapper.writeValueAsString(request());

		mockMvc.perform(post("/ordenes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensaje").value("X-User-Id requerido"));
	}

	@Test
	void crearConDatosInvalidosDevuelve400ConDetallePorCampo() throws Exception {
		String cuerpo = objectMapper.writeValueAsString(OrdenRequest.builder()
				.vehiculoId(1L)
				.kilometraje(-1)
				.observacionIngreso("")
				.porcentajeIva(new BigDecimal("101.00"))
				.build());

		mockMvc.perform(post("/ordenes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.header("X-User-Id", "7")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensaje", containsString("kilometraje")))
				.andExpect(jsonPath("$.mensaje", containsString("observacionIngreso")))
				.andExpect(jsonPath("$.mensaje", containsString("porcentajeIva")));

		verify(service, never()).crear(any(OrdenRequest.class), anyString());
	}

	@Test
	void crearConLineaInvalidaDevuelve400() throws Exception {
		OrdenRequest request = request();
		request.getLineas().get(0).setCosto(-5);
		String cuerpo = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/ordenes")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.header("X-User-Id", "7")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("costo")));

		verify(service, never()).crear(any(OrdenRequest.class), anyString());
	}

	@Test
	void actualizarObservacionValidoDevuelve200() throws Exception {
		when(service.actualizarObservacion(any(Long.class), any(ObservacionRequest.class)))
				.thenReturn(OrdenResponse.builder().id(1L).observacionIngreso("Observación actualizada").build());

		mockMvc.perform(put("/ordenes/1")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								ObservacionRequest.builder().observacionIngreso("Observación actualizada").build())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.observacionIngreso").value("Observación actualizada"));

		verify(service).actualizarObservacion(any(Long.class), any(ObservacionRequest.class));
	}

	@Test
	void cambiarEstadoValidoDevuelve200() throws Exception {
		when(service.cambiarEstado(any(Long.class), any(CambioEstadoRequest.class))).thenReturn(detalle());

		mockMvc.perform(patch("/ordenes/1/estado")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(CambioEstadoRequest.builder()
								.estado(EstadoOt.ENTREGADO).observacionEntrega("Entregada conforme").build())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orden.id").value(1));

		verify(service).cambiarEstado(any(Long.class), any(CambioEstadoRequest.class));
	}

	@Test
	void cambiarEstadoDeserializaObservacionEntregaDelBody() throws Exception {
		when(service.cambiarEstado(any(Long.class), any(CambioEstadoRequest.class))).thenReturn(detalle());

		mockMvc.perform(patch("/ordenes/1/estado")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"ENTREGADO\",\"observacionEntrega\":\"Entregada conforme\"}"))
				.andExpect(status().isOk());

		ArgumentCaptor<CambioEstadoRequest> captor = ArgumentCaptor.forClass(CambioEstadoRequest.class);
		verify(service).cambiarEstado(any(Long.class), captor.capture());
		assertEquals(EstadoOt.ENTREGADO, captor.getValue().getEstado());
		assertEquals("Entregada conforme", captor.getValue().getObservacionEntrega());
	}

	@Test
	void cambiarEstadoConTransicionInvalidaDevuelve400() throws Exception {
		when(service.cambiarEstado(any(Long.class), any(CambioEstadoRequest.class)))
				.thenThrow(new IllegalArgumentException("Transición de estado no permitida: PENDIENTE -> ENTREGADO"));

		mockMvc.perform(patch("/ordenes/1/estado")
						.header("X-User-Username", USUARIO)
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(CambioEstadoRequest.builder()
								.estado(EstadoOt.ENTREGADO).observacionEntrega("Entregada").build())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje", containsString("Transición de estado no permitida")));
	}

	@Test
	void sinHeadersDevuelve401() throws Exception {
		mockMvc.perform(get("/ordenes"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verify(service, never()).listar();
	}

	@Test
	void conRolNoAutorizadoDevuelve403() throws Exception {
		mockMvc.perform(get("/ordenes")
						.header("X-User-Username", "ayudante@brem.cl")
						.header("X-User-Roles", "AYUDANTE"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.mensaje").value("Acceso denegado"));

		verify(service, never()).listar();
	}

	private OrdenRequest request() {
		return OrdenRequest.builder()
				.vehiculoId(1L)
				.kilometraje(50000)
				.observacionIngreso("Revisión general")
				.lineas(new java.util.ArrayList<>(List.of(
						cl.brem.taller.dto.LineaRequest.builder()
								.tipo(TipoLinea.OM).descripcion("Mano de obra").costo(30000).build())))
				.build();
	}

	private OrdenDetalleResponse detalle() {
		return OrdenDetalleResponse.builder()
				.orden(OrdenResponse.builder().id(1L).total(112574).build())
				.build();
	}

}

package cl.brem.taller.service;

import cl.brem.taller.dto.CambioEstadoRequest;
import cl.brem.taller.dto.EvidenciaRequest;
import cl.brem.taller.dto.EvidenciaResponse;
import cl.brem.taller.dto.LineaRequest;
import cl.brem.taller.dto.LineaResponse;
import cl.brem.taller.dto.ObservacionRequest;
import cl.brem.taller.dto.OrdenDetalleResponse;
import cl.brem.taller.dto.OrdenRequest;
import cl.brem.taller.dto.OrdenResponse;
import cl.brem.taller.dto.VistaOrdenResponse;
import cl.brem.taller.exception.AutenticacionRequeridaException;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.Cliente;
import cl.brem.taller.model.EstadoOt;
import cl.brem.taller.model.EvidenciaOt;
import cl.brem.taller.model.HistorialEstado;
import cl.brem.taller.model.LineaOt;
import cl.brem.taller.model.OrdenTrabajo;
import cl.brem.taller.model.TipoEvidencia;
import cl.brem.taller.model.TipoLinea;
import cl.brem.taller.model.Vehiculo;
import cl.brem.taller.model.VistaOrden;
import cl.brem.taller.repository.ClienteRepository;
import cl.brem.taller.repository.EvidenciaOtRepository;
import cl.brem.taller.repository.HistorialEstadoRepository;
import cl.brem.taller.repository.LineaOtRepository;
import cl.brem.taller.repository.OrdenTrabajoRepository;
import cl.brem.taller.repository.VehiculoRepository;
import cl.brem.taller.repository.VistaOrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdenTrabajoServiceTest {

	private static final Long OT_ID = 1L;

	@Mock
	private OrdenTrabajoRepository ordenTrabajoRepository;

	@Mock
	private LineaOtRepository lineaOtRepository;

	@Mock
	private EvidenciaOtRepository evidenciaOtRepository;

	@Mock
	private HistorialEstadoRepository historialEstadoRepository;

	@Mock
	private VistaOrdenRepository vistaOrdenRepository;

	@Mock
	private VehiculoRepository vehiculoRepository;

	@Mock
	private ClienteRepository clienteRepository;

	@InjectMocks
	private OrdenTrabajoService service;

	@BeforeEach
	void configurarGuardados() {
		when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));
		when(lineaOtRepository.save(any(LineaOt.class))).thenAnswer(inv -> inv.getArgument(0));
		when(evidenciaOtRepository.save(any(EvidenciaOt.class))).thenAnswer(inv -> inv.getArgument(0));
		when(historialEstadoRepository.save(any(HistorialEstado.class))).thenAnswer(inv -> inv.getArgument(0));
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(Vehiculo.builder()
				.id(1L).patente("ABCD12").clienteId(1L).build()));
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(Cliente.builder()
				.id(1L).nombre("Cliente Ejemplo Uno").build()));
	}

	@Test
	void casoObligatorioCalculaNetoIvaYTotal() {
		when(vehiculoRepository.existsById(1L)).thenReturn(true);
		when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> {
			OrdenTrabajo orden = inv.getArgument(0);
			if (orden.getId() == null) {
				orden.setId(OT_ID);
			}
			return orden;
		});
		List<LineaOt> guardadas = new ArrayList<>();
		when(lineaOtRepository.save(any(LineaOt.class))).thenAnswer(inv -> {
			LineaOt linea = inv.getArgument(0);
			linea.setId((long) (guardadas.size() + 1));
			guardadas.add(linea);
			return linea;
		});
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID)).thenAnswer(inv -> List.copyOf(guardadas));

		OrdenDetalleResponse resultado = service.crear(requestCasoObligatorio(), "7");

		assertEquals(4, resultado.getLineas().size());
		assertEquals(30000, resultado.getLineas().get(0).getCobrado());
		assertEquals(18200, resultado.getLineas().get(1).getCobrado());
		assertEquals(36400, resultado.getLineas().get(2).getCobrado());
		assertEquals(10000, resultado.getLineas().get(3).getCobrado());
		assertEquals(new BigDecimal("1.00"), resultado.getLineas().get(0).getFactor());
		assertEquals(new BigDecimal("1.30"), resultado.getLineas().get(1).getFactor());
		assertEquals(94600, resultado.getOrden().getNeto());
		assertEquals(17974, resultado.getOrden().getIva());
		assertEquals(112574, resultado.getOrden().getTotal());
		assertEquals(new BigDecimal("19.00"), resultado.getOrden().getPorcentajeIva());
		assertEquals("PENDIENTE", resultado.getOrden().getEstado());
		assertEquals(7L, resultado.getOrden().getCreadoPorId());
		assertEquals("ABCD12", resultado.getOrden().getPatente());
		assertEquals("Cliente Ejemplo Uno", resultado.getOrden().getNombreCliente());
		assertNotNull(resultado.getOrden().getFechaIngreso());
		assertNotNull(resultado.getOrden().getFechaRegistro());
	}

	@Test
	void crearPersistePorcentajeIvaPersonalizado() {
		when(vehiculoRepository.existsById(1L)).thenReturn(true);
		when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> {
			OrdenTrabajo orden = inv.getArgument(0);
			orden.setId(OT_ID);
			return orden;
		});
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID))
				.thenReturn(List.of(linea(10000, 10000, new BigDecimal("1.00"))));

		OrdenRequest request = OrdenRequest.builder()
				.vehiculoId(1L)
				.kilometraje(1000)
				.observacionIngreso("Cambio de aceite")
				.porcentajeIva(new BigDecimal("10.00"))
				.build();

		OrdenDetalleResponse resultado = service.crear(request, "7");

		assertEquals(new BigDecimal("10.00"), resultado.getOrden().getPorcentajeIva());
		assertEquals(10000, resultado.getOrden().getNeto());
		assertEquals(1000, resultado.getOrden().getIva());
		assertEquals(11000, resultado.getOrden().getTotal());
	}

	@Test
	void calcularCobradoUsaFactorOverrideYHalfUp() {
		when(vehiculoRepository.existsById(1L)).thenReturn(true);
		when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> {
			OrdenTrabajo orden = inv.getArgument(0);
			orden.setId(OT_ID);
			return orden;
		});
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID))
				.thenReturn(List.of(linea(10, 11, new BigDecimal("1.05"))));

		OrdenRequest request = OrdenRequest.builder()
				.vehiculoId(1L)
				.kilometraje(1000)
				.observacionIngreso("Prueba de redondeo")
				.lineas(List.of(LineaRequest.builder()
						.tipo(TipoLinea.OM)
						.descripcion("Mano de obra")
						.costo(10)
						.factor(new BigDecimal("1.05"))
						.build()))
				.build();

		OrdenDetalleResponse resultado = service.crear(request, "7");

		assertEquals(11, resultado.getLineas().get(0).getCobrado());
		assertEquals(11, resultado.getOrden().getNeto());
		assertEquals(2, resultado.getOrden().getIva());
		assertEquals(13, resultado.getOrden().getTotal());
	}

	@Test
	void crearSinXUserIdResponde401() {
		AutenticacionRequeridaException ex = assertThrows(AutenticacionRequeridaException.class,
				() -> service.crear(requestCasoObligatorio(), null));

		assertEquals("X-User-Id requerido", ex.getMessage());
		verifyNoInteractions(ordenTrabajoRepository, lineaOtRepository, vehiculoRepository);
	}

	@Test
	void crearConXUserIdNoNumericoLanzaIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> service.crear(requestCasoObligatorio(), "abc"));
		verifyNoInteractions(ordenTrabajoRepository, lineaOtRepository, vehiculoRepository);
	}

	@Test
	void crearConVehiculoInexistenteLanzaResourceNotFound() {
		when(vehiculoRepository.existsById(99L)).thenReturn(false);

		OrdenRequest request = OrdenRequest.builder()
				.vehiculoId(99L).kilometraje(1000).observacionIngreso("Sin vehículo").build();

		assertThrows(ResourceNotFoundException.class, () -> service.crear(request, "7"));
		verify(ordenTrabajoRepository, never()).save(any(OrdenTrabajo.class));
	}

	@Test
	void listarUsaLaTablaDeOrdenes() {
		when(ordenTrabajoRepository.findAllByOrderByIdDesc())
				.thenReturn(List.of(orden(EstadoOt.PENDIENTE), orden(EstadoOt.LISTO)));

		List<OrdenResponse> resultado = service.listar();

		assertEquals(2, resultado.size());
		verify(ordenTrabajoRepository).findAllByOrderByIdDesc();
		verifyNoInteractions(vistaOrdenRepository);
	}

	@Test
	void buscarUsaSoloLaVistaDeOrdenes() {
		when(vistaOrdenRepository.buscar("abcd")).thenReturn(List.of(VistaOrden.builder()
				.otId(1L).patente("ABCD12").nombreCliente("Cliente Ejemplo Uno")
				.estado("PENDIENTE").total(112574).vehiculoId(1L).build()));

		List<VistaOrdenResponse> resultado = service.buscar(" abcd ");

		assertEquals(1, resultado.size());
		assertEquals(1L, resultado.get(0).getOtId());
		assertEquals("ABCD12", resultado.get(0).getPatente());
		assertEquals(112574, resultado.get(0).getTotal());
		verify(vistaOrdenRepository).buscar("abcd");
		verifyNoInteractions(ordenTrabajoRepository);
	}

	@Test
	void buscarPorIdDevuelveDetalleCompleto() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.PENDIENTE)));
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID))
				.thenReturn(List.of(linea(30000, 30000, new BigDecimal("1.00"))));
		when(evidenciaOtRepository.findByOtIdOrderByIdAsc(OT_ID)).thenReturn(List.of(EvidenciaOt.builder()
				.id(1L).otId(OT_ID).tipo(TipoEvidencia.VEHICULO).ruta("/fotos/1.jpg").build()));
		when(historialEstadoRepository.findByOtIdOrderByIdAsc(OT_ID)).thenReturn(List.of(HistorialEstado.builder()
				.id(1L).otId(OT_ID).estadoAnterior(null).estadoNuevo(EstadoOt.PENDIENTE).build()));

		OrdenDetalleResponse resultado = service.buscarPorId(OT_ID);

		assertEquals(1, resultado.getLineas().size());
		assertEquals(1, resultado.getEvidencias().size());
		assertEquals("/fotos/1.jpg", resultado.getEvidencias().get(0).getRuta());
		assertEquals(1, resultado.getHistorial().size());
		assertEquals("PENDIENTE", resultado.getHistorial().get(0).getEstadoNuevo());
	}

	@Test
	void buscarPorIdInexistenteLanzaResourceNotFound() {
		when(ordenTrabajoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
	}

	@Test
	void actualizarObservacionSoloCambiaLaObservacionDeIngreso() {
		OrdenTrabajo orden = orden(EstadoOt.EN_PROCESO);
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden));

		OrdenResponse resultado = service.actualizarObservacion(OT_ID,
				ObservacionRequest.builder().observacionIngreso("Nueva observación").build());

		ArgumentCaptor<OrdenTrabajo> captor = ArgumentCaptor.forClass(OrdenTrabajo.class);
		verify(ordenTrabajoRepository).save(captor.capture());
		assertEquals("Nueva observación", captor.getValue().getObservacionIngreso());
		assertEquals(EstadoOt.EN_PROCESO, captor.getValue().getEstado());
		assertEquals("Nueva observación", resultado.getObservacionIngreso());
	}

	@ParameterizedTest
	@CsvSource({
			"PENDIENTE,EN_PROCESO",
			"EN_PROCESO,LISTO",
			"LISTO,ENTREGADO",
			"PENDIENTE,ANULADA",
			"EN_PROCESO,ANULADA",
			"LISTO,ANULADA"
	})
	void cambiarEstadoTransicionesValidasRegistranHistorial(String desde, String hacia) {
		EstadoOt anterior = EstadoOt.valueOf(desde);
		EstadoOt nuevo = EstadoOt.valueOf(hacia);
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(anterior)));

		service.cambiarEstado(OT_ID, CambioEstadoRequest.builder()
				.estado(nuevo).observacionEntrega("Entregada conforme").build());

		ArgumentCaptor<HistorialEstado> captor = ArgumentCaptor.forClass(HistorialEstado.class);
		verify(historialEstadoRepository).save(captor.capture());
		assertEquals(anterior, captor.getValue().getEstadoAnterior());
		assertEquals(nuevo, captor.getValue().getEstadoNuevo());
		assertEquals(OT_ID, captor.getValue().getOtId());
		assertNotNull(captor.getValue().getFechaCambio());
		if (nuevo == EstadoOt.ENTREGADO) {
			assertEquals("Entregada conforme", captor.getValue().getObservacion());
		}
	}

	@ParameterizedTest
	@CsvSource({
			"PENDIENTE,LISTO",
			"PENDIENTE,ENTREGADO",
			"EN_PROCESO,ENTREGADO",
			"LISTO,PENDIENTE",
			"ENTREGADO,ANULADA",
			"ANULADA,PENDIENTE"
	})
	void cambiarEstadoTransicionesInvalidasLanzanIllegalArgument(String desde, String hacia) {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.valueOf(desde))));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> service.cambiarEstado(OT_ID, CambioEstadoRequest.builder()
						.estado(EstadoOt.valueOf(hacia)).build()));

		assertEquals("Transición de estado no permitida: " + desde + " -> " + hacia, ex.getMessage());
		verify(historialEstadoRepository, never()).save(any(HistorialEstado.class));
	}

	@Test
	void cambiarAEntregadoConObservacionEnBlancoLanzaIllegalArgument() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.LISTO)));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> service.cambiarEstado(OT_ID, CambioEstadoRequest.builder()
						.estado(EstadoOt.ENTREGADO).observacionEntrega("   ").build()));

		assertEquals("La observación de entrega es obligatoria al pasar a ENTREGADO", ex.getMessage());
		verify(historialEstadoRepository, never()).save(any(HistorialEstado.class));
	}

	@Test
	void cambiarAEntregadoSinObservacionEntregaLanzaIllegalArgument() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.LISTO)));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> service.cambiarEstado(OT_ID, CambioEstadoRequest.builder()
						.estado(EstadoOt.ENTREGADO).build()));

		assertEquals("La observación de entrega es obligatoria al pasar a ENTREGADO", ex.getMessage());
		verify(historialEstadoRepository, never()).save(any(HistorialEstado.class));
	}

	@Test
	void cambiarAEntregadoGuardaLaObservacionDeEntrega() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.LISTO)));

		OrdenDetalleResponse resultado = service.cambiarEstado(OT_ID, CambioEstadoRequest.builder()
				.estado(EstadoOt.ENTREGADO).observacionEntrega("Entregada con revisión").build());

		assertEquals("ENTREGADO", resultado.getOrden().getEstado());
		assertEquals("Entregada con revisión", resultado.getOrden().getObservacionEntrega());
		ArgumentCaptor<OrdenTrabajo> captor = ArgumentCaptor.forClass(OrdenTrabajo.class);
		verify(ordenTrabajoRepository).save(captor.capture());
		assertEquals("Entregada con revisión", captor.getValue().getObservacionEntrega());
	}

	@Test
	void noSeInsertaHistorialAlCrearLaOrden() {
		when(vehiculoRepository.existsById(1L)).thenReturn(true);
		when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> {
			OrdenTrabajo orden = inv.getArgument(0);
			orden.setId(OT_ID);
			return orden;
		});

		service.crear(requestCasoObligatorio(), "7");

		verify(historialEstadoRepository, never()).save(any(HistorialEstado.class));
	}

	@Test
	void agregarLineaRecalculaLosMontos() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.PENDIENTE)));
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID))
				.thenReturn(List.of(linea(10000, 13000, new BigDecimal("1.30"))));

		LineaResponse resultado = service.agregarLinea(OT_ID, LineaRequest.builder()
				.tipo(TipoLinea.REPUESTO).descripcion("Repuesto").costo(10000).build());

		assertEquals(13000, resultado.getCobrado());
		assertEquals(new BigDecimal("1.30"), resultado.getFactor());
		ArgumentCaptor<OrdenTrabajo> captor = ArgumentCaptor.forClass(OrdenTrabajo.class);
		verify(ordenTrabajoRepository).save(captor.capture());
		assertEquals(13000, captor.getValue().getNeto());
		assertEquals(2470, captor.getValue().getIva());
		assertEquals(15470, captor.getValue().getTotal());
	}

	@Test
	void actualizarLineaRecalculaLosMontos() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.EN_PROCESO)));
		LineaOt existente = linea(10000, 10000, new BigDecimal("1.00"));
		existente.setId(5L);
		when(lineaOtRepository.findById(5L)).thenReturn(Optional.of(existente));
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID))
				.thenReturn(List.of(linea(10000, 13000, new BigDecimal("1.30"))));

		LineaResponse resultado = service.actualizarLinea(OT_ID, 5L, LineaRequest.builder()
				.tipo(TipoLinea.OMEX).descripcion("Trabajo externo").costo(10000).build());

		assertEquals(13000, resultado.getCobrado());
		assertEquals(15470, captorIvaYTotal().getTotal());
	}

	private OrdenTrabajo captorIvaYTotal() {
		ArgumentCaptor<OrdenTrabajo> captor = ArgumentCaptor.forClass(OrdenTrabajo.class);
		verify(ordenTrabajoRepository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void eliminarLineaRecalculaLosMontos() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.PENDIENTE)));
		LineaOt existente = linea(10000, 13000, new BigDecimal("1.30"));
		existente.setId(5L);
		when(lineaOtRepository.findById(5L)).thenReturn(Optional.of(existente));
		when(lineaOtRepository.findByOtIdOrderByIdAsc(OT_ID)).thenReturn(List.of());

		service.eliminarLinea(OT_ID, 5L);

		verify(lineaOtRepository).delete(existente);
		OrdenTrabajo guardada = captorIvaYTotal();
		assertEquals(0, guardada.getNeto());
		assertEquals(0, guardada.getIva());
		assertEquals(0, guardada.getTotal());
	}

	@ParameterizedTest
	@EnumSource(value = EstadoOt.class, names = {"ENTREGADO", "ANULADA"})
	void lineasBloqueadasEnOrdenesCerradas(EstadoOt estado) {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(estado)));

		LineaRequest request = LineaRequest.builder()
				.tipo(TipoLinea.OM).descripcion("Trabajo").costo(1000).build();

		assertThrows(IllegalArgumentException.class, () -> service.agregarLinea(OT_ID, request));
		assertThrows(IllegalArgumentException.class, () -> service.actualizarLinea(OT_ID, 5L, request));
		assertThrows(IllegalArgumentException.class, () -> service.eliminarLinea(OT_ID, 5L));
		verifyNoInteractions(lineaOtRepository);
	}

	@Test
	void lineaDeOtraOrdenNoSeEncuentra() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.PENDIENTE)));
		LineaOt deOtraOrden = linea(1000, 1000, new BigDecimal("1.00"));
		deOtraOrden.setId(5L);
		deOtraOrden.setOtId(2L);
		when(lineaOtRepository.findById(5L)).thenReturn(Optional.of(deOtraOrden));

		assertThrows(ResourceNotFoundException.class, () -> service.eliminarLinea(OT_ID, 5L));
		verify(lineaOtRepository, never()).delete(any(LineaOt.class));
	}

	@Test
	void agregarEvidenciaGuardaSoloLaRutaConTipoDefault() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.PENDIENTE)));

		EvidenciaResponse resultado = service.agregarEvidencia(OT_ID, EvidenciaRequest.builder()
				.ruta("/fotos/vehiculo.jpg").build());

		assertEquals("/fotos/vehiculo.jpg", resultado.getRuta());
		assertEquals("VEHICULO", resultado.getTipo());
		assertNotNull(resultado.getFecha());
	}

	@Test
	void evidenciasSePuedenAgregarEnOrdenEntregada() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.ENTREGADO)));

		EvidenciaResponse resultado = service.agregarEvidencia(OT_ID, EvidenciaRequest.builder()
				.ruta("/fotos/entrega.jpg").tipo(TipoEvidencia.DOCUMENTO).build());

		assertEquals("DOCUMENTO", resultado.getTipo());
		assertEquals("/fotos/entrega.jpg", resultado.getRuta());
	}

	@Test
	void listarHistorialDevuelveLosCambiosDeEstado() {
		when(ordenTrabajoRepository.findById(OT_ID)).thenReturn(Optional.of(orden(EstadoOt.LISTO)));
		when(historialEstadoRepository.findByOtIdOrderByIdAsc(OT_ID)).thenReturn(List.of(
				HistorialEstado.builder().id(1L).otId(OT_ID)
						.estadoAnterior(null).estadoNuevo(EstadoOt.PENDIENTE).build(),
				HistorialEstado.builder().id(2L).otId(OT_ID)
						.estadoAnterior(EstadoOt.PENDIENTE).estadoNuevo(EstadoOt.EN_PROCESO)
						.observacion("Iniciado").build()));

		assertEquals(2, service.listarHistorial(OT_ID).size());
		assertNull(service.listarHistorial(OT_ID).get(0).getEstadoAnterior());
		assertEquals("EN_PROCESO", service.listarHistorial(OT_ID).get(1).getEstadoNuevo());
	}

	private OrdenRequest requestCasoObligatorio() {
		return OrdenRequest.builder()
				.vehiculoId(1L)
				.kilometraje(50000)
				.observacionIngreso("Revisión general")
				.lineas(List.of(
						LineaRequest.builder().tipo(TipoLinea.OM)
								.descripcion("Mano de obra").costo(30000).build(),
						LineaRequest.builder().tipo(TipoLinea.OMEX)
								.descripcion("Trabajo externo").costo(14000).build(),
						LineaRequest.builder().tipo(TipoLinea.REPUESTO)
								.descripcion("Repuestos").costo(28000).build(),
						LineaRequest.builder().tipo(TipoLinea.INSUMO)
								.descripcion("Insumos").costo(10000).build()))
				.build();
	}

	private OrdenTrabajo orden(EstadoOt estado) {
		return OrdenTrabajo.builder()
				.id(OT_ID)
				.vehiculoId(1L)
				.creadoPorId(7L)
				.estado(estado)
				.kilometraje(1000)
				.observacionIngreso("Observación de ingreso")
				.neto(10000)
				.iva(1900)
				.porcentajeIva(new BigDecimal("19.00"))
				.total(11900)
				.build();
	}

	private LineaOt linea(Integer costo, Integer cobrado, BigDecimal factor) {
		return LineaOt.builder()
				.otId(OT_ID)
				.tipo(TipoLinea.REPUESTO)
				.descripcion("Repuesto")
				.costo(costo)
				.factor(factor)
				.cobrado(cobrado)
				.build();
	}

}

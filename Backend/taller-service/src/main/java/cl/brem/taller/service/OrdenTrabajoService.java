package cl.brem.taller.service;

import cl.brem.taller.dto.CambioEstadoRequest;
import cl.brem.taller.dto.EvidenciaRequest;
import cl.brem.taller.dto.EvidenciaResponse;
import cl.brem.taller.dto.HistorialResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Toda la lógica de negocio de órdenes de trabajo, líneas, evidencias e
 * historial. Los montos enviados por el cliente NUNCA se tienen en cuenta:
 * factores, cobrado, neto, IVA y total se calculan siempre aquí.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenTrabajoService {

	private static final BigDecimal FACTOR_SIN_RECARGO = new BigDecimal("1.00");

	private static final BigDecimal FACTOR_CON_RECARGO = new BigDecimal("1.30");

	private static final BigDecimal PORCENTAJE_IVA_DEFAULT = new BigDecimal("19.00");

	private static final BigDecimal CIEN = new BigDecimal("100");

	private static final Map<EstadoOt, Set<EstadoOt>> TRANSICIONES = Map.of(
			EstadoOt.PENDIENTE, Set.of(EstadoOt.EN_PROCESO, EstadoOt.ANULADA),
			EstadoOt.EN_PROCESO, Set.of(EstadoOt.LISTO, EstadoOt.ANULADA),
			EstadoOt.LISTO, Set.of(EstadoOt.ENTREGADO, EstadoOt.ANULADA),
			EstadoOt.ENTREGADO, Set.of(),
			EstadoOt.ANULADA, Set.of());

	private final OrdenTrabajoRepository ordenTrabajoRepository;

	private final LineaOtRepository lineaOtRepository;

	private final EvidenciaOtRepository evidenciaOtRepository;

	private final HistorialEstadoRepository historialEstadoRepository;

	private final VistaOrdenRepository vistaOrdenRepository;

	private final VehiculoRepository vehiculoRepository;

	private final ClienteRepository clienteRepository;

	public List<OrdenResponse> listar() {
		return ordenTrabajoRepository.findAllByOrderByIdDesc().stream()
				.map(this::aResponse)
				.toList();
	}

	public OrdenDetalleResponse buscarPorId(Long id) {
		return aDetalle(obtenerOrden(id));
	}

	/** Buscador: lee exclusivamente la vista vista_ordenes (solo lectura). */
	public List<VistaOrdenResponse> buscar(String q) {
		String termino = q == null ? "" : q.trim();
		return vistaOrdenRepository.buscar(termino).stream()
				.map(this::aVistaResponse)
				.toList();
	}

	@Transactional
	public OrdenDetalleResponse crear(OrdenRequest request, String creadoPorHeader) {
		Long creadoPorId = parsearCreadoPor(creadoPorHeader);
		validarVehiculoExiste(request.getVehiculoId());

		LocalDateTime ahora = LocalDateTime.now();
		BigDecimal porcentajeIva = request.getPorcentajeIva() != null
				? request.getPorcentajeIva()
				: PORCENTAJE_IVA_DEFAULT;

		OrdenTrabajo orden = OrdenTrabajo.builder()
				.vehiculoId(request.getVehiculoId())
				.creadoPorId(creadoPorId)
				.fechaIngreso(ahora)
				.fechaRegistro(ahora)
				.kilometraje(request.getKilometraje())
				.observacionIngreso(request.getObservacionIngreso())
				.estado(EstadoOt.PENDIENTE)
				.neto(0)
				.iva(0)
				.porcentajeIva(porcentajeIva)
				.total(0)
				.build();
		OrdenTrabajo guardada = ordenTrabajoRepository.save(orden);

		if (request.getLineas() != null) {
			for (LineaRequest linea : request.getLineas()) {
				lineaOtRepository.save(construirLinea(guardada.getId(), linea));
			}
		}
		recalcularMontos(guardada);
		return aDetalle(guardada);
	}

	@Transactional
	public OrdenResponse actualizarObservacion(Long id, ObservacionRequest request) {
		OrdenTrabajo orden = obtenerOrden(id);
		orden.setObservacionIngreso(request.getObservacionIngreso());
		return aResponse(ordenTrabajoRepository.save(orden));
	}

	@Transactional
	public OrdenDetalleResponse cambiarEstado(Long id, CambioEstadoRequest request) {
		OrdenTrabajo orden = obtenerOrden(id);
		EstadoOt anterior = orden.getEstado();
		EstadoOt nuevo = request.getEstado();

		Set<EstadoOt> permitidos = TRANSICIONES.get(anterior);
		if (permitidos == null || !permitidos.contains(nuevo)) {
			throw new IllegalArgumentException("Transición de estado no permitida: " + anterior + " -> " + nuevo);
		}

		String observacion = normalizarObservacion(request.getObservacionEntrega());
		if (nuevo == EstadoOt.ENTREGADO && observacion == null) {
			throw new IllegalArgumentException("La observación de entrega es obligatoria al pasar a ENTREGADO");
		}

		orden.setEstado(nuevo);
		if (nuevo == EstadoOt.ENTREGADO) {
			orden.setObservacionEntrega(observacion);
		}
		ordenTrabajoRepository.save(orden);

		historialEstadoRepository.save(HistorialEstado.builder()
				.otId(id)
				.estadoAnterior(anterior)
				.estadoNuevo(nuevo)
				.fechaCambio(LocalDateTime.now())
				.observacion(observacion)
				.build());

		return aDetalle(orden);
	}

	public List<LineaResponse> listarLineas(Long otId) {
		obtenerOrden(otId);
		return lineaOtRepository.findByOtIdOrderByIdAsc(otId).stream()
				.map(this::aLineaResponse)
				.toList();
	}

	@Transactional
	public LineaResponse agregarLinea(Long otId, LineaRequest request) {
		OrdenTrabajo orden = obtenerOrden(otId);
		validarLineasEditables(orden);
		LineaOt guardada = lineaOtRepository.save(construirLinea(otId, request));
		recalcularMontos(orden);
		return aLineaResponse(guardada);
	}

	@Transactional
	public LineaResponse actualizarLinea(Long otId, Long lineaId, LineaRequest request) {
		OrdenTrabajo orden = obtenerOrden(otId);
		validarLineasEditables(orden);
		LineaOt linea = obtenerLinea(otId, lineaId);

		BigDecimal factor = request.getFactor() != null
				? request.getFactor()
				: factorPorDefecto(request.getTipo());
		linea.setTipo(request.getTipo());
		linea.setDescripcion(request.getDescripcion());
		linea.setCosto(request.getCosto());
		linea.setFactor(factor);
		linea.setCobrado(calcularCobrado(request.getCosto(), factor));

		LineaOt guardada = lineaOtRepository.save(linea);
		recalcularMontos(orden);
		return aLineaResponse(guardada);
	}

	@Transactional
	public void eliminarLinea(Long otId, Long lineaId) {
		OrdenTrabajo orden = obtenerOrden(otId);
		validarLineasEditables(orden);
		LineaOt linea = obtenerLinea(otId, lineaId);
		lineaOtRepository.delete(linea);
		recalcularMontos(orden);
	}

	public List<EvidenciaResponse> listarEvidencias(Long otId) {
		obtenerOrden(otId);
		return evidenciaOtRepository.findByOtIdOrderByIdAsc(otId).stream()
				.map(this::aEvidenciaResponse)
				.toList();
	}

	@Transactional
	public EvidenciaResponse agregarEvidencia(Long otId, EvidenciaRequest request) {
		obtenerOrden(otId);
		TipoEvidencia tipo = request.getTipo() != null ? request.getTipo() : TipoEvidencia.VEHICULO;
		EvidenciaOt evidencia = EvidenciaOt.builder()
				.otId(otId)
				.tipo(tipo)
				.ruta(request.getRuta())
				.fecha(LocalDateTime.now())
				.build();
		return aEvidenciaResponse(evidenciaOtRepository.save(evidencia));
	}

	public List<HistorialResponse> listarHistorial(Long otId) {
		obtenerOrden(otId);
		return historialEstadoRepository.findByOtIdOrderByIdAsc(otId).stream()
				.map(this::aHistorialResponse)
				.toList();
	}

	private OrdenTrabajo obtenerOrden(Long id) {
		return ordenTrabajoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Orden de trabajo no encontrada con id: " + id));
	}

	private LineaOt obtenerLinea(Long otId, Long lineaId) {
		return lineaOtRepository.findById(lineaId)
				.filter(linea -> linea.getOtId().equals(otId))
				.orElseThrow(() -> new ResourceNotFoundException(
						"Línea no encontrada con id: " + lineaId + " en la OT: " + otId));
	}

	private void validarVehiculoExiste(Long vehiculoId) {
		if (!vehiculoRepository.existsById(vehiculoId)) {
			throw new ResourceNotFoundException("Vehículo no encontrado con id: " + vehiculoId);
		}
	}

	private void validarLineasEditables(OrdenTrabajo orden) {
		EstadoOt estado = orden.getEstado();
		if (estado == EstadoOt.ENTREGADO || estado == EstadoOt.ANULADA) {
			throw new IllegalArgumentException("No se pueden modificar líneas de una OT en estado " + estado);
		}
	}

	private Long parsearCreadoPor(String header) {
		if (header == null || header.isBlank()) {
			throw new AutenticacionRequeridaException("X-User-Id requerido");
		}
		try {
			return Long.valueOf(header.trim());
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("X-User-Id debe ser numérico");
		}
	}

	private BigDecimal factorPorDefecto(TipoLinea tipo) {
		return (tipo == TipoLinea.OMEX || tipo == TipoLinea.REPUESTO)
				? FACTOR_CON_RECARGO
				: FACTOR_SIN_RECARGO;
	}

	private int calcularCobrado(Integer costo, BigDecimal factor) {
		return BigDecimal.valueOf(costo)
				.multiply(factor)
				.setScale(0, RoundingMode.HALF_UP)
				.intValueExact();
	}

	/** Recalcula y guarda neto, IVA (HALF_UP) y total = neto + IVA. */
	private void recalcularMontos(OrdenTrabajo orden) {
		int neto = lineaOtRepository.findByOtIdOrderByIdAsc(orden.getId()).stream()
				.mapToInt(LineaOt::getCobrado)
				.sum();
		int iva = BigDecimal.valueOf(neto)
				.multiply(orden.getPorcentajeIva())
				.divide(CIEN, 0, RoundingMode.HALF_UP)
				.intValueExact();

		orden.setNeto(neto);
		orden.setIva(iva);
		orden.setTotal(neto + iva);
		ordenTrabajoRepository.save(orden);
	}

	private String normalizarObservacion(String observacion) {
		if (observacion == null || observacion.isBlank()) {
			return null;
		}
		return observacion.trim();
	}

	private LineaOt construirLinea(Long otId, LineaRequest request) {
		BigDecimal factor = request.getFactor() != null
				? request.getFactor()
				: factorPorDefecto(request.getTipo());
		return LineaOt.builder()
				.otId(otId)
				.tipo(request.getTipo())
				.descripcion(request.getDescripcion())
				.costo(request.getCosto())
				.factor(factor)
				.cobrado(calcularCobrado(request.getCosto(), factor))
				.build();
	}

	private OrdenDetalleResponse aDetalle(OrdenTrabajo orden) {
		Long id = orden.getId();
		return OrdenDetalleResponse.builder()
				.orden(aResponse(orden))
				.lineas(lineaOtRepository.findByOtIdOrderByIdAsc(id).stream()
						.map(this::aLineaResponse)
						.toList())
				.evidencias(evidenciaOtRepository.findByOtIdOrderByIdAsc(id).stream()
						.map(this::aEvidenciaResponse)
						.toList())
				.historial(historialEstadoRepository.findByOtIdOrderByIdAsc(id).stream()
						.map(this::aHistorialResponse)
						.toList())
				.build();
	}

	private OrdenResponse aResponse(OrdenTrabajo orden) {
		DatosVehiculo datos = datosVehiculo(orden.getVehiculoId());
		return OrdenResponse.builder()
				.id(orden.getId())
				.vehiculoId(orden.getVehiculoId())
				.patente(datos.patente())
				.nombreCliente(datos.nombreCliente())
				.creadoPorId(orden.getCreadoPorId())
				.fechaIngreso(orden.getFechaIngreso())
				.fechaRegistro(orden.getFechaRegistro())
				.kilometraje(orden.getKilometraje())
				.observacionIngreso(orden.getObservacionIngreso())
				.observacionEntrega(orden.getObservacionEntrega())
				.estado(nombre(orden.getEstado()))
				.neto(orden.getNeto())
				.iva(orden.getIva())
				.porcentajeIva(orden.getPorcentajeIva())
				.total(orden.getTotal())
				.build();
	}

	private DatosVehiculo datosVehiculo(Long vehiculoId) {
		return vehiculoRepository.findById(vehiculoId)
				.map(vehiculo -> new DatosVehiculo(
						vehiculo.getPatente(),
						clienteRepository.findById(vehiculo.getClienteId())
								.map(Cliente::getNombre)
								.orElse(null)))
				.orElseGet(() -> new DatosVehiculo(null, null));
	}

	private LineaResponse aLineaResponse(LineaOt linea) {
		return LineaResponse.builder()
				.id(linea.getId())
				.otId(linea.getOtId())
				.tipo(nombre(linea.getTipo()))
				.descripcion(linea.getDescripcion())
				.costo(linea.getCosto())
				.factor(linea.getFactor())
				.cobrado(linea.getCobrado())
				.build();
	}

	private EvidenciaResponse aEvidenciaResponse(EvidenciaOt evidencia) {
		return EvidenciaResponse.builder()
				.id(evidencia.getId())
				.otId(evidencia.getOtId())
				.tipo(nombre(evidencia.getTipo()))
				.ruta(evidencia.getRuta())
				.fecha(evidencia.getFecha())
				.build();
	}

	private HistorialResponse aHistorialResponse(HistorialEstado historial) {
		return HistorialResponse.builder()
				.id(historial.getId())
				.otId(historial.getOtId())
				.estadoAnterior(nombre(historial.getEstadoAnterior()))
				.estadoNuevo(nombre(historial.getEstadoNuevo()))
				.fechaCambio(historial.getFechaCambio())
				.observacion(historial.getObservacion())
				.build();
	}

	private VistaOrdenResponse aVistaResponse(VistaOrden vista) {
		return VistaOrdenResponse.builder()
				.otId(vista.getOtId())
				.patente(vista.getPatente())
				.nombreCliente(vista.getNombreCliente())
				.estado(vista.getEstado())
				.fechaIngreso(vista.getFechaIngreso())
				.total(vista.getTotal())
				.vehiculoId(vista.getVehiculoId())
				.build();
	}

	private String nombre(Enum<?> valor) {
		return valor == null ? null : valor.name();
	}

	private record DatosVehiculo(String patente, String nombreCliente) {
	}

}

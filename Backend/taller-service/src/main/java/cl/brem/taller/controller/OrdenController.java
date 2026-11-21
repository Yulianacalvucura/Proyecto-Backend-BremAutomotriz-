package cl.brem.taller.controller;

import cl.brem.taller.dto.CambioEstadoRequest;
import cl.brem.taller.dto.ObservacionRequest;
import cl.brem.taller.dto.OrdenDetalleResponse;
import cl.brem.taller.dto.OrdenRequest;
import cl.brem.taller.dto.OrdenResponse;
import cl.brem.taller.dto.VistaOrdenResponse;
import cl.brem.taller.service.OrdenTrabajoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordenes")
@RequiredArgsConstructor
@Tag(name = "Órdenes de trabajo", description = "Gestión de órdenes de trabajo del taller")
public class OrdenController {

	private final OrdenTrabajoService service;

	@Operation(summary = "Buscar órdenes por patente, cliente o número de OT (usa vista_ordenes)")
	@GetMapping("/buscar")
	public List<VistaOrdenResponse> buscar(@RequestParam(name = "q", required = false, defaultValue = "") String q) {
		return service.buscar(q);
	}

	@Operation(summary = "Listar todas las órdenes de trabajo")
	@GetMapping
	public List<OrdenResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Buscar una orden por id, con líneas, evidencias e historial")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Orden encontrada"),
			@ApiResponse(responseCode = "404", description = "Orden no encontrada")
	})
	@GetMapping("/{id}")
	public OrdenDetalleResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@Operation(summary = "Crear una orden de trabajo con sus líneas")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Orden creada"),
			@ApiResponse(responseCode = "400", description = "Datos inválidos"),
			@ApiResponse(responseCode = "401", description = "Falta X-User-Id"),
			@ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
	})
	@PostMapping
	public ResponseEntity<OrdenDetalleResponse> crear(
			@Valid @RequestBody OrdenRequest request,
			@RequestHeader(value = "X-User-Id", required = false) String creadoPorId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, creadoPorId));
	}

	@Operation(summary = "Actualizar solo la observación de ingreso de una orden")
	@PutMapping("/{id}")
	public OrdenResponse actualizarObservacion(@PathVariable Long id,
			@Valid @RequestBody ObservacionRequest request) {
		return service.actualizarObservacion(id, request);
	}

	@Operation(summary = "Cambiar el estado de una orden (registra el historial)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Estado cambiado"),
			@ApiResponse(responseCode = "400", description = "Transición inválida o falta observacionEntrega al pasar a ENTREGADO"),
			@ApiResponse(responseCode = "404", description = "Orden no encontrada")
	})
	@PatchMapping("/{id}/estado")
	public OrdenDetalleResponse cambiarEstado(@PathVariable Long id,
			@Valid @RequestBody CambioEstadoRequest request) {
		return service.cambiarEstado(id, request);
	}

}

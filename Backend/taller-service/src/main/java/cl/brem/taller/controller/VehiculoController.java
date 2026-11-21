package cl.brem.taller.controller;

import cl.brem.taller.dto.VehiculoDetalleResponse;
import cl.brem.taller.dto.VehiculoRequest;
import cl.brem.taller.dto.VehiculoResponse;
import cl.brem.taller.service.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehiculos")
@RequiredArgsConstructor
@Tag(name = "Vehículos", description = "Gestión de vehículos del taller")
public class VehiculoController {

	private final VehiculoService service;

	@Operation(summary = "Listar todos los vehículos")
	@GetMapping
	public List<VehiculoResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Buscar un vehículo por id")
	@GetMapping("/{id}")
	public VehiculoResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@Operation(summary = "Listar los vehículos de un cliente")
	@GetMapping("/cliente/{clienteId}")
	public List<VehiculoResponse> listarPorCliente(@PathVariable Long clienteId) {
		return service.listarPorCliente(clienteId);
	}

	@Operation(summary = "Buscar un vehículo por patente, con datos del cliente")
	@GetMapping("/patente/{patente}")
	public VehiculoDetalleResponse buscarPorPatente(@PathVariable String patente) {
		return service.buscarPorPatente(patente);
	}

	@Operation(summary = "Crear un vehículo")
	@PostMapping
	public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
	}

	@Operation(summary = "Actualizar un vehículo")
	@PutMapping("/{id}")
	public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest request) {
		return service.actualizar(id, request);
	}

}

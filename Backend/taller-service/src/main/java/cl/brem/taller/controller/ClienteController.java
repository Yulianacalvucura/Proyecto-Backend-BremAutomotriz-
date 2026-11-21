package cl.brem.taller.controller;

import cl.brem.taller.dto.ClienteRequest;
import cl.brem.taller.dto.ClienteResponse;
import cl.brem.taller.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de clientes del taller")
public class ClienteController {

	private final ClienteService service;

	@Operation(summary = "Listar todos los clientes")
	@GetMapping
	public List<ClienteResponse> listar() {
		return service.listar();
	}

	@Operation(summary = "Buscar un cliente por id")
	@GetMapping("/{id}")
	public ClienteResponse buscarPorId(@PathVariable Long id) {
		return service.buscarPorId(id);
	}

	@Operation(summary = "Crear un cliente")
	@PostMapping
	public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
	}

	@Operation(summary = "Actualizar un cliente")
	@PutMapping("/{id}")
	public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
		return service.actualizar(id, request);
	}

}

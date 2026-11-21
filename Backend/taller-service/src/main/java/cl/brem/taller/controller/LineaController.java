package cl.brem.taller.controller;

import cl.brem.taller.dto.LineaRequest;
import cl.brem.taller.dto.LineaResponse;
import cl.brem.taller.service.OrdenTrabajoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordenes/{ordenId}/lineas")
@RequiredArgsConstructor
@Tag(name = "Líneas de orden", description = "Trabajos y repuestos de una orden de trabajo")
public class LineaController {

	private final OrdenTrabajoService service;

	@Operation(summary = "Listar las líneas de una orden")
	@GetMapping
	public List<LineaResponse> listar(@PathVariable Long ordenId) {
		return service.listarLineas(ordenId);
	}

	@Operation(summary = "Agregar una línea a una orden")
	@PostMapping
	public ResponseEntity<LineaResponse> agregar(@PathVariable Long ordenId,
			@Valid @RequestBody LineaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarLinea(ordenId, request));
	}

	@Operation(summary = "Actualizar una línea de una orden")
	@PutMapping("/{lineaId}")
	public LineaResponse actualizar(@PathVariable Long ordenId, @PathVariable Long lineaId,
			@Valid @RequestBody LineaRequest request) {
		return service.actualizarLinea(ordenId, lineaId, request);
	}

	@Operation(summary = "Eliminar una línea de una orden")
	@DeleteMapping("/{lineaId}")
	public ResponseEntity<Void> eliminar(@PathVariable Long ordenId, @PathVariable Long lineaId) {
		service.eliminarLinea(ordenId, lineaId);
		return ResponseEntity.noContent().build();
	}

}

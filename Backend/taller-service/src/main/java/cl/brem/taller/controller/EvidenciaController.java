package cl.brem.taller.controller;

import cl.brem.taller.dto.EvidenciaRequest;
import cl.brem.taller.dto.EvidenciaResponse;
import cl.brem.taller.service.OrdenTrabajoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordenes/{ordenId}/evidencias")
@RequiredArgsConstructor
@Tag(name = "Evidencias de orden", description = "Rutas de fotos y documentos de una orden de trabajo")
public class EvidenciaController {

	private final OrdenTrabajoService service;

	@Operation(summary = "Listar las evidencias de una orden")
	@GetMapping
	public List<EvidenciaResponse> listar(@PathVariable Long ordenId) {
		return service.listarEvidencias(ordenId);
	}

	@Operation(summary = "Guardar la ruta de una evidencia (no el archivo)")
	@PostMapping
	public ResponseEntity<EvidenciaResponse> agregar(@PathVariable Long ordenId,
			@Valid @RequestBody EvidenciaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarEvidencia(ordenId, request));
	}

}

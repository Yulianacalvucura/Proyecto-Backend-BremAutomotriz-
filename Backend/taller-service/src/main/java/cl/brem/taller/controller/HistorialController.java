package cl.brem.taller.controller;

import cl.brem.taller.dto.HistorialResponse;
import cl.brem.taller.service.OrdenTrabajoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ordenes/{ordenId}/historial")
@RequiredArgsConstructor
@Tag(name = "Historial de órdenes", description = "Cambios de estado de una orden de trabajo")
public class HistorialController {

	private final OrdenTrabajoService service;

	@Operation(summary = "Listar el historial de cambios de estado de una orden")
	@GetMapping
	public List<HistorialResponse> listar(@PathVariable Long ordenId) {
		return service.listarHistorial(ordenId);
	}

}

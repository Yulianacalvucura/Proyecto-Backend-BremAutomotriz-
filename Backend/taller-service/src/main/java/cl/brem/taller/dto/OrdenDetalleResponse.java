package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Detalle completo de una OT: cabecera, líneas, evidencias e historial.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDetalleResponse {

	private OrdenResponse orden;

	private List<LineaResponse> lineas;

	private List<EvidenciaResponse> evidencias;

	private List<HistorialResponse> historial;

}

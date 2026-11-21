package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Resultado del buscador, leído desde la vista vista_ordenes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VistaOrdenResponse {

	private Long otId;

	private String patente;

	private String nombreCliente;

	private String estado;

	private LocalDateTime fechaIngreso;

	private Integer total;

	private Long vehiculoId;

}

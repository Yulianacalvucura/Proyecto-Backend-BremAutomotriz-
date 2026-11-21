package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vehículo junto con los datos del cliente, usado por la apertura de una OT
 * a partir de la patente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoDetalleResponse {

	private Long id;

	private String patente;

	private String marca;

	private String modelo;

	private Integer anio;

	private String vin;

	private String tipo;

	private Long clienteId;

	private String clienteNombre;

	private String clienteRun;

	private String clienteTelefono;

	private String clienteCorreo;

}

package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoResponse {

	private Long id;

	private String patente;

	private String marca;

	private String modelo;

	private Integer anio;

	private String vin;

	private String tipo;

	private Long clienteId;

}

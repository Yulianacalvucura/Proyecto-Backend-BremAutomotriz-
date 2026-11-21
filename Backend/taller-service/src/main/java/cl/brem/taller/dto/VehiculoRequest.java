package cl.brem.taller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class VehiculoRequest {

	@NotBlank
	private String patente;

	@NotBlank
	@Size(max = 50)
	private String marca;

	@NotBlank
	@Size(max = 50)
	private String modelo;

	@NotNull
	@Min(1950)
	@Max(2100)
	private Integer anio;

	@Size(min = 17, max = 17)
	private String vin;

	@Size(max = 30)
	private String tipo;

	@NotNull
	private Long clienteId;

}

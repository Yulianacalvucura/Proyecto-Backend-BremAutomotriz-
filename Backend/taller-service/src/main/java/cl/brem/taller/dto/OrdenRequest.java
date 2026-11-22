package cl.brem.taller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenRequest {

	@NotNull
	private Long vehiculoId;

	@NotNull
	@Min(0)
	@Max(1999999)
	private Integer kilometraje;

	@NotBlank
	@Size(max = 500)
	private String observacionIngreso;

	@DecimalMin("0.00")
	@DecimalMax("100.00")
	@Digits(integer = 3, fraction = 2)
	private BigDecimal porcentajeIva;

	private List<@Valid LineaRequest> lineas;

}

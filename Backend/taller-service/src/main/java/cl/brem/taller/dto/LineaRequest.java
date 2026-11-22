package cl.brem.taller.dto;

import cl.brem.taller.model.TipoLinea;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaRequest {

	@NotNull
	private TipoLinea tipo;

	@NotBlank
	@Size(max = 100)
	private String descripcion;

	@NotNull
	@Positive
	private Integer costo;
	@DecimalMin("1.00")
	@Digits(integer = 1, fraction = 2)
	private BigDecimal factor;

}

package cl.brem.taller.dto;

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
public class LineaResponse {

	private Long id;

	private Long otId;

	private String tipo;

	private String descripcion;

	private Integer costo;

	private BigDecimal factor;

	private Integer cobrado;

}

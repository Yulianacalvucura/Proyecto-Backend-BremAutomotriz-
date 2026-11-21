package cl.brem.taller.dto;

import cl.brem.taller.model.EstadoOt;
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
public class CambioEstadoRequest {

	@NotNull
	private EstadoOt estado;

	/** Obligatoria cuando el estado destino es ENTREGADO. */
	@Size(max = 500)
	private String observacionEntrega;

}

package cl.brem.taller.dto;

import cl.brem.taller.model.TipoEvidencia;
import jakarta.validation.constraints.NotBlank;
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
public class EvidenciaRequest {

	/** Solo se guarda la ruta o URL; el archivo no viaja en la petición. */
	@NotBlank
	@Size(max = 255)
	private String ruta;

	/** Opcional: si no viene se usa VEHICULO. */
	private TipoEvidencia tipo;

}

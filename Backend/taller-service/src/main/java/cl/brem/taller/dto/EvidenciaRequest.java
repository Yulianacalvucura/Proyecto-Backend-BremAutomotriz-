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

	@NotBlank
	@Size(max = 255)
	private String ruta;
	private TipoEvidencia tipo;

}

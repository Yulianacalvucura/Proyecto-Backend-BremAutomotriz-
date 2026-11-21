package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaResponse {

	private Long id;

	private Long otId;

	private String tipo;

	private String ruta;

	private LocalDateTime fecha;

}

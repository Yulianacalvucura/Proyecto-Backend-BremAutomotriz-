package cl.brem.taller.dto;

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
public class ClienteRequest {

	@NotBlank
	@Size(max = 100)
	private String nombre;

	@Size(max = 12)
	private String run;

	@Size(max = 20)
	private String telefono;

	@Size(max = 100)
	private String correo;

}

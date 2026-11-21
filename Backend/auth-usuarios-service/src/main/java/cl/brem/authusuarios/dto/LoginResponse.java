package cl.brem.authusuarios.dto;

import cl.brem.authusuarios.model.Rol;
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
public class LoginResponse {

	private String token;

	private String nombre;

	private String email;

	private Rol rol;

}

package cl.brem.authusuarios.dto;

import cl.brem.authusuarios.model.Rol;
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
public class UsuarioResponse {

	private Long id;

	private String nombre;

	private String email;

	private Rol rol;

	private Boolean activo;

	private LocalDateTime creadoEn;

}

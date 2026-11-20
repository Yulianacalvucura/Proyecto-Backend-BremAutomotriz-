package cl.brem.authusuarios.dto;

import cl.brem.authusuarios.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del login: token JWT y datos públicos del usuario")
public class LoginResponse {

    private String token;
    private String nombre;
    private String email;
    private Rol rol;
}

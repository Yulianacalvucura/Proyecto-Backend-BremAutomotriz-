package cl.brem.authusuarios.controller;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistrarRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Login, registro y gestión de usuarios (ADMIN)")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Devuelve el token JWT y los datos públicos del usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login correcto"),
            @ApiResponse(responseCode = "400", description = "Credenciales inválidas o datos mal formados")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Registrar usuario", description = "Solo ADMIN. Nunca devuelve el hash de la contraseña")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email repetido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de ADMIN")
    })
    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistrarRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @Operation(summary = "Listar usuarios", description = "Solo ADMIN")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios sin password_hash")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @Operation(summary = "Desactivar usuario", description = "Solo ADMIN. Los usuarios no se borran")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario desactivado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PatchMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(authService.cambiarActivo(id, false));
    }

    @Operation(summary = "Activar usuario", description = "Solo ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario activado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PatchMapping("/usuarios/{id}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(authService.cambiarActivo(id, true));
    }
}

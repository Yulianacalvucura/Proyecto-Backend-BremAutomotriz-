package cl.brem.authusuarios.controller;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistroRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y usuarios", description = "Login, registro y gestión de usuarios de Brem Automotriz")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión y obtener un JWT")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Login correcto, devuelve el token"),
			@ApiResponse(responseCode = "401", description = "Credenciales inválidas")
	})
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/registrar")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar un usuario (solo ADMIN)")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Usuario creado"),
			@ApiResponse(responseCode = "400", description = "Datos inválidos"),
			@ApiResponse(responseCode = "401", description = "Sin autenticación"),
			@ApiResponse(responseCode = "403", description = "Sin rol ADMIN"),
			@ApiResponse(responseCode = "409", description = "El email ya existe")
	})
	public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
		return authService.registrar(request);
	}

	@GetMapping("/usuarios")
	@Operation(summary = "Listar usuarios (solo ADMIN)")
	@ApiResponse(responseCode = "200", description = "Lista de usuarios")
	public List<UsuarioResponse> listar() {
		return authService.listar();
	}

	@PatchMapping("/usuarios/{id}/desactivar")
	@Operation(summary = "Desactivar un usuario (solo ADMIN)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Usuario desactivado"),
			@ApiResponse(responseCode = "400", description = "Intento de desactivar la propia cuenta"),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado")
	})
	public UsuarioResponse desactivar(@PathVariable Long id) {
		String emailQuienEjecuta = SecurityContextHolder.getContext().getAuthentication().getName();
		return authService.desactivar(id, emailQuienEjecuta);
	}

	@PatchMapping("/usuarios/{id}/activar")
	@Operation(summary = "Activar un usuario (solo ADMIN)")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Usuario activado"),
			@ApiResponse(responseCode = "404", description = "Usuario no encontrado")
	})
	public UsuarioResponse activar(@PathVariable Long id) {
		return authService.activar(id);
	}

}

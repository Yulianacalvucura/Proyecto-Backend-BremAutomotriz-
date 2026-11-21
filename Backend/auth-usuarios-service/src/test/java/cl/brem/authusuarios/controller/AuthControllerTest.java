package cl.brem.authusuarios.controller;

import cl.brem.authusuarios.config.SecurityConfig;
import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistroRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.exception.GlobalExceptionHandler;
import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Test
	void loginCorrectoDevuelve200ConTokenYSinDatosDeContrasena() throws Exception {
		// Arrange
		when(authService.login(any(LoginRequest.class))).thenReturn(LoginResponse.builder()
				.token("token-jwt-de-prueba")
				.nombre("Jefe de Taller")
				.email("jefe@brem.cl")
				.rol(Rol.JEFE)
				.build());
		String cuerpo = objectMapper.writeValueAsString(new LoginRequest("jefe@brem.cl", "jefe123"));

		// Act - Assert
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("token-jwt-de-prueba"))
				.andExpect(jsonPath("$.email").value("jefe@brem.cl"))
				.andExpect(content().string(not(containsString("passwordHash"))))
				.andExpect(content().string(not(containsString("password_hash"))));
	}

	@Test
	void loginConCredencialesInvalidasDevuelve401() throws Exception {
		// Arrange
		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new BadCredentialsException("Credenciales inválidas"));
		String cuerpo = objectMapper.writeValueAsString(new LoginRequest("jefe@brem.cl", "clave-mala"));

		// Act - Assert
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
	}

	@Test
	void registrarConRolJefeDevuelve403() throws Exception {
		// Arrange
		String cuerpo = objectMapper.writeValueAsString(RegistroRequest.builder()
				.nombre("Nuevo Usuario")
				.email("nuevo@brem.cl")
				.password("clave-segura-123")
				.rol(Rol.ADMIN)
				.build());

		// Act - Assert
		mockMvc.perform(post("/auth/registrar")
						.header("X-User-Username", "jefe@brem.cl")
						.header("X-User-Roles", "JEFE")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isForbidden());

		verify(authService, never()).registrar(any(RegistroRequest.class));
	}

	@Test
	void registrarSinHeadersDevuelve401() throws Exception {
		// Arrange
		String cuerpo = objectMapper.writeValueAsString(RegistroRequest.builder()
				.nombre("Nuevo Usuario")
				.email("nuevo@brem.cl")
				.password("clave-segura-123")
				.rol(Rol.ADMIN)
				.build());

		// Act - Assert
		mockMvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isUnauthorized());

		verify(authService, never()).registrar(any(RegistroRequest.class));
	}

	@Test
	void registrarConDatosInvalidosDevuelve400ConDetallePorCampo() throws Exception {
		// Arrange
		String cuerpo = objectMapper.writeValueAsString(RegistroRequest.builder()
				.nombre("")
				.email("no-es-un-email")
				.password("corto")
				.rol(null)
				.build());

		// Act - Assert
		mockMvc.perform(post("/auth/registrar")
						.header("X-User-Username", "admin1@brem.cl")
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensaje", containsString("nombre")))
				.andExpect(jsonPath("$.mensaje", containsString("email")))
				.andExpect(jsonPath("$.mensaje", containsString("password")))
				.andExpect(jsonPath("$.mensaje", containsString("rol")));
	}

	@Test
	void registrarValidoConRolAdminDevuelve201SinHash() throws Exception {
		// Arrange
		when(authService.registrar(any(RegistroRequest.class))).thenReturn(UsuarioResponse.builder()
				.id(10L)
				.nombre("Nuevo Usuario")
				.email("nuevo@brem.cl")
				.rol(Rol.JEFE)
				.activo(true)
				.creadoEn(LocalDateTime.of(2026, 11, 20, 12, 0))
				.build());
		String cuerpo = objectMapper.writeValueAsString(RegistroRequest.builder()
				.nombre("Nuevo Usuario")
				.email("nuevo@brem.cl")
				.password("clave-segura-123")
				.rol(Rol.JEFE)
				.build());

		// Act - Assert
		mockMvc.perform(post("/auth/registrar")
						.header("X-User-Username", "admin1@brem.cl")
						.header("X-User-Roles", "ADMIN")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.email").value("nuevo@brem.cl"))
				.andExpect(content().string(not(containsString("passwordHash"))))
				.andExpect(content().string(not(containsString("password_hash"))));
	}

}

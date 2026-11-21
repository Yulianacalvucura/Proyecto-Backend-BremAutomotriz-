package cl.brem.authusuarios.service;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistroRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.exception.ConflictException;
import cl.brem.authusuarios.exception.ResourceNotFoundException;
import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.model.Usuario;
import cl.brem.authusuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String MENSAJE_CREDENCIALES = "Credenciales inválidas";

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private JwtService jwtService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private AuthService authService;

	@BeforeEach
	void configurar() {
		authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
	}

	@Test
	void loginConCredencialesValidasDevuelveTokenYDatosDelUsuario() {
		// Arrange
		Usuario jefe = Usuario.builder()
				.id(3L)
				.nombre("Jefe de Taller")
				.email("jefe@brem.cl")
				.passwordHash(passwordEncoder.encode("jefe123"))
				.rol(Rol.JEFE)
				.activo(true)
				.build();
		when(usuarioRepository.findByEmail("jefe@brem.cl")).thenReturn(Optional.of(jefe));
		when(jwtService.generarToken(jefe)).thenReturn("token-falso-de-prueba");

		// Act
		LoginResponse respuesta = authService.login(new LoginRequest("jefe@brem.cl", "jefe123"));

		// Assert
		assertEquals("token-falso-de-prueba", respuesta.getToken());
		assertEquals("Jefe de Taller", respuesta.getNombre());
		assertEquals("jefe@brem.cl", respuesta.getEmail());
		assertEquals(Rol.JEFE, respuesta.getRol());
	}

	@Test
	void loginConEmailInexistenteLanzaBadCredentialsConMensajeGenerico() {
		// Arrange
		when(usuarioRepository.findByEmail("noexiste@brem.cl")).thenReturn(Optional.empty());

		// Act
		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> authService.login(new LoginRequest("noexiste@brem.cl", "jefe123")));

		// Assert
		assertEquals(MENSAJE_CREDENCIALES, ex.getMessage());
	}

	@Test
	void loginConContrasenaIncorrectaLanzaBadCredentialsConMensajeGenerico() {
		// Arrange
		Usuario usuario = Usuario.builder()
				.id(1L)
				.nombre("Administrador Uno")
				.email("admin1@brem.cl")
				.passwordHash(passwordEncoder.encode("admin123"))
				.rol(Rol.ADMIN)
				.activo(true)
				.build();
		when(usuarioRepository.findByEmail("admin1@brem.cl")).thenReturn(Optional.of(usuario));

		// Act
		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> authService.login(new LoginRequest("admin1@brem.cl", "clave-erronea")));

		// Assert
		assertEquals(MENSAJE_CREDENCIALES, ex.getMessage());
	}

	@Test
	void loginConUsuarioInactivoLanzaBadCredentialsConMensajeGenerico() {
		// Arrange
		Usuario inactivo = Usuario.builder()
				.id(2L)
				.nombre("Administrador Dos")
				.email("admin2@brem.cl")
				.passwordHash(passwordEncoder.encode("admin123"))
				.rol(Rol.ADMIN)
				.activo(false)
				.build();
		when(usuarioRepository.findByEmail("admin2@brem.cl")).thenReturn(Optional.of(inactivo));

		// Act
		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> authService.login(new LoginRequest("admin2@brem.cl", "admin123")));

		// Assert
		assertEquals(MENSAJE_CREDENCIALES, ex.getMessage());
	}

	@Test
	void registrarGuardaHashBCryptYNoLaContrasenaEnTextoPlano() {
		// Arrange
		RegistroRequest request = RegistroRequest.builder()
				.nombre("Nuevo Admin")
				.email("nuevo@brem.cl")
				.password("clave-segura-123")
				.rol(Rol.ADMIN)
				.build();
		when(usuarioRepository.existsByEmail("nuevo@brem.cl")).thenReturn(false);
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		UsuarioResponse respuesta = authService.registrar(request);

		// Assert
		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).save(captor.capture());
		Usuario guardado = captor.getValue();

		assertNotNull(guardado.getPasswordHash());
		assertNotEquals("clave-segura-123", guardado.getPasswordHash());
		assertTrue(passwordEncoder.matches("clave-segura-123", guardado.getPasswordHash()));
		assertTrue(guardado.getActivo());
		assertEquals("nuevo@brem.cl", respuesta.getEmail());
		assertEquals(Rol.ADMIN, respuesta.getRol());
	}

	@Test
	void registrarConEmailDuplicadoLanzaConflictException() {
		// Arrange
		RegistroRequest request = RegistroRequest.builder()
				.nombre("Duplicado")
				.email("admin1@brem.cl")
				.password("clave-segura-123")
				.rol(Rol.ADMIN)
				.build();
		when(usuarioRepository.existsByEmail("admin1@brem.cl")).thenReturn(true);

		// Act
		ConflictException ex = assertThrows(ConflictException.class, () -> authService.registrar(request));

		// Assert
		assertEquals("El email ya está registrado", ex.getMessage());
		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void desactivarAOtroUsuarioLoMarcaInactivo() {
		// Arrange
		Usuario otro = Usuario.builder()
				.id(9L)
				.nombre("Otro Usuario")
				.email("otro@brem.cl")
				.passwordHash(passwordEncoder.encode("clave-segura-123"))
				.rol(Rol.JEFE)
				.activo(true)
				.build();
		when(usuarioRepository.findById(9L)).thenReturn(Optional.of(otro));
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		UsuarioResponse respuesta = authService.desactivar(9L, "admin1@brem.cl");

		// Assert
		assertFalse(respuesta.getActivo());
		verify(usuarioRepository).save(otro);
	}

	@Test
	void desactivarLaPropiaCuentaLanzaIllegalArgumentException() {
		// Arrange
		Usuario yo = Usuario.builder()
				.id(1L)
				.email("admin1@brem.cl")
				.rol(Rol.ADMIN)
				.activo(true)
				.build();
		when(usuarioRepository.findById(1L)).thenReturn(Optional.of(yo));

		// Act
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> authService.desactivar(1L, "admin1@brem.cl"));

		// Assert
		assertEquals("No puedes desactivar tu propia cuenta", ex.getMessage());
		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void activarConIdInexistenteLanzaResourceNotFound() {
		// Arrange
		when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

		// Act
		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> authService.activar(404L));

		// Assert
		assertEquals("No existe el usuario con id 404", ex.getMessage());
	}

	@Test
	void desactivarConIdInexistenteLanzaResourceNotFound() {
		// Arrange
		when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

		// Act
		ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
				() -> authService.desactivar(404L, "admin1@brem.cl"));

		// Assert
		assertEquals("No existe el usuario con id 404", ex.getMessage());
	}

}

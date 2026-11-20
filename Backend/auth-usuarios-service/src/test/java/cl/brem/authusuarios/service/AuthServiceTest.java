package cl.brem.authusuarios.service;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistrarRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.exception.ResourceNotFoundException;
import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.model.Usuario;
import cl.brem.authusuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registrar_whenEmailNuevo_shouldGuardarHashYRolSinDevolverlo() {
        // Arrange
        RegistrarRequest request = new RegistrarRequest("Jefe Nuevo", "nuevo@brem.cl", "clave123", "jefe");
        when(usuarioRepository.existsByEmail("nuevo@brem.cl")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("$2b$hashGenerado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario guardado = invocation.getArgument(0);
            guardado.setId(4L);
            return guardado;
        });

        // Act
        UsuarioResponse resultado = authService.registrar(request);

        // Assert
        assertEquals(4L, resultado.getId());
        assertEquals("Jefe Nuevo", resultado.getNombre());
        assertEquals("nuevo@brem.cl", resultado.getEmail());
        assertEquals(Rol.JEFE, resultado.getRol());
        assertTrue(resultado.getActivo());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("$2b$hashGenerado", captor.getValue().getPasswordHash());
        assertEquals(Rol.JEFE, captor.getValue().getRol());
        assertTrue(captor.getValue().getActivo());
    }

    @Test
    void registrar_whenEmailRepetido_shouldLanzarIllegalArgumentException() {
        // Arrange
        RegistrarRequest request = new RegistrarRequest("Otro", "admin1@brem.cl", "clave123", "ADMIN");
        when(usuarioRepository.existsByEmail("admin1@brem.cl")).thenReturn(true);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registrar(request));
        assertTrue(ex.getMessage().contains("ya está registrado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_whenRolInvalido_shouldLanzarIllegalArgumentException() {
        // Arrange
        RegistrarRequest request = new RegistrarRequest("Otro", "otro@brem.cl", "clave123", "VENDEDOR");
        when(usuarioRepository.existsByEmail("otro@brem.cl")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("$2b$hashGenerado");

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.registrar(request));
        assertTrue(ex.getMessage().contains("Rol inválido"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void login_whenCredencialesValidas_shouldDevolverTokenYDatos() {
        // Arrange
        Usuario usuario = new Usuario(3L, "Jefe de Taller", "jefe@brem.cl", "$2b$hash", Rol.JEFE, true, null);
        LoginRequest request = new LoginRequest("jefe@brem.cl", "jefe123");
        when(usuarioRepository.findByEmail("jefe@brem.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("jefe123", "$2b$hash")).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token123");

        // Act
        LoginResponse resultado = authService.login(request);

        // Assert
        assertEquals("token123", resultado.getToken());
        assertEquals("Jefe de Taller", resultado.getNombre());
        assertEquals("jefe@brem.cl", resultado.getEmail());
        assertEquals(Rol.JEFE, resultado.getRol());
        verify(jwtService).generarToken(usuario);
    }

    @Test
    void login_whenPasswordIncorrecta_shouldLanzarIllegalArgumentException() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Admin", "admin1@brem.cl", "$2b$hash", Rol.ADMIN, true, null);
        when(usuarioRepository.findByEmail("admin1@brem.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mal", "$2b$hash")).thenReturn(false);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("admin1@brem.cl", "mal")));
        assertTrue(ex.getMessage().contains("Credenciales inválidas"));
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    void login_whenEmailNoExiste_shouldLanzarIllegalArgumentException() {
        // Arrange
        when(usuarioRepository.findByEmail("ghost@brem.cl")).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("ghost@brem.cl", "clave123")));
        assertTrue(ex.getMessage().contains("Credenciales inválidas"));
    }

    @Test
    void login_whenUsuarioInactivo_shouldLanzarIllegalArgumentException() {
        // Arrange
        Usuario inactivo = new Usuario(2L, "Inactivo", "inactivo@brem.cl", "$2b$hash", Rol.JEFE, false, null);
        when(usuarioRepository.findByEmail("inactivo@brem.cl")).thenReturn(Optional.of(inactivo));

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("inactivo@brem.cl", "clave123")));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void listarUsuarios_shouldMapearSinPasswordHash() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Administrador Uno", "admin1@brem.cl", "$2b$hash", Rol.ADMIN, true, null);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        // Act
        List<UsuarioResponse> resultado = authService.listarUsuarios();

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("admin1@brem.cl", resultado.get(0).getEmail());
        assertEquals(Rol.ADMIN, resultado.get(0).getRol());
    }

    @Test
    void cambiarActivo_whenExiste_shouldActualizarEstado() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Admin", "admin1@brem.cl", "$2b$hash", Rol.ADMIN, true, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UsuarioResponse resultado = authService.cambiarActivo(1L, false);

        // Assert
        assertFalse(resultado.getActivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarActivo_whenNoExiste_shouldLanzarResourceNotFound() {
        // Arrange
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> authService.cambiarActivo(99L, true));
        assertTrue(ex.getMessage().contains("99"));
        verify(usuarioRepository, never()).save(any());
    }
}

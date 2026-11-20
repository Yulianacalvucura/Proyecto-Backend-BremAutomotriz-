package cl.brem.authusuarios.controller;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistrarRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.exception.ResourceNotFoundException;
import cl.brem.authusuarios.model.Rol;
import cl.brem.authusuarios.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_whenCredencialesValidas_shouldReturn200ConToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("admin1@brem.cl", "admin123");
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("token123", "Administrador Uno", "admin1@brem.cl", Rol.ADMIN));

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.email").value("admin1@brem.cl"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_whenCredencialesInvalidas_shouldReturn400() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Credenciales inválidas"));

        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin1@brem.cl", "mal"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void login_whenBodyInvalido_shouldReturn400ConDetallePorCampo() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("email")))
                .andExpect(jsonPath("$.mensaje", containsString("password")));
    }

    @Test
    void registrar_shouldReturn200SinPasswordHash() throws Exception {
        // Arrange
        RegistrarRequest request = new RegistrarRequest("Jefe Nuevo", "nuevo@brem.cl", "clave123", "JEFE");
        when(authService.registrar(any(RegistrarRequest.class))).thenReturn(
                new UsuarioResponse(4L, "Jefe Nuevo", "nuevo@brem.cl", Rol.JEFE, true, null));

        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.rol").value("JEFE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist());
    }

    @Test
    void registrar_whenRolInvalido_shouldReturn400() throws Exception {
        // Arrange
        when(authService.registrar(any(RegistrarRequest.class)))
                .thenThrow(new IllegalArgumentException("Rol inválido: VENDEDOR"));

        // Act + Assert
        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrarRequest("Otro", "otro@brem.cl", "clave123", "VENDEDOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("Rol inválido")));
    }

    @Test
    void listarUsuarios_shouldReturn200ConLista() throws Exception {
        // Arrange
        when(authService.listarUsuarios()).thenReturn(List.of(
                new UsuarioResponse(1L, "Administrador Uno", "admin1@brem.cl", Rol.ADMIN, true, null)));

        // Act + Assert
        mockMvc.perform(get("/auth/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("admin1@brem.cl"));
    }

    @Test
    void desactivar_shouldReturn200ConActivoFalse() throws Exception {
        // Arrange
        when(authService.cambiarActivo(1L, false)).thenReturn(
                new UsuarioResponse(1L, "Admin", "admin1@brem.cl", Rol.ADMIN, false, null));

        // Act + Assert
        mockMvc.perform(patch("/auth/usuarios/1/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        verify(authService).cambiarActivo(1L, false);
    }

    @Test
    void activar_whenNoExiste_shouldReturn404() throws Exception {
        // Arrange
        when(authService.cambiarActivo(eq(99L), eq(true)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con id: 99"));

        // Act + Assert
        mockMvc.perform(patch("/auth/usuarios/99/activar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con id: 99"));
    }
}

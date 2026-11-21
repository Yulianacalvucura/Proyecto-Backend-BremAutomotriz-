package cl.brem.authusuarios.service;

import cl.brem.authusuarios.dto.LoginRequest;
import cl.brem.authusuarios.dto.LoginResponse;
import cl.brem.authusuarios.dto.RegistroRequest;
import cl.brem.authusuarios.dto.UsuarioResponse;
import cl.brem.authusuarios.exception.ConflictException;
import cl.brem.authusuarios.exception.ResourceNotFoundException;
import cl.brem.authusuarios.model.Usuario;
import cl.brem.authusuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

	private final UsuarioRepository usuarioRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	public LoginResponse login(LoginRequest request) {
		Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadCredentialsException(CREDENCIALES_INVALIDAS));

		boolean contrasenaValida = passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash());
		if (!contrasenaValida || Boolean.FALSE.equals(usuario.getActivo())) {
			throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
		}

		return LoginResponse.builder()
				.token(jwtService.generarToken(usuario))
				.nombre(usuario.getNombre())
				.email(usuario.getEmail())
				.rol(usuario.getRol())
				.build();
	}

	@Transactional
	public UsuarioResponse registrar(RegistroRequest request) {
		if (usuarioRepository.existsByEmail(request.getEmail())) {
			throw new ConflictException("El email ya está registrado");
		}

		Usuario usuario = Usuario.builder()
				.nombre(request.getNombre())
				.email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.rol(request.getRol())
				.activo(true)
				.build();

		return mapear(usuarioRepository.save(usuario));
	}

	public List<UsuarioResponse> listar() {
		return usuarioRepository.findAll().stream()
				.map(this::mapear)
				.toList();
	}

	@Transactional
	public UsuarioResponse desactivar(Long id, String emailQuienEjecuta) {
		Usuario usuario = buscar(id);

		if (usuario.getEmail().equalsIgnoreCase(emailQuienEjecuta)) {
			throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
		}

		usuario.setActivo(false);
		return mapear(usuarioRepository.save(usuario));
	}

	@Transactional
	public UsuarioResponse activar(Long id) {
		Usuario usuario = buscar(id);
		usuario.setActivo(true);
		return mapear(usuarioRepository.save(usuario));
	}

	private Usuario buscar(Long id) {
		return usuarioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));
	}

	private UsuarioResponse mapear(Usuario usuario) {
		return UsuarioResponse.builder()
				.id(usuario.getId())
				.nombre(usuario.getNombre())
				.email(usuario.getEmail())
				.rol(usuario.getRol())
				.activo(usuario.getActivo())
				.creadoEn(usuario.getCreadoEn())
				.build();
	}

}

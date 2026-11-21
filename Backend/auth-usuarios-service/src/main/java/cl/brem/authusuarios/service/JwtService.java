package cl.brem.authusuarios.service;

import cl.brem.authusuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

	private static final int LONGITUD_MINIMA_SECRETO = 32;

	private final SecretKey key;

	private final long expiration;

	public JwtService(@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration}") long expiration) {
		if (secret == null || secret.length() < LONGITUD_MINIMA_SECRETO) {
			throw new IllegalStateException("jwt.secret debe tener al menos " + LONGITUD_MINIMA_SECRETO
					+ " caracteres (configura JWT_SECRET con un valor seguro)");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiration = expiration;
	}

	public String generarToken(Usuario usuario) {
		Date ahora = new Date();
		Date expira = new Date(ahora.getTime() + expiration);

		return Jwts.builder()
				.subject(usuario.getEmail())
				.claim("roles", List.of(usuario.getRol().name()))
				.claim("userId", usuario.getId())
				.claim("nombre", usuario.getNombre())
				.issuedAt(ahora)
				.expiration(expira)
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public Claims parsear(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}

package cl.brem.authusuarios.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptHashVerificationTest {

	private static final String HASH_ADMIN = "$2b$10$r4///wsszdNx3YcaA1QULOsAuwTLdlOuCWnZoyRbjXmrWmRhiswwO";

	private static final String HASH_JEFE = "$2b$10$n3rnoBjbdA27gZ81EJp9mOLo4eDT.bCo2KUy6UHmJrlSOSBTVadX.";

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Test
	void hashDelAdministradorDelSqlCorrespondeAAdmin123() {
		assertTrue(encoder.matches("admin123", HASH_ADMIN),
				"El hash del SQL NO coincide con admin123: hay que regenerarlo");
	}

	@Test
	void hashDelJefeDelSqlCorrespondeAJefe123() {
		assertTrue(encoder.matches("jefe123", HASH_JEFE),
				"El hash del SQL NO coincide con jefe123: hay que regenerarlo");
	}

}

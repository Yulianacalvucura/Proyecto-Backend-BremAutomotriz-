package cl.brem.authusuarios.repository;

import cl.brem.authusuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El email es único en la tabla usuarios; el nombre NO lo es.
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}

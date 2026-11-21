package cl.brem.taller.repository;

import cl.brem.taller.model.VistaOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de SOLO LECTURA de la vista vista_ordenes, usado únicamente
 * por el buscador de órdenes (patente, cliente o número de OT).
 */
public interface VistaOrdenRepository extends JpaRepository<VistaOrden, Long> {

	@Query("select v from VistaOrden v "
			+ "where lower(v.patente) like lower(concat('%', :q, '%')) "
			+ "or lower(v.nombreCliente) like lower(concat('%', :q, '%')) "
			+ "or cast(v.otId as string) like concat('%', :q, '%')")
	List<VistaOrden> buscar(@Param("q") String q);

}

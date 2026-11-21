package cl.brem.taller.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Proyección de la vista vista_ordenes. Es de SOLO LECTURA: nunca se escribe.
 */
@Entity
@Immutable
@Table(name = "vista_ordenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VistaOrden {

	@Id
	@Column(name = "ot_id")
	private Long otId;

	@Column(name = "patente")
	private String patente;

	@Column(name = "nombre_cliente")
	private String nombreCliente;

	@Column(name = "estado")
	private String estado;

	@Column(name = "fecha_ingreso")
	private LocalDateTime fechaIngreso;

	@Column(name = "total")
	private Integer total;

	@Column(name = "vehiculo_id")
	private Long vehiculoId;

}

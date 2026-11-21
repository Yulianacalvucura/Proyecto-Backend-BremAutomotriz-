package cl.brem.taller.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "patente", nullable = false, length = 6)
	private String patente;

	@Column(name = "marca", nullable = false, length = 50)
	private String marca;

	@Column(name = "modelo", nullable = false, length = 50)
	private String modelo;

	@Column(name = "anio", nullable = false)
	private Integer anio;

	@Column(name = "vin", length = 17)
	private String vin;

	@Column(name = "tipo", length = 30)
	private String tipo;

	@Column(name = "cliente_id", nullable = false)
	private Long clienteId;

}

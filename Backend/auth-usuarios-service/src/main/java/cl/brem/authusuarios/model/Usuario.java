package cl.brem.authusuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(max = 100)
	@Column(nullable = false)
	private String nombre;

	@NotBlank
	@Email
	@Size(max = 100)
	@Column(nullable = false, unique = true)
	private String email;

	@NotBlank
	@Size(max = 255)
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rol rol;

	@Builder.Default
	@Column(nullable = false)
	private Boolean activo = true;

	@Column(name = "creado_en")
	private LocalDateTime creadoEn;

	@PrePersist
	void alPersistir() {
		if (this.creadoEn == null) {
			this.creadoEn = LocalDateTime.now();
		}
		if (this.activo == null) {
			this.activo = true;
		}
	}

}

package cl.brem.authusuarios;

import org.springframework.boot.SpringApplication;

public class TestAuthUsuariosServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AuthUsuariosServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package cl.brem.taller;

import org.springframework.boot.SpringApplication;

public class TestTallerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TallerServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

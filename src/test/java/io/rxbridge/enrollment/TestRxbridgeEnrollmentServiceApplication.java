package io.rxbridge.enrollment;

import org.springframework.boot.SpringApplication;

public class TestRxbridgeEnrollmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(RxbridgeEnrollmentServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

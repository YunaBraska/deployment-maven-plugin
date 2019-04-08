package com.ratepay.albatross.worker;

import org.springframework.boot.SpringApplication;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication()
public class Application {

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(final String[] args) {
        SpringApplication.run(com.ratepay.albatross.worker.WorkerApplication.class, args);
    }
}

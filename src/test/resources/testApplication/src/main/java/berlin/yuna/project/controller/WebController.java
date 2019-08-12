package berlin.yuna.project.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
}

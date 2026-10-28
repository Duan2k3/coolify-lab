package dev.lab.coolify.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    private final String instanceName;

    public HelloController(@Value("${app.instance-name:local}") String instanceName) {
        this.instanceName = instanceName;
    }

    @GetMapping
    public Map<String, Object> hello() {
        return Map.of(
            "message", "Hello from the Coolify lab",
            "instance", instanceName,
            "timestamp", Instant.now().toString()
        );
    }
}

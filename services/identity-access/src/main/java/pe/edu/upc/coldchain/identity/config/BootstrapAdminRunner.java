package pe.edu.upc.coldchain.identity.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pe.edu.upc.coldchain.identity.service.IdentityService;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    private final IdentityProperties properties;
    private final IdentityService service;

    public BootstrapAdminRunner(IdentityProperties properties, IdentityService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = properties.bootstrapAdmin();
        service.ensureBootstrapUser(admin.username(), admin.password(), admin.fullName());
    }
}

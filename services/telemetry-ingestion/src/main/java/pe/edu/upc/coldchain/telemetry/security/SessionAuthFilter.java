package pe.edu.upc.coldchain.telemetry.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "coldchain.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SessionAuthFilter extends OncePerRequestFilter {
    public static final String AUTH_CONTEXT_ATTR = "coldchain.auth.context";
    private final AuthProperties properties;
    private final IdentityClient identityClient;

    public SessionAuthFilter(AuthProperties properties, IdentityClient identityClient) {
        this.properties = properties;
        this.identityClient = identityClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.enabled() || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "Se requiere autenticación Bearer");
            return;
        }
        IdentityClient.IntrospectionResult result = identityClient.introspect(authorization);
        if (result.status() == 401) {
            writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "Sesión inválida, revocada o expirada");
            return;
        }
        if (result.status() == 403) {
            writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "No tiene permisos para acceder a este recurso");
            return;
        }
        if (result.status() != 200 || result.context() == null) {
            writeProblem(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Servicio de identidad no disponible");
            return;
        }
        request.setAttribute(AUTH_CONTEXT_ATTR, result.context());
        chain.doFilter(request, response);
    }

    private void writeProblem(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}

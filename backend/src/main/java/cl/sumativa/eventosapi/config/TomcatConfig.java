package cl.sumativa.eventosapi.config;

import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Oculta la versión de Tomcat y el detalle técnico en las páginas de error propias de Tomcat
 * (A05 Configuración de seguridad incorrecta: divulgación de información).
 */
@Configuration
public class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value("${app.seguridad.endurecida:true}")
    private boolean endurecida;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        if (!endurecida) {
            return;
        }
        factory.addContextCustomizers(context -> {
            ErrorReportValve valve = new ErrorReportValve();
            valve.setShowReport(false);
            valve.setShowServerInfo(false);
            context.getParent().getPipeline().addValve(valve);
        });
    }
}

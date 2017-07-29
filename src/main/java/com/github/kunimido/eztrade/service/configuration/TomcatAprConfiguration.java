package com.github.kunimido.eztrade.service.configuration;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TomcatAprProperties.class)
@Slf4j
public class TomcatAprConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "server.tomcat.apr", name = "enabled")
    public EmbeddedServletContainerFactory embeddedServletContainerFactory(final TomcatAprProperties tomcatAprProperties) {
        log.debug("Configuring Tomcat APR connector");
        final TomcatEmbeddedServletContainerFactory result = new TomcatEmbeddedServletContainerFactory();
        result.setProtocol("org.apache.coyote.http11.Http11AprProtocol");

        final AprLifecycleListener aprLifecycleListener = new AprLifecycleListener();
        aprLifecycleListener.setUseAprConnector(true);
        result.addContextLifecycleListeners(aprLifecycleListener);

        result.addConnectorCustomizers(connector -> {
            final Http11AprProtocol handler = (Http11AprProtocol) connector.getProtocolHandler();
            if (tomcatAprProperties.isHttp2()) {
                log.debug("Enabling HTTP/2 support");
                handler.addUpgradeProtocol(new Http2Protocol());
            }

            if (tomcatAprProperties.getSsl().isEnabled()) {
                log.debug("Enabling SSL support");
                handler.setSSLEnabled(true);

                log.debug("Using SSL certificate key file {}", tomcatAprProperties.getSsl().getCertificateKeyFile());
                handler.setSSLCertificateKeyFile(tomcatAprProperties.getSsl().getCertificateKeyFile());

                log.debug("Using SSL certificate file {}", tomcatAprProperties.getSsl().getCertificateFile());
                handler.setSSLCertificateFile(tomcatAprProperties.getSsl().getCertificateFile());

                log.debug("Using SSL certificate chain file {}", tomcatAprProperties.getSsl().getCertificateChainFile());
                handler.setSSLCertificateChainFile(tomcatAprProperties.getSsl().getCertificateChainFile());

                connector.setScheme("https");
                connector.setSecure(true);
            }
        });

        return result;
    }
}
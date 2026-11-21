package com.proyecto.servicios.config;

import feign.Client;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuracion SSL/TLS y X-API-Key exclusiva para los clientes Feign de GestoPago.
 * Sin @Configuration para evitar aplicarse globalmente a otros clientes Feign.
 */
@Slf4j
public class GestoPagoFeignConfig {

    @Bean
    public RequestInterceptor gestoPagoApiKeyInterceptor(
            @Value("${gestopago.auth.api-key:}") String apiKey) {
        return template -> {
            if (apiKey != null && !apiKey.isBlank()) {
                template.header("X-API-Key", apiKey.trim());
            }
        };
    }

    @Bean
    public Client gestoPagoFeignClient(
            ResourceLoader resourceLoader,
            @Value("${gestopago.ssl.enabled:true}") boolean sslEnabled,
            @Value("${gestopago.ssl.trust-store:classpath:certs/gestopago-truststore.p12}") String trustStorePath,
            @Value("${gestopago.ssl.trust-store-password:changeit}") String trustStorePassword,
            @Value("${gestopago.ssl.trust-store-type:PKCS12}") String trustStoreType,
            @Value("${gestopago.ssl.protocols:TLSv1.3,TLSv1.2}") String tlsProtocols) {
        if (!sslEnabled) {
            return new Client.Default(null, null);
        }
        try {
            SSLContext sslContext = crearSslContext(resourceLoader, trustStorePath, trustStorePassword, trustStoreType);
            String[] protocolos = parsearProtocolos(tlsProtocols);
            SSLSocketFactory socketFactory = new TlsProtocolSocketFactory(sslContext.getSocketFactory(), protocolos);
            log.info("Cliente Feign GestoPago inicializado con SSL/TrustStore ({}) y protocolos TLS={}",
                    trustStorePath, Arrays.toString(protocolos));
            return new Client.Default(socketFactory, HttpsURLConnection.getDefaultHostnameVerifier());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar el contexto SSL/TLS para GestoPago", ex);
        }
    }

    static SSLContext crearSslContext(
            ResourceLoader resourceLoader,
            String trustStorePath,
            String trustStorePassword,
            String trustStoreType) throws Exception {
        List<X509TrustManager> managers = new ArrayList<>();

        if (trustStorePath != null && !trustStorePath.isBlank()) {
            Resource resource = resourceLoader.getResource(trustStorePath.trim());
            if (resource.exists()) {
                KeyStore customTrustStore = KeyStore.getInstance(
                        trustStoreType != null && !trustStoreType.isBlank() ? trustStoreType.trim() : "PKCS12");
                char[] passChars = trustStorePassword != null ? trustStorePassword.toCharArray() : new char[0];
                try (InputStream is = resource.getInputStream()) {
                    customTrustStore.load(is, passChars);
                }
                TrustManagerFactory customTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                customTmf.init(customTrustStore);
                agregarX509Managers(customTmf.getTrustManagers(), managers);
            }
        }

        // Combinar tambien con el TrustStore por defecto de la JVM (cacerts)
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null);
        agregarX509Managers(defaultTmf.getTrustManagers(), managers);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new CompositeX509TrustManager(managers)}, new SecureRandom());
        return sslContext;
    }

    private static void agregarX509Managers(TrustManager[] origen, List<X509TrustManager> destino) {
        if (origen == null) {
            return;
        }
        for (TrustManager tm : origen) {
            if (tm instanceof X509TrustManager x509) {
                destino.add(x509);
            }
        }
    }

    static String[] parsearProtocolos(String raw) {
        if (raw == null || raw.isBlank()) {
            return new String[]{"TLSv1.3", "TLSv1.2"};
        }
        String[] result = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        return result.length > 0 ? result : new String[]{"TLSv1.3", "TLSv1.2"};
    }

    static final class CompositeX509TrustManager implements X509TrustManager {
        private final List<X509TrustManager> trustManagers;

        CompositeX509TrustManager(List<X509TrustManager> trustManagers) {
            this.trustManagers = trustManagers;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager tm : trustManagers) {
                try {
                    tm.checkClientTrusted(chain, authType);
                    return;
                } catch (CertificateException ex) {
                    last = ex;
                }
            }
            throw last != null ? last : new CertificateException("No hay TrustManager valido para cliente");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager tm : trustManagers) {
                try {
                    tm.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException ex) {
                    last = ex;
                }
            }
            throw last != null ? last : new CertificateException("Certificado del servidor no confiado en TrustStore");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            Set<X509Certificate> issuers = new LinkedHashSet<>();
            for (X509TrustManager tm : trustManagers) {
                issuers.addAll(Arrays.asList(tm.getAcceptedIssuers()));
            }
            return issuers.toArray(new X509Certificate[0]);
        }
    }

    static final class TlsProtocolSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        private final String[] preferredProtocols;

        TlsProtocolSocketFactory(SSLSocketFactory delegate, String[] preferredProtocols) {
            this.delegate = delegate;
            this.preferredProtocols = preferredProtocols;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return habilitarTls(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return habilitarTls(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return habilitarTls(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return habilitarTls(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return habilitarTls(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket habilitarTls(Socket socket) {
            if (socket instanceof SSLSocket sslSocket) {
                Set<String> soportados = new LinkedHashSet<>(Arrays.asList(sslSocket.getSupportedProtocols()));
                String[] activos = Arrays.stream(preferredProtocols)
                        .filter(soportados::contains)
                        .toArray(String[]::new);
                if (activos.length > 0) {
                    sslSocket.setEnabledProtocols(activos);
                }
            }
            return socket;
        }
    }
}

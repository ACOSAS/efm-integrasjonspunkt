package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.Reject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
@Slf4j
public class SvarInnClient {

    @Getter
    private final RestTemplate restTemplate;
    @Getter
    private final String rootUri;
    private final Plumber plumber;
    private final IntegrasjonspunktProperties props;

    public SvarInnClient(IntegrasjonspunktProperties props, RestTemplateBuilder restTemplateBuilder, Plumber plumber) {
        this.plumber = plumber;
        this.props = props;
        this.rootUri = props.getFiks().getInn().getBaseUrl();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(props.getFiks().getInn().getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(props.getFiks().getInn().getReadTimeout()))
                .errorHandler(new DefaultResponseErrorHandler())
                .rootUri(props.getFiks().getInn().getBaseUrl())
                .build();
    }

    private HttpHeaders authHeadersForOrgnr(String orgnr) {
        if (props.getFiks().getInn().getPaaVegneAv().containsKey(orgnr)) {
            FiksConfig.FiksCredentials fiksAuth = props.getFiks().getInn().getPaaVegneAv().get(orgnr);
            return createHeaders(fiksAuth.getUsername(), fiksAuth.getPassword());
        }

        if (orgnr.equals(props.getFiks().getInn().getOrgnr()) && !isNullOrEmpty(props.getFiks().getInn().getUsername())) {
            return createHeaders(props.getFiks().getInn().getUsername(),
                    props.getFiks().getInn().getPassword());
        }

        throw new IllegalArgumentException("FIKS SvarInn authentication not provided for orgnr " + orgnr);
    }

    private HttpHeaders createHeaders(String username, String password) {
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encode = Base64.getEncoder().encode(auth.getBytes(UTF_8));
            String authHeader = "Basic " + new String(encode);
            set("Authorization", authHeader);
        }};
    }

    List<Forsendelse> checkForNewMessages(String orgnr) {
        Forsendelse[] body = restTemplate.exchange("/mottaker/hentNyeForsendelser",
                HttpMethod.GET,
                new HttpEntity<>(authHeadersForOrgnr(orgnr)),
                Forsendelse[].class).getBody();
        return Arrays.asList(Objects.requireNonNull(body));
    }

    InputStream downloadZipFile(Forsendelse forsendelse, Reject reject) {
        return plumber.pipe("downloading zip file", inlet ->
                restTemplate.execute(forsendelse.getDownloadUrl(),
                        HttpMethod.GET,
                        request -> {
                            if (props.getFiks().getInn().getPaaVegneAv().containsKey(forsendelse.getMottaker().getOrgnr())) {
                                FiksConfig.FiksCredentials fiksCredentials = props.getFiks().getInn().getPaaVegneAv().get(forsendelse.getMottaker().getOrgnr());
                                request.getHeaders().setBasicAuth(fiksCredentials.getUsername(), fiksCredentials.getPassword(), UTF_8);
                            } else if (forsendelse.getMottaker().getOrgnr().equals(props.getFiks().getInn().getOrgnr()) &&
                                    !isNullOrEmpty(props.getFiks().getInn().getUsername())) {
                                request.getHeaders().setBasicAuth(props.getFiks().getInn().getUsername(),
                                        props.getFiks().getInn().getPassword(),
                                        UTF_8);
                            } else {
                                throw new IllegalArgumentException("FIKS SvarInn authentication not provided for orgnr " + forsendelse.getMottaker().getOrgnr());
                            }
                        }, response -> {
                            InputStream body = new AutoCloseInputStream(response.getBody());
                            int bytes = IOUtils.copy(body, inlet);
                            log.info("File for forsendelse {} was downloaded ({} bytes)", forsendelse.getId(), bytes);
                            return null;
                        }), reject
        ).outlet();
    }

    void confirmMessage(Forsendelse forsendelse) {
        restTemplate.exchange("/kvitterMottak/forsendelse/{forsendelseId}",
                HttpMethod.POST,
                new HttpEntity<>(authHeadersForOrgnr(forsendelse.getMottaker().getOrgnr())),
                Void.class,
                forsendelse.getId());
    }

    void setErrorStateForMessage(Forsendelse forsendelse, String errorMsg) {
        ErrorResponse errorResponse = new ErrorResponse(errorMsg, true);
        restTemplate.exchange("/mottakFeilet/forsendelse/{forsendelseId}",
                HttpMethod.POST,
                new HttpEntity<>(errorResponse, authHeadersForOrgnr(forsendelse.getMottaker().getOrgnr())),
                Void.class,
                forsendelse.getId());
    }
}

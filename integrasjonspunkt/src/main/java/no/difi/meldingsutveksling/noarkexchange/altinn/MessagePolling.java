package no.difi.meldingsutveksling.noarkexchange.altinn;


import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.elma.ELMALookup;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.lookup.api.LookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 * MessagePolling periodically checks Altinn Formidlingstjeneste for new messages. If new messages are discovered they
 * are downloaded forwarded to the Archive system.
 */
@Component
public class MessagePolling {
    public static final String PREFIX_NORWAY = "9908:";
    Logger logger = LoggerFactory.getLogger(MessagePolling.class);

    @Autowired
    IntegrasjonspunktConfiguration config;

    @Autowired
    IntegrajonspunktReceiveImpl integrajonspunktReceive;

    @Autowired
    ELMALookup elmaLookup;

    private static JAXBContext jaxbContextdomain;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class, Kvittering.class);
            jaxbContextdomain = JAXBContext.newInstance(Document.class, Payload.class, Kvittering.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private static JAXBContext jaxbContext;

    @Scheduled(fixedRate = 15000)
    public void checkForNewMessages() {
        logger.debug("Checking for new messages");
        MDC.put(IntegrasjonspunktConfiguration.KEY_ORGANISATION_NUMBER, config.getOrganisationNumber());
        Endpoint endpoint;
        try {
            endpoint = elmaLookup.lookup(PREFIX_NORWAY + config.getOrganisationNumber());
        } catch (LookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(endpoint.getAddress(), config.getConfiguration());
        AltinnWsClient client = new AltinnWsClient(configuration);

        List<FileReference> fileReferences = client.availableFiles(config.getOrganisationNumber());

        if(!fileReferences.isEmpty()) {
            Audit.info("Found new messages! Proceeding with download...");
        }

        for (FileReference reference : fileReferences) {
            Audit.info("Downloading message", markerFrom(reference));
            Document document = client.download(new DownloadRequest(reference.getValue(), config.getOrganisationNumber()));
            forwardToNoark(document);
        }
    }

    private void forwardToNoark(Document document) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JAXBElement<Document> d = new ObjectFactory().createStandardBusinessDocument(document);

            jaxbContextdomain.createMarshaller().marshal(d, os);
            byte[] tmp = os.toByteArray();

            JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument> toDocument
                    = (JAXBElement<no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument>)
                    jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(tmp));

            final StandardBusinessDocument standardBusinessDocument = toDocument.getValue();
            Audit.info("Successfully extracted standard business document. Forwarding document to NOARK system...", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
            try {
                integrajonspunktReceive.forwardToNoarkSystem(standardBusinessDocument);
            } catch (MessageException e) {
                Audit.error("Could not forward document to NOARK system...", markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)));
                logger.error(markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)), e.getStatusMessage().getTechnicalMessage(), e);
            }
        } catch (JAXBException e) {
            Audit.error("Could not forward document to NOARK system... due to a technical error");
            throw new MeldingsUtvekslingRuntimeException("Could not forward document to archive system", e);
        }
    }
}


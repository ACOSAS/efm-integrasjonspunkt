package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.KvitteringStatusMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class BestEduErrorAppReceiptService {

    private final IntegrasjonspunktProperties properties;
    private final SBDFactory createSBD;
    private final NextMoveMessageService nextMoveMessageService;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final NoarkClient noarkClient;
    private final UUIDGenerator uuidGenerator;

    public BestEduErrorAppReceiptService(
            IntegrasjonspunktProperties properties,
            SBDFactory createSBD,
            @Lazy NextMoveMessageService nextMoveMessageService,
            PutMessageRequestFactory putMessageRequestFactory,
            @Qualifier("localNoark") ObjectProvider<NoarkClient> noarkClient,
            UUIDGenerator uuidGenerator) {
        this.properties = properties;
        this.createSBD = createSBD;
        this.nextMoveMessageService = nextMoveMessageService;
        this.putMessageRequestFactory = putMessageRequestFactory;
        this.noarkClient = noarkClient.getIfAvailable();
        this.uuidGenerator = uuidGenerator;
    }

    void sendBestEduErrorAppReceipt(NextMoveOutMessage message, String errorText) {
        AppReceiptType appReceipt = AppReceiptFactory.from("ERROR", "Unknown", errorText);
        PutMessageRequestType putMessage = putMessageRequestFactory.create(message.getSbd(), BestEduConverter.appReceiptAsString(appReceipt));
        noarkClient.sendEduMelding(putMessage);
    }

    void sendBestEduErrorAppReceipt(StandardBusinessDocument sbd) {
        String errorText = String.format("Feilet under mottak hos %s - ble ikke avlevert sakarkivsystem", sbd.getReceiverIdentifier());
        ArkivmeldingKvitteringMessage kvittering = new ArkivmeldingKvitteringMessage()
                .setReceiptType("ERROR")
                .addMessage(new KvitteringStatusMessage("Unknown", errorText));

        StandardBusinessDocument receiptSbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(sbd.getReceiverIdentifier()),
                Organisasjonsnummer.from(sbd.getSenderIdentifier()),
                sbd.getConversationId(),
                uuidGenerator.generate(),
                properties.getArkivmelding().getReceiptProcess(),
                DocumentType.ARKIVMELDING_KVITTERING,
                kvittering);

        NextMoveOutMessage message = nextMoveMessageService.createMessage(receiptSbd);
        nextMoveMessageService.sendMessage(message);
    }
}

package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PutMessageRequestFactory {

    private final ServiceRegistryLookup srLookup;


    public PutMessageRequestType create(StandardBusinessDocument sbd, Object payload) {
        String receiverRef = sbd.findScope(ScopeType.RECEIVER_REF).map(Scope::getIdentifier).orElse(null);
        String senderRef = sbd.findScope(ScopeType.SENDER_REF).map(Scope::getIdentifier).orElse(null);
        InfoRecord receiverInfo = srLookup.getInfoRecord(sbd.getReceiverIdentifier());
        InfoRecord senderInfo = srLookup.getInfoRecord(sbd.getSenderIdentifier());
        return create(sbd.getConversationId(),
                Sender.of(sbd.getSenderIdentifier(), senderInfo.getOrganizationName(), senderRef),
                Receiver.of(sbd.getReceiverIdentifier(), receiverInfo.getOrganizationName(), receiverRef),
                payload);
    }

    public PutMessageRequestType create(String conversationId,
                                         Sender sender,
                                         Receiver receiver,
                                         Object payload) {

        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(receiver.getIdentifier());
        receiverAddressType.setName(receiver.getName());
        receiverAddressType.setRef(receiver.getRef());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(sender.getIdentifier());
        senderAddressType.setName(sender.getName());
        senderAddressType.setRef(sender.getRef());

        EnvelopeType envelopeType = of.createEnvelopeType();
        envelopeType.setConversationId(conversationId);
        envelopeType.setContentNamespace("http://www.arkivverket.no/Noark4-1-WS-WD/types");
        envelopeType.setReceiver(receiverAddressType);
        envelopeType.setSender(senderAddressType);

        PutMessageRequestType putMessageRequestType = of.createPutMessageRequestType();
        putMessageRequestType.setEnvelope(envelopeType);
        putMessageRequestType.setPayload(payload);

        return putMessageRequestType;
    }
}

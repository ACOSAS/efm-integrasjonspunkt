package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.OffsetDateTime;

@Entity
@DiscriminatorValue("in")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class NextMoveInMessage extends NextMoveMessage {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime lockTimeout;

    public NextMoveInMessage(String conversationId,
                             String messageId,
                             String processIdentifier,
                             String receiverIdentifier,
                             String senderIdentifier,
                             ServiceIdentifier serviceIdentifier,
                             StandardBusinessDocument sbd) {
        super(conversationId, messageId, processIdentifier, receiverIdentifier, senderIdentifier, serviceIdentifier, sbd);
    }

    public static NextMoveInMessage of(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        return new NextMoveInMessage(
                SBDUtil.getConversationId(sbd),
                SBDUtil.getMessageId(sbd),
                SBDUtil.getProcess(sbd),
                SBDUtil.getReceiver(sbd).getPrimaryIdentifier(),
                SBDUtil.getSender(sbd).getPrimaryIdentifier(),
                serviceIdentifier,
                sbd);
    }

    @Override
    public ConversationDirection getDirection() {
        return ConversationDirection.INCOMING;
    }
}

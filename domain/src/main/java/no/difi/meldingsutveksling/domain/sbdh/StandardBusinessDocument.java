//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.25 at 12:23:12 PM CET 
//


package no.difi.meldingsutveksling.domain.sbdh;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageDeserializer;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageSerializer;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.w3c.dom.Node;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.Optional;


/**
 * <p>Java class for StandardBusinessDocument complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="StandardBusinessDocument">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader}StandardBusinessDocumentHeader" minOccurs="0"/>
 *         &lt;any processContents='lax' namespace='##other'/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StandardBusinessDocument", propOrder = {
        "standardBusinessDocumentHeader",
        "any"
})
@Data
@Entity
@Table(name = "document")
@JsonSerialize(using = NextMoveMessageSerializer.class)
public class StandardBusinessDocument {

    @Id
    @GeneratedValue
    @JsonIgnore
    @XmlTransient
    private Long id;

    @XmlElement(name = "StandardBusinessDocumentHeader")
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @NotNull
    @Valid
    private StandardBusinessDocumentHeader standardBusinessDocumentHeader;

    @XmlAnyElement(lax = true)
    @Transient // TODO should not be transient in the end
    @JsonDeserialize(using = NextMoveMessageDeserializer.class)
    @JsonAlias({"dpo", "dpv"})
    protected Object any;

    @JsonIgnore
    public MessageInfo getMessageInfo() {
        return new MessageInfo(getReceiverOrgNumber(), getSenderOrgNumber(), getJournalPostId(), getConversationId(), getMessageType());
    }

    @JsonIgnore
    public String getSenderOrgNumber() {
        return getStandardBusinessDocumentHeader().getSender().iterator().next().getIdentifier().getValue().split(":")[1];
    }

    @JsonIgnore
    public String getReceiverOrgNumber() {
        return getStandardBusinessDocumentHeader().getReceiver().iterator().next().getIdentifier().getValue().split(":")[1];
    }

    @JsonIgnore
    public final String getJournalPostId() {
        return findScope(ScopeType.JOURNALPOST_ID).orElseThrow(MeldingsUtvekslingRuntimeException::new).getInstanceIdentifier();
    }

    @JsonIgnore
    public String getConversationId() {
        return findScope(ScopeType.CONVERSATION_ID).orElseThrow(MeldingsUtvekslingRuntimeException::new).getInstanceIdentifier();
    }

    @JsonIgnore
    public Optional<Scope> getConversationScope() {
        return findScope(ScopeType.CONVERSATION_ID);
    }

    private Optional<Scope> findScope(ScopeType scopeType) {
        final List<Scope> scopes = getStandardBusinessDocumentHeader().getBusinessScope().getScope();
        for (Scope scope : scopes) {
            if (scopeType.toString().equals(scope.getType())) {
                return Optional.of(scope);
            }
        }
        return Optional.empty();
    }

    @JsonIgnore
    public String getMessageType() {
        return getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
    }

    @JsonIgnore
    public String getDocumentId() {
        return getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier();
    }

    @JsonIgnore
    public boolean isReceipt() {
        return getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    @JsonIgnore
    public boolean isNextMove() {
        return StandardBusinessDocumentHeader.NEXTMOVE_TYPE.equalsIgnoreCase(getStandardBusinessDocumentHeader().getDocumentIdentification().getType());
    }

    @JsonIgnore
    public Payload getPayload() {
        if (getAny() instanceof Payload) {
            return (Payload) getAny();
        } else if (getAny() instanceof Node) {
            return unmarshallAnyElement(getAny());
        } else {
            throw new MeldingsUtvekslingRuntimeException("Could not cast any element " + getAny() + " from " + StandardBusinessDocument.class + " to " + Payload.class);
        }
    }

    private Payload unmarshallAnyElement(Object any) {
        JAXBContext jaxbContextP;
        Unmarshaller unMarshallerP;
        Payload payload;
        try {
            jaxbContextP = JAXBContextFactory.createContext(new Class[]{Payload.class}, null);
            unMarshallerP = jaxbContextP.createUnmarshaller();
            payload = unMarshallerP.unmarshal((org.w3c.dom.Node) any, Payload.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return payload;
    }

    public LogstashMarker createLogstashMarkers() {
        return getMessageInfo().createLogstashMarkers();
    }
}

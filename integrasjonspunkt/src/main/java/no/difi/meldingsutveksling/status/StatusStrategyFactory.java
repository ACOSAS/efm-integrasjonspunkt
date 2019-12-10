package no.difi.meldingsutveksling.status;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.status.strategy.NoOperationStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class StatusStrategyFactory {

    private Map<ServiceIdentifier, StatusStrategy> conversationStrategies = new EnumMap<>(ServiceIdentifier.class);

    public StatusStrategyFactory(ObjectProvider<StatusStrategy> statusStrategies) {
        statusStrategies.orderedStream().forEach(s -> {
            conversationStrategies.putIfAbsent(s.getServiceIdentifier(), s);
        });
    }

    StatusStrategy getFactory(Conversation conversation) {
        return conversationStrategies.getOrDefault(conversation.getServiceIdentifier(), new NoOperationStrategy());
    }
}

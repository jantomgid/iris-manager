package id.global.iris.manager.queue.utils;

import static id.global.common.iris.constants.MessagingHeaders.RequeueMessage.X_RETRY_COUNT;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.rabbitmq.client.AMQP;

import id.global.iris.manager.queue.operations.OperationId;

@ApplicationScoped
public class RoutingMessageHeaderModifier {

    public AMQP.BasicProperties modifyHeaders(AMQP.BasicProperties properties, OperationId operationId,
            String countHeaderName) {

        Map<String, Object> headers = getOrCreateHeaders(properties);
        if (countHeaderName != null) {
            headers.compute(countHeaderName, (k, v) -> v == null ? 1 : Integer.sum(((Integer) v), 1));
        }
        headers.put(OperationId.HEADER_NAME, operationId.value());
        // always reset retry count when moving/requeuing a message
        headers.put(X_RETRY_COUNT, 0);
        return properties.builder().headers(headers).build();
    }

    private Map<String, Object> getOrCreateHeaders(AMQP.BasicProperties properties) {
        if (properties.getHeaders() == null || properties.getHeaders().isEmpty()) {
            return new HashMap<>();
        } else {
            return copyHeader(properties.getHeaders());
        }
    }

    private Map<String, Object> copyHeader(Map<String, Object> headers) {
        return new HashMap<>(headers);
    }

}

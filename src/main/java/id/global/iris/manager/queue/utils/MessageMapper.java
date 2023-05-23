package id.global.iris.manager.queue.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;

import id.global.iris.manager.queue.model.BasicProperties;
import id.global.iris.manager.queue.model.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MessageMapper {

    private final MessageChecksum messageChecksum;

    @Inject
    public MessageMapper(MessageChecksum messageChecksum) {
        this.messageChecksum = messageChecksum;
    }

    public Message map(GetResponse response) {
        String checksum = messageChecksum.createFor(response.getProps(), response.getBody());
        return new Message(response.getEnvelope(), mapProperties(response.getProps()), response.getBody(), checksum);
    }

    private BasicProperties mapProperties(AMQP.BasicProperties input) {
        BasicProperties result = new BasicProperties();
        result.setContentType(input.getContentType());
        result.setContentEncoding(input.getContentEncoding());
        mapHeaders(input, result);
        result.setDeliveryMode(input.getDeliveryMode());
        result.setPriority(input.getPriority());
        result.setCorrelationId(input.getCorrelationId());
        result.setReplyTo(input.getReplyTo());
        result.setExpiration(input.getExpiration());
        result.setMessageId(input.getMessageId());
        result.setTimestamp(input.getTimestamp());
        result.setType(input.getType());
        result.setUserId(input.getUserId());
        result.setAppId(input.getAppId());
        result.setClusterId(input.getClusterId());
        return result;
    }

    private void mapHeaders(AMQP.BasicProperties input, BasicProperties result) {
        if (input.getHeaders() == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : input.getHeaders().entrySet()) {
            Object value = entry.getValue();
            value = mapRabbitMqSpecificType(value);
            result.addHeader(entry.getKey(), value);
        }
    }

    private Object mapRabbitMqSpecificType(Object value) {
        if (value instanceof LongString) {
            return value.toString();
        } else if (value instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            Set<Map.Entry> entrySet = ((Map) value).entrySet();
            for (Map.Entry e : entrySet) {
                result.put(e.getKey().toString(), mapRabbitMqSpecificType(e.getValue()));
            }
            return result;
        } else if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object o : (List) value) {
                result.add(mapRabbitMqSpecificType(o));
            }
            return result;
        }
        return value;
    }
}

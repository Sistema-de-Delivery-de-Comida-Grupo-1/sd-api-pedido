package br.ifg.urutai.sdapipedido.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitMQConfig {
    @Value("${app.queue-name}")
    private String queueName;
    @Bean
    public Queue minhaFila() {
        return new Queue(queueName, true); // fila durável
    }
}

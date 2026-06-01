package br.ifg.urutai.sdapipedido.config;

import ifg.urutai.sdapipagamento.grpc.ServicoPagamentoGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    public ManagedChannel pagamentoChannel() {
        return ManagedChannelBuilder
                .forAddress("localhost", 5689)
                .usePlaintext()
                .build();
    }

    @Bean
    public ServicoPagamentoGrpc.ServicoPagamentoBlockingStub pagamentoStub(
            ManagedChannel pagamentoChannel) {

        return ServicoPagamentoGrpc.newBlockingStub(
                pagamentoChannel
        );
    }
}
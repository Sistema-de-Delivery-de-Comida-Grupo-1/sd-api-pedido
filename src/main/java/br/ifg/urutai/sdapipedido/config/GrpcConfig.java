package br.ifg.urutai.sdapipedido.config;

import ifg.urutai.sdapipagamento.grpc.ServicoPagamentoGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    public ManagedChannel pagamentoChannel(
            DiscoveryClient discoveryClient) {

        ServiceInstance instance = discoveryClient.getInstances("SD-API-PAGAMENTO").get(0);
        System.out.println(instance.getHost() + ":" + instance.getPort());

        return ManagedChannelBuilder
                .forAddress(
                        instance.getHost(),
                        8086
                )
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
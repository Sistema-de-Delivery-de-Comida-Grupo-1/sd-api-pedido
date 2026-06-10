package br.ifg.urutai.sdapipedido.service;

import ifg.urutai.sdapipagamento.grpc.ServicoPagamentoGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PagamentoGrpcClient {

    private final DiscoveryClient discoveryClient;

    public PagamentoGrpcClient(
            DiscoveryClient discoveryClient) {

        this.discoveryClient = discoveryClient;
    }

    public ServicoPagamentoGrpc.ServicoPagamentoBlockingStub getStub() {

        List<ServiceInstance> instances =
                discoveryClient.getInstances("sd-api-pagamento");

        if (instances.isEmpty()) {
            throw new RuntimeException(
                    "Serviço de pagamento indisponível");
        }

        ServiceInstance instance = instances.get(0);

        ManagedChannel channel =
                ManagedChannelBuilder
                        .forAddress(
                                instance.getHost(),
                                8086
                        )
                        .usePlaintext()
                        .build();

        return ServicoPagamentoGrpc
                .newBlockingStub(channel);
    }
}
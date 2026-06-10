package br.ifg.urutai.sdapipedido.service;

import br.ifg.urutai.sdapipedido.dto.UsuarioResumeDTO;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UsuarioClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    public UsuarioClient(
            RestClient.Builder builder,
            DiscoveryClient discoveryClient) {

        this.restClient = builder.build();
        this.discoveryClient = discoveryClient;
    }

    public UsuarioResumeDTO buscarUsuario(Long id) {

        List<ServiceInstance> instances = discoveryClient.getInstances("MICROSSERVICO-USUARIOS");

        if (instances.isEmpty()) {
            throw new RuntimeException(
                    "MICROSSERVICO-USUARIOS não encontrado no Eureka");
        }

        ServiceInstance instance = instances.getFirst();

        String url = String.format(
                "http://%s:%d/usuarios/%d/resumo",
                instance.getHost(),
                instance.getPort(),
                id
        );

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(UsuarioResumeDTO.class);
    }
}
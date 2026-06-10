package br.ifg.urutai.sdapipedido.service;
import br.ifg.urutai.sdapipedido.dto.*;
import br.ifg.urutai.sdapipedido.model.ItemPedido;
import br.ifg.urutai.sdapipedido.model.Pedido;
import br.ifg.urutai.sdapipedido.model.StatusPedido;
import br.ifg.urutai.sdapipedido.repository.PedidoRepository;
import br.ifg.urutai.sdapipedido.util.DataMapper;
import ifg.urutai.sdapipagamento.grpc.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PedidoService {

    private final ServicoPagamentoGrpc.ServicoPagamentoBlockingStub stubPagamento;
    private final UsuarioClient usuarioClient;
    private final RabbitTemplate rabbitTemplate;
    private final StreamBridge streamBridge;
    private final String queueName;
    private final PedidoRepository pedidoRepository;

    @Autowired
    public PedidoService(ServicoPagamentoGrpc.ServicoPagamentoBlockingStub stubPagamento, UsuarioClient usuarioCliient, RabbitTemplate rabbitTemplate, StreamBridge streamBridge, @Value("${app.queue-name}") String queueName, PedidoRepository pedidoRepository) {
        this.stubPagamento = stubPagamento;
        this.usuarioClient = usuarioCliient;
        this.rabbitTemplate = rabbitTemplate;
        this.streamBridge = streamBridge;
        this.queueName = queueName;
        this.pedidoRepository = pedidoRepository;
    }

    public PedidoResponseDTO criarPedido(PedidoCreteDTO pedidoDTO) {

        Pedido pedido = DataMapper.parseObject(pedidoDTO, Pedido.class);
        pedido.setId(null);

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido deve possuir ao menos um item");
        }

        try {

            UsuarioResumeDTO usuario = usuarioClient.buscarUsuario(pedidoDTO.getIdCliente());

        } catch (HttpServerErrorException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cliente não encontrado"
            );

        }

        double valorTotal = pedido.getItens()
                .stream()
                .mapToDouble(item -> item.getPreco() * item.getQuantidade())
                .sum();

        pedido.setValorTotal(valorTotal);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        pedido.getItens().forEach(item -> item.setPedido(pedido));

        PedidoResponseDTO pedidoSalvo = DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
        streamBridge.send("sd-api-pedido", pedidoSalvo);
        return pedidoSalvo;
    }

    public PedidoResponseDTO atualizar(Long id, PedidoCreteDTO pedidoDTO) {

        Pedido pedidoBanco = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedidoDTO.getIdCliente() != null) {
            try {

                UsuarioResumeDTO usuario = usuarioClient.buscarUsuario(pedidoDTO.getIdCliente());

            } catch (HttpServerErrorException e) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cliente não encontrado"
                );

            }
            pedidoBanco.setIdCliente(pedidoDTO.getIdCliente());
        }

        if (pedidoDTO.getItens() != null) {

            List<ItemPedido> novosItens = DataMapper.parseListObjects(pedidoDTO.getItens(), ItemPedido.class);

            pedidoBanco.getItens().clear();

            novosItens.forEach(item -> {item.setPedido(pedidoBanco);pedidoBanco.getItens().add(item);});

            double valorTotal = pedidoBanco.getItens()
                    .stream()
                    .mapToDouble(item -> item.getPreco() * item.getQuantidade())
                    .sum();

            pedidoBanco.setValorTotal(valorTotal);
        }

        PedidoResponseDTO pedidoSalvo = DataMapper.parseObject(pedidoRepository.save(pedidoBanco), PedidoResponseDTO.class);
        streamBridge.send("sd-api-pedido", pedidoSalvo);
        return pedidoSalvo;
    }

    public PedidoResponseDTO pagarPedido(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getIdCliente() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido não possui cliente");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido não possui itens");
        }

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pedido não está aguardando pagamento");
        }

        RequisicaoPagamento requisicao =
                RequisicaoPagamento.newBuilder()
                        .setIdUsuario(String.valueOf(pedido.getIdCliente()))
                        .setIdPedido(String.valueOf(pedido.getId()))
                        .setValor(pedido.getValorTotal())
                        .setFormaPagamento("PIX")
                        .build();

        RespostaPagamento resposta = stubPagamento.processarPagamento(requisicao);

        if ("SUCESSO".equals(resposta.getStatus())) {
            pedido.setStatus(StatusPedido.PAGAMENTO_APROVADO);
        } else {
            pedido.setStatus(StatusPedido.PAGAMENTO_RECUSADO);
        }

        PedidoResponseDTO pedidoSalvo = DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
        streamBridge.send("sd-api-pedido", pedidoSalvo);
        return pedidoSalvo;
    }

    public PedidoResponseDTO iniciarPreparo(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.PAGAMENTO_APROVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente pedidos pagos podem entrar em preparo");
        }

        pedido.setStatus(StatusPedido.EM_PREPARO);

        PedidoResponseDTO pedidoSalvo = DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
        streamBridge.send("sd-api-pedido", pedidoSalvo);
        return pedidoSalvo;
    }

    public PedidoResponseDTO finalizarPreparo(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.EM_PREPARO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não está em preparo");
        }

        pedido.setStatus(StatusPedido.PRONTO_PARA_ENTREGA);

        PedidoResponseDTO pedidoSalvo = DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);



        streamBridge.send("sd-api-pedido", pedidoSalvo);
        rabbitTemplate.convertAndSend(queueName, pedidoSalvo);

        return DataMapper.parseObject(pedidoSalvo, PedidoResponseDTO.class);
    }

    public PedidoResponseDTO estornarPedido(Long id, String motivo) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.PAGAMENTO_APROVADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente pedidos pagos podem ser estornados");
        }

        RequisicaoEstorno requisicao =
                RequisicaoEstorno.newBuilder()
                        .setIdPedido(String.valueOf(pedido.getId()))
                        .setMotivo(motivo)
                        .build();

        RespostaEstorno resposta =
                stubPagamento.estornarPagamento(requisicao);

        if ("ESTORNADO".equals(resposta.getStatus())) {

            pedido.setStatus(StatusPedido.CANCELADO);

            Pedido pedidoSalvo =
                    pedidoRepository.save(pedido);

            PedidoResponseDTO dto =
                    DataMapper.parseObject(
                            pedidoSalvo,
                            PedidoResponseDTO.class);

            streamBridge.send("sd-api-pedido", dto);

            return dto;
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                resposta.getMensagem());
    }

    public SaldoDTO consultarSaldo() {

        RequisicaoSaldo requisicao =
                RequisicaoSaldo.newBuilder()
                        .build();

        return DataMapper.parseObject(stubPagamento.consultarSaldo(requisicao), SaldoDTO.class);
    }

    public ListaTransacoesDTO listarTransacoes(
            int pagina,
            int tamanho) {

        RespostaListaTransacoes resposta =
                stubPagamento.listarTransacoes(
                        RequisicaoListaTransacoes.newBuilder()
                                .setPagina(pagina)
                                .setTamanho(tamanho)
                                .build()
                );

        List<TransacaoDTO> transacoes =
                resposta.getTransacoesList()
                        .stream()
                        .map(t -> new TransacaoDTO(
                                t.getIdTransacao(),
                                t.getIdUsuario(),
                                t.getIdPedido(),
                                t.getValor(),
                                t.getFormaPagamento(),
                                t.getStatus(),
                                t.getCriadoEm(),
                                t.getEstornadoEm(),
                                t.getMotivoEstorno()
                        ))
                        .toList();

        return new ListaTransacoesDTO(transacoes, resposta.getTotalElementos());
    }

    public PedidoResponseDTO buscarPorId(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        return DataMapper.parseObject(pedido, PedidoResponseDTO.class);
    }

    public List<PedidoResponseDTO> busarTodos() {
        return DataMapper.parseListObjects(pedidoRepository.findAll(), PedidoResponseDTO.class);
    }

    public void remover(Long id) {

        pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        pedidoRepository.deleteById(id);
    }
}
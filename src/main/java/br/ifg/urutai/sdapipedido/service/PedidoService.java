package br.ifg.urutai.sdapipedido.service;
import br.ifg.urutai.sdapipedido.dto.PedidoCreteDTO;
import br.ifg.urutai.sdapipedido.dto.PedidoResponseDTO;
import br.ifg.urutai.sdapipedido.model.ItemPedido;
import br.ifg.urutai.sdapipedido.model.Pedido;
import br.ifg.urutai.sdapipedido.model.StatusPedido;
import br.ifg.urutai.sdapipedido.repository.PedidoRepository;
import br.ifg.urutai.sdapipedido.util.DataMapper;
import ifg.urutai.sdapipagamento.grpc.RequisicaoPagamento;
import ifg.urutai.sdapipagamento.grpc.RespostaPagamento;
import ifg.urutai.sdapipagamento.grpc.ServicoPagamentoGrpc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PedidoService {

    private final ServicoPagamentoGrpc.ServicoPagamentoBlockingStub stubPagamento;
    private final RabbitTemplate rabbitTemplate;
    private final String queueName;
    private final PedidoRepository pedidoRepository;

    @Autowired
    public PedidoService(ServicoPagamentoGrpc.ServicoPagamentoBlockingStub stubPagamento, RabbitTemplate rabbitTemplate, @Value("${app.queue-name}") String queueName, PedidoRepository pedidoRepository) {
        this.stubPagamento = stubPagamento;
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
        this.pedidoRepository = pedidoRepository;
    }

    public PedidoResponseDTO criarPedido(PedidoCreteDTO pedidoDTO) {

        Pedido pedido = DataMapper.parseObject(pedidoDTO, Pedido.class);
        pedido.setId(null);

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pedido deve possuir ao menos um item");
        }

        double valorTotal = pedido.getItens()
                .stream()
                .mapToDouble(item -> item.getPreco() * item.getQuantidade())
                .sum();

        pedido.setValorTotal(valorTotal);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        pedido.getItens().forEach(item -> item.setPedido(pedido));

        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO atualizar(Long id, PedidoCreteDTO pedidoDTO) {

        Pedido pedidoBanco = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedidoDTO.getIdCliente() != null) {
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

        return DataMapper.parseObject(pedidoRepository.save(pedidoBanco), PedidoResponseDTO.class);
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

        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO iniciarPreparo(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.PAGAMENTO_APROVADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente pedidos pagos podem entrar em preparo");
        }

        pedido.setStatus(StatusPedido.EM_PREPARO);

        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO finalizarPreparo(Long id) {

        Pedido pedido = pedidoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.EM_PREPARO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não está em preparo");
        }

        pedido.setStatus(StatusPedido.PRONTO_PARA_ENTREGA);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        rabbitTemplate.convertAndSend(queueName, pedidoSalvo);

        System.out.println("Mensagem enviada: Pedido " + pedidoSalvo.getId());

        return DataMapper.parseObject(pedidoSalvo, PedidoResponseDTO.class);
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
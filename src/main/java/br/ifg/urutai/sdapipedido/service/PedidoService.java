package br.ifg.urutai.sdapipedido.service;

import br.ifg.urutai.sdapipedido.StatusPedido;
import br.ifg.urutai.sdapipedido.dto.PedidoResponseDTO;
import br.ifg.urutai.sdapipedido.model.Pedido;
import br.ifg.urutai.sdapipedido.repository.PedidoRepository;
import br.ifg.urutai.sdapipedido.util.DataMapper;
import ifg.urutai.sdapipagamento.grpc.RequisicaoPagamento;
import ifg.urutai.sdapipagamento.grpc.RespostaPagamento;
import ifg.urutai.sdapipagamento.grpc.ServicoPagamentoGrpc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;


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

    public PedidoResponseDTO criarPedido(Pedido pedido) {

        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        if (pedido.getItens() != null) {
            pedido.getItens().forEach(item -> item.setPedido(pedido));
        }

        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO atualizar(Long id,Pedido pedido) {
        Pedido pedidoNovo = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getStatus() != null &&
                !pedido.getStatus().equals(pedidoNovo.getStatus())) {
            pedidoNovo.setStatus(pedido.getStatus());
        }

        if (!Objects.equals(pedido.getIdCliente(), pedidoNovo.getIdCliente())) {
            pedidoNovo.setIdCliente(pedido.getIdCliente());
        }

        if (pedido.getValorTotal() != pedidoNovo.getValorTotal()) {
            pedidoNovo.setValorTotal(pedido.getValorTotal());
        }

        if (pedido.getItens() != null &&
                !pedido.getItens().equals(pedidoNovo.getItens())) {

            pedido.getItens().forEach(item -> item.setPedido(pedidoNovo));

            pedidoNovo.setItens(pedido.getItens());
        }

        return DataMapper.parseObject(pedidoRepository.save(pedidoNovo),PedidoResponseDTO.class);
    }

    public PedidoResponseDTO pagarPedido(Long id) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getIdCliente() == null) {
            throw new RuntimeException("Pedido não possui cliente");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new RuntimeException("Pedido não possui itens");
        }

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RuntimeException("Status inválido");
        }

        RequisicaoPagamento requisicao =
                RequisicaoPagamento.newBuilder()
                        .setNomeUsuario("Cliente " + pedido.getIdCliente())
                        .setEmail("cliente@email.com")
                        .setValor(pedido.getValorTotal())
                        .setFormaPagamento("PIX")
                        .build();

        RespostaPagamento resposta =
                stubPagamento.processarPagamento(requisicao);

        if ("SUCESSO".equals(resposta.getStatus())) {

            pedido.setStatus(StatusPedido.PAGAMENTO_APROVADO);

        }else {
            pedido.setStatus(StatusPedido.PAGAMENTO_RECUSADO);

        }
        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO iniciarPreparo(Long id) {

        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() != StatusPedido.PAGAMENTO_APROVADO) {
            throw new RuntimeException(
                    "Somente pedidos pagos podem entrar em preparo");
        }

        pedido.setStatus(StatusPedido.EM_PREPARO);

        return DataMapper.parseObject(pedidoRepository.save(pedido), PedidoResponseDTO.class);
    }

    public PedidoResponseDTO finalizarPreparo(Long id) {

        Pedido pedido = buscarPorId(id);

        if (pedido.getStatus() != StatusPedido.EM_PREPARO) {
            throw new RuntimeException(
                    "Pedido não está em preparo");
        }

        pedido.setStatus(StatusPedido.PRONTO_PARA_ENTREGA);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        rabbitTemplate.convertAndSend(queueName, pedidoSalvo);

        System.out.println("Mensagem enviada: Pedido " +
                pedidoSalvo.getId());

        return DataMapper.parseObject(pedidoSalvo, PedidoResponseDTO.class);
    }



    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id).orElseThrow(()-> new RuntimeException("Pedido não encontrado"));
    }

    public List<Pedido> busarTodos(){
        return pedidoRepository.findAll();
    }

    public void remover(Long id) {
        pedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Item não encontrado"));
        pedidoRepository.deleteById(id);
    }


}

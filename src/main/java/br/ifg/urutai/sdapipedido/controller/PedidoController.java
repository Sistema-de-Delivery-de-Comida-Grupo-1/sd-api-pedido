package br.ifg.urutai.sdapipedido.controller;

import br.ifg.urutai.sdapipedido.dto.PedidoResponseDTO;
import br.ifg.urutai.sdapipedido.model.Pedido;
import br.ifg.urutai.sdapipedido.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criarPedido(@RequestBody Pedido pedido) {
        return ResponseEntity.ok(
                pedidoService.criarPedido(pedido)
        );
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponseDTO> pagarPedido(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.pagarPedido(id)
        );
    }

    @PostMapping("/{id}/iniciar-preparo")
    public ResponseEntity<PedidoResponseDTO> iniciarPreparo(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.iniciarPreparo(id)
        );
    }

    @PostMapping("/{id}/finalizar-preparo")
    public ResponseEntity<PedidoResponseDTO> finalizarPreparo(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.finalizarPreparo(id)
        );
    }

    @PostMapping("/{id}/sair-para-entrega")
    public ResponseEntity<PedidoResponseDTO> sairParaEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.sairParaEntrega(id)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.buscarPorId(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        return ResponseEntity.ok(
                pedidoService.busarTodos()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody Pedido pedido) {

        return ResponseEntity.ok(
                pedidoService.atualizar(id, pedido)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        pedidoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
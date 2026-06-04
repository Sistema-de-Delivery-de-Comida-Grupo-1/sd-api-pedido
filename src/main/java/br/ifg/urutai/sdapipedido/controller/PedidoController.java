package br.ifg.urutai.sdapipedido.controller;

import br.ifg.urutai.sdapipedido.dto.PedidoCreteDTO;
import br.ifg.urutai.sdapipedido.dto.PedidoResponseDTO;
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
    public ResponseEntity<PedidoResponseDTO> criarPedido(@RequestBody PedidoCreteDTO pedido) {
        return ResponseEntity.ok(
                pedidoService.criarPedido(pedido)
        );
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponseDTO> pagarPedido(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.pagarPedido(id)
        );
    }

    @PutMapping("/{id}/iniciar-preparo")
    public ResponseEntity<PedidoResponseDTO> iniciarPreparo(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.iniciarPreparo(id)
        );
    }

    @PutMapping("/{id}/finalizar-preparo")
    public ResponseEntity<PedidoResponseDTO> finalizarPreparo(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.finalizarPreparo(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> atualizar(@PathVariable Long id, @RequestBody PedidoCreteDTO pedido) {

        return ResponseEntity.ok(
                pedidoService.atualizar(id, pedido)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                pedidoService.buscarPorId(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(
                pedidoService.busarTodos()
        );
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        pedidoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
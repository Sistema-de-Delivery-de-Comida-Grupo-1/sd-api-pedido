package br.ifg.urutai.sdapipedido.dto;

import br.ifg.urutai.sdapipedido.StatusPedido;
import java.util.List;

public class PedidoResponseDTO {
    private Long id;
    private Long idCliente;
    private int valorTotal;
    private List<ItemPedidoResumeDTO> itens;
    private StatusPedido status;

    public PedidoResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public int getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(int valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<ItemPedidoResumeDTO> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedidoResumeDTO> itens) {
        this.itens = itens;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }
}

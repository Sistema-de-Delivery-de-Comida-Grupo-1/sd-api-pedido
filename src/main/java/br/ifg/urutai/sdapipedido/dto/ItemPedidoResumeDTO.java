package br.ifg.urutai.sdapipedido.dto;

import java.io.Serializable;

public class ItemPedidoResumeDTO implements Serializable {
    private Long id;
    private int quantidade;
    private double preco;
    private String descricao;

    public ItemPedidoResumeDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}

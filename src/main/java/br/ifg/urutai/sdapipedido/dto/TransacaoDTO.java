package br.ifg.urutai.sdapipedido.dto;

public class TransacaoDTO {
    private String idTransacao;
    private String idUsuario;
    private String idPedido;
    private double valor;
    private String formaPagamento;
    private String status;
    private String criadoEm;
    private String estornadoEm;
    private String motivoEstorno;

    public TransacaoDTO() {
    }

    public TransacaoDTO(String idTransacao, String idUsuario, String idPedido, double valor, String formaPagamento, String status, String criadoEm, String estornadoEm, String motivoEstorno) {
        this.idTransacao = idTransacao;
        this.idUsuario = idUsuario;
        this.idPedido = idPedido;
        this.valor = valor;
        this.formaPagamento = formaPagamento;
        this.status = status;
        this.criadoEm = criadoEm;
        this.estornadoEm = estornadoEm;
        this.motivoEstorno = motivoEstorno;
    }

    public String getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(String idTransacao) {
        this.idTransacao = idTransacao;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(String criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getEstornadoEm() {
        return estornadoEm;
    }

    public void setEstornadoEm(String estornadoEm) {
        this.estornadoEm = estornadoEm;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public void setMotivoEstorno(String motivoEstorno) {
        this.motivoEstorno = motivoEstorno;
    }
}

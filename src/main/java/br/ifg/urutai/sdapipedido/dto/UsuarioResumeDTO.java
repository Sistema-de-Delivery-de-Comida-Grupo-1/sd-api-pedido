package br.ifg.urutai.sdapipedido.dto;

public class UsuarioResumeDTO {
    private Long id;
    private String nome;

    public UsuarioResumeDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}

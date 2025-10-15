package com.example.startuppulse.data;

import java.io.Serializable;

/**
 * Representa uma etapa (página) no processo de criação da ideia no ViewPager.
 * Esta classe é imutável para garantir consistência após sua criação.
 */
public class CanvasEtapa implements Serializable {

    // --- Constantes para as chaves das etapas ---
    // --- Constantes para as chaves de todas as etapas ---
    public static final String CHAVE_STATUS = "STATUS";
    public static final String CHAVE_INICIO = "INICIO";
    public static final String CHAVE_AMBIENTE_CHECK = "AMBIENTE_CHECK";
    public static final String CHAVE_PROPOSTA_VALOR = "PROPOSTA_VALOR";
    public static final String CHAVE_SEGMENTO_CLIENTES = "SEGMENTO_CLIENTES";
    public static final String CHAVE_CANAIS = "CANAIS";
    public static final String CHAVE_RELACIONAMENTO_CLIENTES = "RELACIONAMENTO_CLIENTES";
    public static final String CHAVE_FONTES_RENDA = "FONTES_RENDA";
    public static final String CHAVE_RECURSOS_PRINCIPAIS = "RECURSOS_PRINCIPAIS";
    public static final String CHAVE_ATIVIDADES_CHAVE = "ATIVIDADES_CHAVE";
    public static final String CHAVE_PARCERIAS_PRINCIPAIS = "PARCERIAS_PRINCIPAIS";
    public static final String CHAVE_ESTRUTURA_CUSTOS = "ESTRUTURA_CUSTOS";
    public static final String CHAVE_EQUIPE = "EQUIPE";
    public static final String CHAVE_FINAL = "FINAL";


    // --- Propriedades ---
    private final String chave;
    private final String titulo;
    private final String descricao;
    private final int iconeResId;


    // --- Construtores ---

    /**
     * Construtor principal para criar uma nova etapa.
     * @param chave Identificador único da etapa.
     * @param titulo Título exibido na aba.
     * @param descricao Texto de ajuda exibido na página.
     * @param iconeResId Recurso do ícone para a aba.
     */
    public CanvasEtapa(String chave, String titulo, String descricao, int iconeResId) {
        this.chave = chave;
        this.titulo = titulo;
        this.descricao = descricao;
        this.iconeResId = iconeResId;
    }

    /**
     * Construtor de conveniência para etapas que não precisam de descrição.
     */
    public CanvasEtapa(String chave, String titulo, int iconeResId) {
        this(chave, titulo, "", iconeResId);
    }


    // --- Getters ---
    public String getChave() { return chave; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public int getIconeResId() { return iconeResId; }
}
package br.com.bemestar.clinica.persistencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Leitura e escrita de CSV separado por ponto e virgula (padrao do Excel em
 * portugues). Campos com separador, aspas ou quebra de linha sao colocados
 * entre aspas, com aspas internas duplicadas.
 */
final class Csv {

    static final char SEPARADOR = ';';

    private Csv() {
    }

    /** Monta uma linha CSV (terminada em \n) a partir dos campos. Null vira campo vazio. */
    static String linha(Object... campos) {
        StringBuilder linha = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linha.append(SEPARADOR);
            }
            linha.append(escapar(campos[i] == null ? "" : campos[i].toString()));
        }
        return linha.append('\n').toString();
    }

    static String escapar(String valor) {
        boolean precisaAspas = valor.indexOf(SEPARADOR) >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        return precisaAspas ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }

    /** Separa o conteudo em linhas e campos, respeitando campos entre aspas. */
    static List<List<String>> ler(String conteudo) {
        List<List<String>> linhas = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreAspas = false;
        int i = 0;
        if (conteudo.startsWith(String.valueOf((char) 0xFEFF))) {
            i = 1; // ignora BOM gerado por alguns editores
        }
        for (; i < conteudo.length(); i++) {
            char c = conteudo.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < conteudo.length() && conteudo.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == SEPARADOR) {
                campos.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < conteudo.length() && conteudo.charAt(i + 1) == '\n') {
                    i++;
                }
                campos.add(campo.toString());
                campo.setLength(0);
                adicionarSeNaoVazia(linhas, campos);
                campos = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (entreAspas) {
            throw new IllegalArgumentException("campo entre aspas nao foi fechado");
        }
        if (campo.length() > 0 || !campos.isEmpty()) {
            campos.add(campo.toString());
            adicionarSeNaoVazia(linhas, campos);
        }
        return linhas;
    }

    private static void adicionarSeNaoVazia(List<List<String>> linhas, List<String> campos) {
        boolean vazia = campos.size() == 1 && campos.get(0).isBlank();
        if (!vazia) {
            linhas.add(campos);
        }
    }
}

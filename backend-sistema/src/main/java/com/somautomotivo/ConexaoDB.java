package com.somautomotivo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexaoDB {

    private static final String URL = "jdbc:mysql://localhost:3306/som_automotivo_app";
    private static final String USER = "root"; 
    private static final String PASSWORD = "root"; // Sua senha configurada

    public static void main(String[] args) {
        System.out.println("Iniciando o Sistema de Analise Completa...\n");

        try {
            Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);
            
            // 1. Buscando os dados do Projeto e do Módulo
            String sqlModulo = "SELECT p.nome_projeto, p.distancia_bateria_metros, p.amperagem_bateria_atual, " +
                               "m.impedancia_minima, m.consumo_max_amperes " +
                               "FROM projetos p " +
                               "JOIN projeto_modulos pm ON p.id = pm.projeto_id " +
                               "JOIN modulos m ON pm.modulo_id = m.id " +
                               "WHERE p.id = 2"; 
            
            PreparedStatement cmdModulo = conexao.prepareStatement(sqlModulo);
            ResultSet resModulo = cmdModulo.executeQuery();
            
            // 2. Buscando os dados dos Alto-Falantes
            String sqlFalantes = "SELECT pa.quantidade, pa.tipo_ligacao, a.impedancia_nominal " +
                                 "FROM projeto_alto_falantes pa " +
                                 "JOIN alto_falantes a ON pa.alto_falante_id = a.id " +
                                 "WHERE pa.projeto_id = 2";
            
            PreparedStatement cmdFalantes = conexao.prepareStatement(sqlFalantes);
            ResultSet resFalantes = cmdFalantes.executeQuery();
            
            if (resModulo.next() && resFalantes.next()) {
                // Extraindo todas as variáveis para a memória do Java
                String nomeProjeto = resModulo.getString("nome_projeto");
                double impMinimaModulo = resModulo.getDouble("impedancia_minima");
                int consumoMax = resModulo.getInt("consumo_max_amperes");
                double distanciaCabo = resModulo.getDouble("distancia_bateria_metros");
                int bateriaAtual = resModulo.getInt("amperagem_bateria_atual");
                
                int qtdFalantes = resFalantes.getInt("quantidade");
                String tipoLigacao = resFalantes.getString("tipo_ligacao");
                double impFalante = resFalantes.getDouble("impedancia_nominal");
                
                System.out.println("=================================================");
                System.out.println(" RELATORIO DE ENGENHARIA AUTOMOTIVA");
                System.out.println(" PROJETO: " + nomeProjeto);
                System.out.println("=================================================");
                
                // --- PILAR 1: BITOLA DO CABO ---
                System.out.println("\n[1] DIMENSIONAMENTO DE CABOS");
                System.out.print(" -> Bitola Recomendada (" + distanciaCabo + "m): ");
                if (consumoMax <= 50) System.out.println("10 mm2");
                else if (consumoMax <= 100) System.out.println("21 mm2");
                else if (consumoMax <= 150) System.out.println("35 mm2");
                else System.out.println("50 mm2 ou superior");

                // --- PILAR 2: CASAMENTO DE IMPEDANCIA ---
                System.out.println("\n[2] CASAMENTO DE IMPEDANCIA");
                double impedanciaFinal = 0.0;
                
                // Calculo sem acentos para evitar bugs
                if (tipoLigacao.equals("Paralelo")) {
                    impedanciaFinal = impFalante / qtdFalantes;
                } else if (tipoLigacao.equals("Serie")) {
                    impedanciaFinal = impFalante * qtdFalantes;
                } else if (tipoLigacao.equals("Serie-Paralelo")) {
                    impedanciaFinal = impFalante; 
                }
                
                System.out.println(" -> Falantes em " + tipoLigacao + " geram: " + impedanciaFinal + " ohms");
                if (impedanciaFinal >= impMinimaModulo) {
                    System.out.println(" -> STATUS: [ OK ] Ligacao Segura.");
                } else {
                    System.out.println(" -> STATUS: [ PERIGO ] Risco de queima do modulo (" + impMinimaModulo + " ohms minimo)!");
                }

                // --- PILAR 3: DIMENSIONAMENTO DE ENERGIA (NOVO) ---
                System.out.println("\n[3] ALIMENTACAO E BATERIA");
                System.out.println(" -> Consumo Maximo do Sistema: " + consumoMax + " Ah");
                System.out.println(" -> Bateria Atual do Veiculo: " + bateriaAtual + " Ah");
                
                // Reserva de segurança do veículo (Motor, Farol, etc) ~ 50Ah
                int bateriaDisponivelProSom = bateriaAtual - 50; 
                if (bateriaDisponivelProSom < 0) bateriaDisponivelProSom = 0;

                if (bateriaDisponivelProSom >= consumoMax) {
                    System.out.println(" -> STATUS: [ OK ] Bateria original suporta o projeto.");
                } else {
                    int bateriaAuxiliarFaltando = consumoMax - bateriaDisponivelProSom;
                    System.out.println(" -> STATUS: [ ALERTA ] Faltara energia para os picos de grave!");
                    System.out.println(" -> RECOMENDACAO: Adicionar Bateria Auxiliar de no minimo " + bateriaAuxiliarFaltando + " Ah.");
                }
                System.out.println("=================================================\n");
            }
            
            conexao.close(); 
            
        } catch (SQLException e) {
            System.out.println("Erro no sistema: " + e.getMessage());
        }
    }
}
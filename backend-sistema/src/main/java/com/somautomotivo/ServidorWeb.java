package com.somautomotivo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import io.javalin.Javalin;

public class ServidorWeb {

    // Credenciais do Banco na NUVEM
    // O Java procura a credencial da nuvem (Render). Se não achar (no seu PC), ele usa o seu banco local de testes!
    private static final String URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/som_automotivo_app";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root"; 
    private static final String PASSWORD = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "root";

    public static void main(String[] args) {
        System.out.println("Ligando as luzes do salão...");

        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost())); 
        }).start(7070); 

        System.out.println("Servidor Online! O Garçom está escutando na porta 7070.");

        // =========================================================
        // ROTA 1: TESTE
        // =========================================================
        app.get("/", ctx -> { ctx.result("✅ Servidor Online!"); });

        // =========================================================
        // ROTA 2: BUSCAR O ESTOQUE
        // =========================================================
        app.get("/produtos", ctx -> {
            StringBuilder jsonEstoque = new StringBuilder();
            jsonEstoque.append("["); 

            try {
                Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);

                String sqlModulos = "SELECT id, marca, modelo, potencia_rms, impedancia_minima FROM modulos";
                PreparedStatement cmdModulos = conexao.prepareStatement(sqlModulos);
                ResultSet resModulos = cmdModulos.executeQuery();

                while (resModulos.next()) {
                    jsonEstoque.append("{");
                    jsonEstoque.append("\"id\":").append(resModulos.getInt("id")).append(",");
                    jsonEstoque.append("\"tipo\":\"Modulo\",");
                    jsonEstoque.append("\"nome\":\"").append(resModulos.getString("marca")).append(" ").append(resModulos.getString("modelo")).append("\",");
                    jsonEstoque.append("\"potencia\":").append(resModulos.getInt("potencia_rms")).append(",");
                    jsonEstoque.append("\"impedancia\":").append(resModulos.getDouble("impedancia_minima"));
                    jsonEstoque.append("},");
                }

                String sqlFalantes = "SELECT id, marca, modelo, potencia_rms, impedancia_nominal FROM alto_falantes";
                PreparedStatement cmdFalantes = conexao.prepareStatement(sqlFalantes);
                ResultSet resFalantes = cmdFalantes.executeQuery();

                while (resFalantes.next()) {
                    jsonEstoque.append("{");
                    jsonEstoque.append("\"id\":").append(resFalantes.getInt("id")).append(",");
                    jsonEstoque.append("\"tipo\":\"Falante\",");
                    jsonEstoque.append("\"nome\":\"").append(resFalantes.getString("marca")).append(" ").append(resFalantes.getString("modelo")).append("\",");
                    jsonEstoque.append("\"potencia\":").append(resFalantes.getInt("potencia_rms")).append(",");
                    jsonEstoque.append("\"impedancia\":").append(resFalantes.getDouble("impedancia_nominal"));
                    jsonEstoque.append("},");
                }
                conexao.close();

                if (jsonEstoque.length() > 1) {
                    jsonEstoque.setLength(jsonEstoque.length() - 1); 
                }
                jsonEstoque.append("]");

            } catch (Exception e) {
                ctx.result("[]"); 
                return;
            }
            ctx.contentType("application/json"); 
            ctx.result(jsonEstoque.toString());
        });

        // =========================================================
        // ROTA 3: CALCULAR O PROJETO (A Mágica)
        // =========================================================
        app.post("/calcular", ctx -> {
            // (Código de cálculo continua intacto)
            String carrinhoCru = ctx.body(); 
            String carrinhoLimpo = carrinhoCru.replace("[", "").replace("]", "").replace("\"", "");
            String[] itensEscolhidos = carrinhoLimpo.split(","); 

            double impMinimaModulo = 0;
            int consumoMaxModulo = 0;
            boolean temModulo = false;
            double impFalante = 0;
            int qtdFalantes = 0;

            StringBuilder relatorio = new StringBuilder();
            relatorio.append("📋 RELATÓRIO DO SEU PROJETO\n");
            relatorio.append("=====================================\n\n");

            try {
                Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);

                for (String item : itensEscolhidos) {
                    item = item.trim(); 
                    if (item.isEmpty()) continue;

                    String sqlMod = "SELECT impedancia_minima, consumo_max_amperes FROM modulos WHERE ? LIKE CONCAT('%', modelo, '%')";
                    PreparedStatement cmdMod = conexao.prepareStatement(sqlMod);
                    cmdMod.setString(1, item);
                    ResultSet resMod = cmdMod.executeQuery();

                    if (resMod.next()) {
                        impMinimaModulo = resMod.getDouble("impedancia_minima");
                        consumoMaxModulo = resMod.getInt("consumo_max_amperes");
                        temModulo = true;
                    }

                    String sqlFal = "SELECT impedancia_nominal FROM alto_falantes WHERE ? LIKE CONCAT('%', modelo, '%')";
                    PreparedStatement cmdFal = conexao.prepareStatement(sqlFal);
                    cmdFal.setString(1, item);
                    ResultSet resFal = cmdFal.executeQuery();

                    if (resFal.next()) {
                        impFalante = resFal.getDouble("impedancia_nominal");
                        qtdFalantes++; 
                    }
                }
                conexao.close();

                if (temModulo && qtdFalantes > 0) {
                    relatorio.append("[1] CABO DE ENERGIA (BATERIA)\n");
                    relatorio.append("-> Consumo do Módulo: ").append(consumoMaxModulo).append(" Amperes\n");
                    relatorio.append("-> Bitola Recomendada: ");
                    if (consumoMaxModulo <= 50) relatorio.append("10 mm²\n\n");
                    else if (consumoMaxModulo <= 100) relatorio.append("21 mm²\n\n");
                    else if (consumoMaxModulo <= 150) relatorio.append("35 mm²\n\n");
                    else relatorio.append("50 mm² ou superior!\n\n");

                    relatorio.append("[2] CASAMENTO DE IMPEDÂNCIA (Em Paralelo)\n");
                    double impedanciaFinal = impFalante / qtdFalantes;
                    relatorio.append("-> ").append(qtdFalantes).append(" falante(s) de ").append(impFalante).append(" ohms.\n");
                    relatorio.append("-> Impedância Final: ").append(impedanciaFinal).append(" ohms.\n");
                    
                    if (impedanciaFinal >= impMinimaModulo) {
                        relatorio.append("-> STATUS: ✅ SEGURO! O módulo não vai queimar.\n\n");
                    } else {
                        relatorio.append("-> STATUS: ❌ PERIGO! Risco de queima.\n\n");
                    }

                    relatorio.append("[3] ALIMENTAÇÃO\n");
                    int sobraProSom = 60 - 40; 
                    if (sobraProSom >= consumoMaxModulo) {
                        relatorio.append("-> STATUS: ✅ A bateria do carro aguenta o som!\n");
                    } else {
                        int falta = consumoMaxModulo - sobraProSom;
                        relatorio.append("-> STATUS: ⚠️ ALERTA DE BATERIA!\n");
                        relatorio.append("-> Recomendação: Instalar uma bateria auxiliar de no mínimo ").append(falta).append(" Ah.\n");
                    }
                } else {
                    relatorio.append("⚠️ Seu projeto está incompleto!\n");
                    relatorio.append("Adicione pelo menos 1 Módulo e 1 Alto-Falante para calcularmos.");
                }

            } catch (Exception e) {
                relatorio.append("❌ Erro interno no servidor: ").append(e.getMessage());
            }
            ctx.result(relatorio.toString());
        });

        // =========================================================
        // ROTA 4: CADASTRAR NOVO MÓDULO (VIA PAINEL)
        // =========================================================
        app.post("/cadastrar-modulo", ctx -> {
            try {
                // Pega o que você digitou na tela admin.html
                String marca = ctx.formParam("marca");
                String modelo = ctx.formParam("modelo");
                int potencia = Integer.parseInt(ctx.formParam("potencia_rms"));
                double imp = Double.parseDouble(ctx.formParam("impedancia_minima"));
                int consumo = Integer.parseInt(ctx.formParam("consumo_max_amperes"));

                // Salva no MySQL automaticamente!
                Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);
                String sql = "INSERT INTO modulos (marca, modelo, potencia_rms, impedancia_minima, consumo_max_amperes) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement cmd = conexao.prepareStatement(sql);
                cmd.setString(1, marca);
                cmd.setString(2, modelo);
                cmd.setInt(3, potencia);
                cmd.setDouble(4, imp);
                cmd.setInt(5, consumo);
                cmd.executeUpdate();
                conexao.close();

                ctx.result("✅ Sucesso! O Módulo " + marca + " " + modelo + " foi salvo no banco!");
            } catch (Exception e) {
                ctx.result("❌ Erro ao salvar: " + e.getMessage());
            }
        });

        // =========================================================
        // ROTA 5: CADASTRAR NOVO ALTO-FALANTE (VIA PAINEL)
        // =========================================================
        app.post("/cadastrar-falante", ctx -> {
            try {
                String marca = ctx.formParam("marca");
                String modelo = ctx.formParam("modelo");
                int potencia = Integer.parseInt(ctx.formParam("potencia_rms"));
                double imp = Double.parseDouble(ctx.formParam("impedancia_nominal"));

                Connection conexao = DriverManager.getConnection(URL, USER, PASSWORD);
                String sql = "INSERT INTO alto_falantes (marca, modelo, potencia_rms, impedancia_nominal) VALUES (?, ?, ?, ?)";
                PreparedStatement cmd = conexao.prepareStatement(sql);
                cmd.setString(1, marca);
                cmd.setString(2, modelo);
                cmd.setInt(3, potencia);
                cmd.setDouble(4, imp);
                cmd.executeUpdate();
                conexao.close();

                ctx.result("✅ Sucesso! O Falante " + marca + " " + modelo + " foi salvo no banco!");
            } catch (Exception e) {
                ctx.result("❌ Erro ao salvar: " + e.getMessage());
            }
        });
    }
}
package com.postechfiap;

import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.core.util.polling.SyncPoller;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

public class Function {

    @FunctionName("envia-relatorio")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION
            )
            HttpRequestMessage<Optional<RelatorioDTO>> request,
            final ExecutionContext context) {

        context.getLogger().info("--- INÍCIO DO PROCESSAMENTO DE RELATÓRIO ---");

        RelatorioDTO dto = request.getBody().orElse(null);

        if (dto == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Payload inválido: informe a quantidade de cursos e a média de notas.")
                    .build();
        }

        context.getLogger().info("Dados recebidos: Quantidade=" + dto.getQuantidadeCursos() + ", Média=" + dto.getMediaNotas());

        String endpoint = System.getenv("ACS_ENDPOINT");
        String senderEmail = System.getenv("SENDER_EMAIL");

        if (endpoint == null || senderEmail == null) {
            context.getLogger().severe("Erro de configuração: ACS_ENDPOINT ou SENDER_EMAIL não definidos.");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro de configuração no servidor.")
                    .build();
        }

        try {
            EmailClient emailClient = new EmailClientBuilder()
                    .endpoint(endpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            String htmlBody = createHtmlBody(dto.getQuantidadeCursos(), dto.getMediaNotas());

            EmailMessage message = new EmailMessage()
                    .setSenderAddress(senderEmail)
                    .setToRecipients(
                            new EmailAddress("admin@escola.com"),
                            new EmailAddress("franciscosouzalima@gmail.com"),
                            new EmailAddress("fuzetirafael@gmail.com"),
                            new EmailAddress("gustavosoaresbomfim@hotmail.com"),
                            new EmailAddress("fernanda_o_ferreira@hotmail.com")
                    )
                    .setSubject("Relatório Semanal de Avaliações")
                    .setBodyHtml(htmlBody);

            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
            poller.waitForCompletion();
            
            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Relatório enviado com sucesso.")
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Erro ao enviar e-mail: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar e-mail: " + e.getMessage())
                    .build();
        }
    }

    private String createHtmlBody(int quantidade, double media) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); overflow: hidden; }" +
                ".header { background-color: #0078d4; color: #ffffff; padding: 30px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 24px; }" +
                ".content { padding: 30px; color: #333333; }" +
                ".stats { display: flex; justify-content: space-around; margin: 30px 0; }" +
                ".stat-box { text-align: center; padding: 15px; background-color: #f9f9f9; border-radius: 8px; width: 40%; border: 1px solid #e0e0e0; }" +
                ".stat-value { display: block; font-size: 32px; font-weight: bold; color: #0078d4; }" +
                ".stat-label { font-size: 14px; color: #666666; }" +
                ".footer { background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 12px; color: #999999; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "  <div class=\"header\">" +
                "    <h1>Relatório Semanal</h1>" +
                "  </div>" +
                "  <div class=\"content\">" +
                "    <p>Olá, Professores,</p>" +
                "    <p>Confiram os resultados das avaliações desta semana:</p>" +
                "    <div class=\"stats\">" +
                "      <div class=\"stat-box\">" +
                "        <span class=\"stat-value\">" + quantidade + "</span>" +
                "        <span class=\"stat-label\">Cursos Avaliados</span>" +
                "      </div>" +
                "      <div class=\"stat-box\">" +
                "        <span class=\"stat-value\">" + String.format("%.1f", media) + "</span>" +
                "        <span class=\"stat-label\">Média de Notas</span>" +
                "      </div>" +
                "    </div>" +
                "    <p>Obrigado pelo empenho e dedicação!</p>" +
                "  </div>" +
                "  <div class=\"footer\">" +
                "    &copy; Postech FIAP. Enviado automaticamente." +
                "  </div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    public static class RelatorioDTO {
        private int quantidadeCursos;
        private double mediaNotas;

        public int getQuantidadeCursos() { return quantidadeCursos; }
        public void setQuantidadeCursos(int quantidadeCursos) { this.quantidadeCursos = quantidadeCursos; }

        public double getMediaNotas() { return mediaNotas; }
        public void setMediaNotas(double mediaNotas) { this.mediaNotas = mediaNotas; }
    }
}
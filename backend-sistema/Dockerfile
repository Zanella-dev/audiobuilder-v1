# Puxa um computador Linux já com o Java e o Maven instalados
FROM maven:3.9.6-eclipse-temurin-17

# Cria uma pasta de trabalho dentro do servidor
WORKDIR /app

# Copia o seu código para dentro do servidor
COPY . .

# Compila o projeto
RUN mvn clean compile

# Avisa o servidor que a nossa porta é a 7070
EXPOSE 7070

# O comando para ligar o motor!
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.somautomotivo.ServidorWeb"]
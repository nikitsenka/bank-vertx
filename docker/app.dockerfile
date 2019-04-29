FROM openjdk:11

RUN apt-get update && apt-get install -y git

RUN git clone https://github.com/nikitsenka/bank-vertx.git

WORKDIR bank-vertx
RUN chmod +x gradlew
RUN ./gradlew build

CMD java -jar build/libs/bankvertx-fat.jar -conf {\"host\":\"${POSTGRES_HOST}\"}


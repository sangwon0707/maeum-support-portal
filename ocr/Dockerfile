FROM maven:3.9.11-eclipse-temurin-17 AS pdf-builder
ENV MAVEN_OPTS="-Xmx192m -XX:+UseSerialGC"
WORKDIR /build
COPY pdf-engine/pom.xml ./pom.xml
COPY pdf-engine/LICENSE ./LICENSE
COPY pdf-engine/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS java-runtime

FROM python:3.11-slim-bookworm
ENV PYTHONUNBUFFERED=1 PADDLE_PDX_CACHE_HOME=/var/cache/paddle PADDLE_PDX_MODEL_SOURCE=BOS
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 libglib2.0-0 libgl1 libfreetype6 fontconfig fonts-noto-cjk && rm -rf /var/lib/apt/lists/*
COPY --from=java-runtime /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk PATH="/opt/java/openjdk/bin:${PATH}" PDF_ENGINE_JAR=/app/pdf-engine.jar
WORKDIR /app
COPY --from=pdf-builder /build/target/pdf-engine.jar /app/pdf-engine.jar
COPY ocr/requirements.txt .
# 저메모리 서버에서는 큰 휠의 임시 파일을 디스크에 두어 메모리 점유를 제한한다.
RUN TMPDIR=/var/tmp pip install --no-cache-dir --extra-index-url https://www.paddlepaddle.org.cn/packages/stable/cpu/ -r requirements.txt
RUN useradd --create-home --uid 10001 ocr && mkdir -p /var/cache/paddle && chown ocr /var/cache/paddle
COPY ocr/*.py ./
USER ocr
EXPOSE 8091
CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8091", "--no-access-log"]

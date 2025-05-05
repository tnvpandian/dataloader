gcloud artifacts repositories create java-docker-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="Docker repo for Spring Boot app"


 # ---------- Stage 1: Build the app ----------
FROM gradle:8.4-jdk17 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle build -x test

# ---------- Stage 2: Run the app ----------

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


####

IMAGE_NAME=us-central1-docker.pkg.dev/YOUR_PROJECT_ID/java-docker-repo/springboot-csv-firestore

docker build -t $IMAGE_NAME .

gcloud auth configure-docker us-central1-docker.pkg.dev


docker push $IMAGE_NAME


gcloud run deploy springboot-csv-firestore \
  --image $IMAGE_NAME \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated




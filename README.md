gcloud artifacts repositories create java-docker-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="Docker repo for Spring Boot app"


# ---------- Stage 1: Build the app with Gradle and JDK 21 ----------
FROM gradle:8.4-jdk21 AS build

WORKDIR /app

COPY --chown=gradle:gradle . .

RUN gradle build -x test

# ---------- Stage 2: Run the app with Java 21 ----------

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]



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


## PubSub
# Create GCS bucket
gsutil mb -l us-central1 gs://your-bucket-name/

# Enable notifications via Pub/Sub
gcloud pubsub topics create csv-upload-topic

# Create a notification
gsutil notification create -t csv-upload-topic -f json gs://your-bucket-name/


gcloud pubsub subscriptions create csv-upload-subscription \
  --topic=csv-upload-topic \
  --push-endpoint=https://your-cloud-run-url/process-csv \
  --push-auth-service-account=your-service-account@your-project.iam.gserviceaccount.com




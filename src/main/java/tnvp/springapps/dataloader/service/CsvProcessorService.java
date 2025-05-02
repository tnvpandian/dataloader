package tnvp.springapps.dataloader.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tnvp.springapps.dataloader.entities.AuditLogData;
import tnvp.springapps.dataloader.entities.UserData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class CsvProcessorService {

    private static final int BATCH_SIZE = 50;

    private final Storage storage;
    private final Firestore firestore;

    private final String fsUserCollectionName;
    private final String fsErrorCollectionName;
    private final String fsVehicleCollectionName;
    private final String fsAuditCollectionName;

    private final Gson gson;

    public CsvProcessorService( Storage storage, Firestore firestore,
                                @Value("${csv.processor.firestore.collection.user-data}")
                                String fsUserCollectionName,
                                @Value("${csv.processor.firestore.collection.error-data}")
                                String fsErrorCollectionName,
                                @Value("${csv.processor.firestore.collection.vehicle-data}")
                                String fsVehicleCollectionName,
                                @Value("${csv.processor.firestore.collection.audit-data}")
                                String fsAuditCollectionName
                                ) {
        this.storage = storage;
        this.firestore = firestore;
        this.fsUserCollectionName = fsUserCollectionName;
        this.fsVehicleCollectionName = fsVehicleCollectionName;
        this.fsErrorCollectionName = fsErrorCollectionName;
        this.fsAuditCollectionName = fsAuditCollectionName;
        this.gson = new Gson();

        log.info( "CsvProcessorService|CsvProcessorService: Service Initialised  CsvProcessorService " +
                    "fsUserCollectionName: {}, fsVehicleCollectionName: {}, fsErrorCollectionName:{}, fsAuditCollectionName: {}",
                 this.fsUserCollectionName, this.fsVehicleCollectionName, this.fsErrorCollectionName, this.fsAuditCollectionName);
    }

    public void processCsv( String bucketName, String fileName )
            throws IOException, InterruptedException, ExecutionException, CsvValidationException {

        log.info("CsvProcessorService|processCsv: bucketName:{}, fileName:{}", bucketName, fileName);

        AuditLogData auditRecord = new AuditLogData(bucketName, fileName);
        int totalRows = 0;
        int errorRows = 0;
        int loadedRows = 0;
        StringBuilder errorMsg = new StringBuilder();

        Blob blob = storage.get(bucketName, fileName);

        try ( ReadableByteChannel readChannel = blob.reader();
             Reader reader = new InputStreamReader(Channels.newInputStream(readChannel));
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build() ) {

                String[] headers = getHeaders(blob);
                List<ApiFuture<List<WriteResult>>> futures = new ArrayList<>();
                WriteBatch batch = firestore.batch();

                int counter = 0;
                String[] row;
                CollectionReference collection = firestore.collection(this.fsUserCollectionName);

                while ((row = csvReader.readNext()) != null) {
                    totalRows++;
                    Map<String, Object> docData = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        docData.put(headers[i], row[i]);
                    }

                    try {
                        UserData userData = gson.fromJson(gson.toJsonTree(docData), UserData.class);
                        String docId = auditRecord.getUniqueLoadId() + "_" + totalRows;

                        DocumentReference docRef = collection.document(docId); // Auto-ID
                        batch.set(docRef, userData);

                        counter++;

                        if (counter == BATCH_SIZE) {
                            futures.add(batch.commit());
                            batch = firestore.batch();
                            counter = 0;
                        }
                        loadedRows++;

                    } catch( Exception exp ) {
                        errorMsg.append("||").append(exp.getMessage());
                        errorRows++;
                    }
                }

                // Commit remaining records
                if (counter > 0) {
                    futures.add( batch.commit() );
                }

                // Wait for all batch writes to complete
                for (ApiFuture<List<WriteResult>> future : futures) {
                    future.get(); // blocks until batch completes
                }

                log.info("CsvProcessorService|processCsv: CSV File processed");

        } catch (Exception exp) {
            errorMsg.append("||").append(exp.getMessage());
        }
        finally {
            auditRecord.setInputRecordsCount(totalRows);
            auditRecord.setLoadedRecordsCount(loadedRows);
            auditRecord.setErrorRecordsCount(errorRows);
            auditRecord.setMessage(errorMsg.toString());
            log.info("CsvProcessorService|processCsv: auditRecord:{}", auditRecord.toString());

            CollectionReference auditCollection = firestore.collection(this.fsAuditCollectionName);
            WriteBatch batch = firestore.batch();
            DocumentReference auditDocRef = auditCollection.document(); // Auto-ID
            batch.set(auditDocRef, auditRecord);
            batch.commit();

        }

    }

    public String countFSDocs( ) {
        log.info("CsvProcessorService|countFSDocs: Inside countFSDocs");
        CollectionReference collection = firestore.collection(this.fsUserCollectionName);
        int totalDocs = 0;

        for (DocumentReference documentReference : collection.listDocuments()) {
            totalDocs++;
        }
        String retResponse =  "{ Collection: this.fsUserCollectionName" + ",Total Docs: "  + totalDocs+" }";
        log.info("CsvProcessorService|countFSDocs: response {}", retResponse);
        return retResponse;
    }

    private String[] getHeaders(Blob blob) throws IOException, CsvValidationException {
        try (ReadableByteChannel channel = blob.reader();
             Reader reader = new InputStreamReader(Channels.newInputStream(channel));
             CSVReader csvReader = new CSVReader(reader)) {
            return csvReader.readNext();
        }
    }

}


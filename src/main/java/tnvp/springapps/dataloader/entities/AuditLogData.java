package tnvp.springapps.dataloader.entities;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class AuditLogData {

    private String uniqueLoadId;
    private String inputFileName;
    private int inputRecordsCount;
    private int loadedRecordsCount;
    private int errorRecordsCount;
    private String message;

    public AuditLogData(String bucketName, String fileName) {
        this.uniqueLoadId = UUID.randomUUID() + "_" + ( new Date() ).getTime();
        this.inputFileName = bucketName + "|" + fileName;
        this.inputRecordsCount = 0;
        this.loadedRecordsCount = 0;
        this.errorRecordsCount = 0;
        this.message = "";
    }

}

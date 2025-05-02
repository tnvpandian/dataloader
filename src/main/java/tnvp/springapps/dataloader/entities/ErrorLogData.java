package tnvp.springapps.dataloader.entities;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class ErrorLogData {

    private String uniqueLoadId;
    private String inputFileName;
    private String message;
    private String errorData;

    public ErrorLogData(String bucketName, String fileName) {
        this.uniqueLoadId = fileName + "|" + ( new Date() ).getTime() + "|" + UUID.randomUUID();
        this.inputFileName = bucketName + "|" + fileName;
        this.message = "";
    }

}

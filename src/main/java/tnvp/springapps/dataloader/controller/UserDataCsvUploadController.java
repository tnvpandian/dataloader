package tnvp.springapps.dataloader.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tnvp.springapps.dataloader.service.CsvProcessorService;

@Slf4j
@RestController
@RequestMapping("/user-data-csv-controller")
public class UserDataCsvUploadController {

    private final CsvProcessorService csvProcessorService;

    public UserDataCsvUploadController(CsvProcessorService csvProcessorService) {
        log.info("UserDataCsvUploadController Initialised");
        this.csvProcessorService = csvProcessorService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck () {
        return ResponseEntity.ok("Status=OK");
    }

    @PostMapping("/process-csv")
    public ResponseEntity<String> processCsv(
           // @RequestBody Map<String, Object> payload
    ) {
        try {
            /*
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            String data = (String) message.get("data");
            String decoded = new String(Base64.getDecoder().decode(data));
            Map<String, String> attributes = new ObjectMapper().readValue(decoded, Map.class);

            String bucket = attributes.get("bucket");
            String name = attributes.get("name");
             */

            String bucket = "data-migration-csv-files";
            String name = "sample_users_100.csv";
            //name="sample_users_100_error_entries.csv";

            log.info(" processCsv : BucketName:{}, FileName:{} ", bucket, name);

            csvProcessorService.processCsv(bucket, name);

            return ResponseEntity.ok("CSV processed.");
        } catch (Exception e) {
            log.error("Exception while processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/count-fs-docs")
    public ResponseEntity<String> countFSDocs(
            // @RequestBody Map<String, Object> payload
    ) {
        return ResponseEntity.ok(csvProcessorService.countFSDocs());
    }
}

package audionote;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.amazonaws.services.s3.transfer.Upload;

@RestController // This tells Spring that the controller contains actions accessible by URL.
public class TranscriptionController {

    // Get token from environmental variable. See Github for more info.
    private String token = System.getenv("AUDIONOTE_TOKEN");

    /*
     * This function is called when a GET request is sent to the URL
     * ".../transcription". The function returns a small piece of JSON containing a
     * dummy transcription. You can include an "audio" to change what the dummy
     * transcription says. Just add "?audio=anything" to the url to try it out.
     * 
     * @GetMapping is used to tell Spring to call this function when a get request
     * is sent to "/transcription". @PostMapping could be used instead to map POST
     * requests to this function.
     * 
     * @RequestParam maps the parameter "audio" to the String audio. This means that
     * if you inclide an audio parameter in the url, it will be available in this
     * function using the audio variable.
     */
    @PostMapping("/transcription") // Maps this method to the path ".../transcription"
    public ResponseEntity<String> transcribeAudio(@RequestPart(value = "audio") MultipartFile audioFile, @RequestHeader(value = "Token") String token) {

        // Check for token
        if (!token.equals(this.token)) {
            return new ResponseEntity<String>("UNAUTHORIZED!", HttpStatus.UNAUTHORIZED); // returns an UNAUTHORIZED status code
        }

        // Try getting the file
        try {

            // Get file
            File converted = convertMultipartFileToNormalFile(audioFile);

            // Upload to s3
            UploadFile.upload(converted, true);

            // Start transcription
            StartTranscribe.startTranscribe(converted.getName());

            // Send response
            String response = String.format("{ File %s is uploaded! }", converted.getName());
            return new ResponseEntity<String>(response, HttpStatus.OK);

        } catch (Exception exception) {
            exception.printStackTrace();
            return new ResponseEntity<String>("Invalid file", HttpStatus.BAD_REQUEST);
        }
    }

    private File convertMultipartFileToNormalFile(MultipartFile multipartFile) throws IOException {
        File newFile = new File(multipartFile.getOriginalFilename());
        FileOutputStream stream = new FileOutputStream(newFile);
        stream.write(multipartFile.getBytes());
        stream.close();
        return newFile;
    }
}
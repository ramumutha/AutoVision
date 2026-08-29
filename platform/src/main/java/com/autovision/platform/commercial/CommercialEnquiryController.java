package com.autovision.platform.commercial;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/public/commercial-enquiries")
public class CommercialEnquiryController {
    private final CommercialEnquiryService service;

    public CommercialEnquiryController(CommercialEnquiryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody CommercialEnquiryRequest request, HttpServletRequest httpRequest) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.submit(request, httpRequest.getRemoteAddr()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse("REQUEST_REJECTED"));
        }
    }

    @GetMapping("/verify")
    public VerificationResponse verify(@RequestParam String token) {
        return service.verify(token) ? new VerificationResponse("VERIFIED") : new VerificationResponse("INVALID_OR_EXPIRED");
    }

    public record VerificationResponse(String status) { }
    public record ErrorResponse(String error) { }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> internalFailure() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("REQUEST_FAILED"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidRequest() {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST"));
    }
}

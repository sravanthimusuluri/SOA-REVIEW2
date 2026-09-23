package com.bibliotech.rental.client;

import com.bibliotech.rental.dto.CalculateFineRequest;
import com.bibliotech.rental.dto.FineResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "fine-service")
public interface FineClient {

    @PostMapping("/api/fines/calculate")
    FineResponse calculateFine(@RequestBody CalculateFineRequest request);

    @GetMapping("/api/fines/student/{studentId}")
    List<FineResponse> getFinesByStudentId(@PathVariable("studentId") String studentId);
}

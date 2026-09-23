package com.bibliotech.rental.client;

import com.bibliotech.rental.dto.AvailabilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "book-service")
public interface BookClient {

    @GetMapping("/api/books/{id}/availability")
    AvailabilityResponse checkAvailability(@PathVariable("id") Long id);

    @PostMapping("/api/books/{id}/decrement")
    AvailabilityResponse decrementCopies(@PathVariable("id") Long id);

    @PostMapping("/api/books/{id}/increment")
    AvailabilityResponse incrementCopies(@PathVariable("id") Long id);
}

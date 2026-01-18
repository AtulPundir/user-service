package com.myapp.userservice.util;

import com.fasterxml.uuid.Generators;
import org.springframework.stereotype.Component;

@Component
public class CuidGenerator {

    /**
     * Generates a unique identifier similar to CUID format.
     * Uses time-based UUID v7 for better database performance (sequential ordering).
     */
    public String generate() {
        // Generate time-based UUID and convert to a shorter format
        return Generators.timeBasedEpochGenerator().generate().toString().replace("-", "").substring(0, 25);
    }
}

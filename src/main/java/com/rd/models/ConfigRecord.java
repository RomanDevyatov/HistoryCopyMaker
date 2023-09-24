package com.rd.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConfigRecord {
    private String username;
    private String browserType;
    private String pathOverwritten;
    private boolean logging;
}

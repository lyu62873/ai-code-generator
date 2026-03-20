package com.leyu.aicodegenerator.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Method used by this component. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatGuardResult {
    private String action; // PASS | REJECT_NOISE | REJECT_SWITCH_MODE | REJECT_ATTACK
    private String reason;
    private String reply;
}
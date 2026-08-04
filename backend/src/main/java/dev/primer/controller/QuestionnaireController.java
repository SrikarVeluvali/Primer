package dev.primer.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.primer.dto.response.QuestionnaireResponse;
import dev.primer.service.questionnaire.QuestionnaireService;
import dev.primer.service.questionnaire.QuestionnaireView;

@RestController
@RequestMapping("/api/papers/{id}/questionnaire")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    public QuestionnaireController(QuestionnaireService questionnaireService) {
        this.questionnaireService = questionnaireService;
    }

    /**
     * Returns 202 while the questionnaire is still being generated so the caller knows to poll, and
     * 200 with the document once it is ready.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionnaireResponse> getQuestionnaire(@PathVariable UUID id) {
        QuestionnaireView view = questionnaireService.find(id);
        HttpStatus status = view.isReady() || view.error() != null ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(QuestionnaireResponse.from(view));
    }

    @PostMapping("/skip")
    public ResponseEntity<Void> skipQuestionnaire(@PathVariable UUID id) {
        questionnaireService.skip(id);
        return ResponseEntity.noContent().build();
    }
}

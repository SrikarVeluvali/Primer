package dev.primer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.primer.entity.AnalysisStatus;
import dev.primer.exception.ApiExceptionHandler;
import dev.primer.exception.PaperNotFoundException;
import dev.primer.service.questionnaire.QuestionnaireService;
import dev.primer.service.questionnaire.QuestionnaireView;

@WebMvcTest(QuestionnaireController.class)
@Import(ApiExceptionHandler.class)
class QuestionnaireControllerTest {

    private static final String QUESTIONNAIRE_JSON = """
            {"paperTitle":"A paper","intro":"A few questions.","questions":[]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionnaireService questionnaireService;

    private final UUID paperId = UUID.randomUUID();

    @Test
    void returnsAcceptedWhileTheQuestionnaireIsStillBeingGenerated() throws Exception {
        when(questionnaireService.find(paperId))
                .thenReturn(new QuestionnaireView(AnalysisStatus.GENERATING, null, null));

        mockMvc.perform(get("/api/papers/{id}/questionnaire", paperId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("GENERATING"))
                .andExpect(jsonPath("$.questionnaire").doesNotExist());
    }

    @Test
    void servesTheStoredQuestionnaireUnchangedOnceItIsReady() throws Exception {
        when(questionnaireService.find(paperId))
                .thenReturn(new QuestionnaireView(AnalysisStatus.READY, QUESTIONNAIRE_JSON, null));

        mockMvc.perform(get("/api/papers/{id}/questionnaire", paperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.questionnaire.paperTitle").value("A paper"));
    }

    @Test
    void reportsAFailedGenerationWithItsReason() throws Exception {
        when(questionnaireService.find(paperId))
                .thenReturn(new QuestionnaireView(AnalysisStatus.FAILED, null, "the model gave up"));

        mockMvc.perform(get("/api/papers/{id}/questionnaire", paperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("the model gave up"));
    }

    @Test
    void returnsNotFoundForAnUnknownPaper() throws Exception {
        when(questionnaireService.find(any())).thenThrow(new PaperNotFoundException("No paper exists."));

        mockMvc.perform(get("/api/papers/{id}/questionnaire", paperId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No paper exists."));
    }

    @Test
    void recordsThatTheReaderSkippedTheQuestionnaire() throws Exception {
        mockMvc.perform(post("/api/papers/{id}/questionnaire/skip", paperId))
                .andExpect(status().isNoContent());

        verify(questionnaireService).skip(paperId);
    }

    @Test
    void returnsNotFoundWhenSkippingAnUnknownPaper() throws Exception {
        doThrow(new PaperNotFoundException("No paper exists.")).when(questionnaireService).skip(any());

        mockMvc.perform(post("/api/papers/{id}/questionnaire/skip", paperId))
                .andExpect(status().isNotFound());
    }
}

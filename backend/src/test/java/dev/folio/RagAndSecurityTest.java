package dev.folio;

import com.fasterxml.jackson.databind.*;
import dev.folio.auth.*;
import dev.folio.chat.*;
import dev.folio.ollama.OllamaClient;
import dev.folio.paper.*;
import dev.folio.retrieval.*;
import dev.folio.personal.*;
import dev.folio.learn.*;
import static dev.folio.learn.LearningTypes.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:folio-test;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "folio.data-directory=./target/test-papers"})
@AutoConfigureMockMvc
class RagAndSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AccountRepository accounts;
    @Autowired PaperRepository papers;
    @Autowired ChunkRepository chunks;
    @Autowired ChatRepository chats;
    @Autowired PersonalDocumentRepository personalDocuments;
    @Autowired dev.folio.annotation.HighlightRepository highlights;
    @MockitoBean OllamaClient ollama;
    @MockitoBean ResourceDiscovery discovery;
    private Paper paper;
    @BeforeEach void setup() {
        highlights.deleteAll(); personalDocuments.deleteAll(); chats.deleteAll(); chunks.deleteAll(); papers.deleteAll(); accounts.deleteAll();
        paper = new Paper("1706.03762", "test-embed"); paper.status = "READY"; paper.title = "Attention Is All You Need";
        paper.indexVersion = PaperService.INDEX_VERSION;
        paper.pageCount=2;
        papers.save(paper); chunks.save(new Chunk(paper.id, 2, 0, "The Transformer uses self-attention instead of recurrence. The proposed architecture includes encoder and decoder layers, parallel computation, positional encodings, and experiments on machine translation tasks with improved performance.", "[1.0,0.0]"));
        when(ollama.embed(anyString(), anyList())).thenReturn(List.of(new double[]{1, 0}));
        when(ollama.chat(anyString(), anyList())).thenReturn("The model uses self-attention [1].");
    }
    private MockHttpSession register(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name", "Researcher", "email", email, "password", "a-good-password-42"))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
    @Test void papersAreSharedButQuestionsAnswersAndDeletionArePrivate() throws Exception {
        var alice = register("alice@example.org"); var bob = register("bob@example.org");
        mvc.perform(get("/api/papers").session(alice)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(paper.id.toString()));
        mvc.perform(get("/api/papers").session(bob)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(paper.id.toString()));
        var path = "/api/papers/" + paper.id + "/chat";
        mvc.perform(post(path).session(alice).with(csrf()).contentType("application/json").content("{\"question\":\"Alice private question\",\"model\":\"test-chat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].page").value(2));
        mvc.perform(get(path).session(bob)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(post(path).session(bob).with(csrf()).contentType("application/json").content("{\"question\":\"Bob private question\",\"model\":\"test-chat\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(path).session(alice)).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].question").value("Alice private question"));
        var prompts = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(ollama, times(2)).chat(eq("test-chat"), prompts.capture());
        assertThat(prompts.getAllValues().get(1).toString()).doesNotContain("Alice private question");
        mvc.perform(delete(path).session(bob).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(path).session(alice)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(path).session(bob)).andExpect(content().json("[]"));
    }
    @Test void requiresAuthenticationAndCsrfAndHashesPasswords() throws Exception {
        mvc.perform(get("/api/papers")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")) .andExpect(status().isForbidden());
        var session = register("reader@example.org");
        assertThat(accounts.findByEmail("reader@example.org").orElseThrow().passwordHash).startsWith("$2a$").isNotEqualTo("a-good-password-42");
        mvc.perform(get("/api/auth/session").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value("reader@example.org")).andExpect(jsonPath("$.csrfToken").isNotEmpty());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"reader@example.org\",\"password\":\"wrong-password\"}")) .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"reader@example.org\",\"password\":\"a-good-password-42\"}")) .andExpect(status().isOk());
    }
    @Test void refusesChatUntilTheSharedIndexIsReady() throws Exception {
        var session = register("wait@example.org"); paper.status = "INDEXING"; papers.save(paper);
        mvc.perform(post("/api/papers/" + paper.id + "/chat").session(session).with(csrf()).contentType("application/json").content("{\"question\":\"What is this about?\",\"model\":\"test-chat\"}")) .andExpect(status().isBadRequest());
        verify(ollama, never()).chat(anyString(), anyList());
    }
    @Test void correctedSummaryDoesNotReuseAnswersFromAnOldIndex() throws Exception {
        var session = register("summary@example.org");
        var user = accounts.findByEmail("summary@example.org").orElseThrow();
        chats.save(new ChatTurn(user.id, paper.id, "Summarize this paper", "old corrupted diagram refusal", "[]", "test-chat"));
        mvc.perform(post("/api/papers/" + paper.id + "/chat").session(session).with(csrf()).contentType("application/json")
                .content("{\"question\":\"Summarize the main contribution and key findings of this paper.\",\"model\":\"test-chat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indexVersion").value(PaperService.INDEX_VERSION));
        var prompt = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(ollama).chat(eq("test-chat"), prompt.capture());
        assertThat(prompt.getValue().toString()).contains("self-attention", "Attention Is All You Need").doesNotContain("old corrupted diagram refusal");
        assertThat(chats.findByUserIdAndPaperIdOrderByCreatedAtAsc(user.id, paper.id)).hasSize(2);
    }
    @Test void notesAndCustomThemesArePrivateAndThemeInputsAreValidated() throws Exception {
        var alice=register("notes-alice@example.org"); var bob=register("notes-bob@example.org");
        String path="/api/papers/"+paper.id+"/notes";
        mvc.perform(put(path).session(alice).with(csrf()).contentType("application/json").content("{\"text\":\"Alice private margin note\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(path).session(bob)).andExpect(jsonPath("$.text").value(""));
        mvc.perform(get(path).session(alice)).andExpect(jsonPath("$.text").value("Alice private margin note"));
        var theme=new PersonalController.Theme("ocean","Ocean","dark","#101828","#182838","#142030","#eeeeee","#bbbbbb","#99aaff","#334455");
        var preferences=new PersonalController.Preferences("ocean",List.of(theme));
        mvc.perform(put("/api/preferences").session(alice).with(csrf()).contentType("application/json").content(json.writeValueAsString(preferences))).andExpect(status().isOk());
        mvc.perform(get("/api/preferences").session(alice)).andExpect(jsonPath("$.activeTheme").value("ocean")).andExpect(jsonPath("$.themes[0].name").value("Ocean"));
        mvc.perform(get("/api/preferences").session(bob)).andExpect(jsonPath("$.activeTheme").value("dark")).andExpect(jsonPath("$.themes.length()").value(0));
        var invalid=new PersonalController.Theme("bad","Bad","dark","url(javascript:alert(1))","#182838","#142030","#eeeeee","#bbbbbb","#99aaff","#334455");
        mvc.perform(put("/api/preferences").session(alice).with(csrf()).contentType("application/json").content(json.writeValueAsString(new PersonalController.Preferences("bad",List.of(invalid))))).andExpect(status().isBadRequest());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @Test void learningQuestionnairePlanAndProgressBelongToTheAuthenticatedAccount() throws Exception {
        var alice=register("learn-alice@example.org");var bob=register("learn-bob@example.org");
        var options=List.of(new Option("a","Correct concept"),new Option("b","Alternative concept"),new Option("c","Other concept"),new Option("d","Not sure"));
        var topics=List.of(new Topic("matrix","Matrix multiplication","Used in attention","What does matrix multiplication represent?",options,"a","It combines linear transformations."),
                new Topic("network","Neural networks","The model architecture","What are network weights?",options,"a","Learned parameters."),
                new Topic("probability","Probability","Softmax scores","What does softmax normalize?",options,"a","A distribution of scores."),
                new Topic("attention","Attention mechanisms","The main method","What determines attention weights?",options,"a","Query and key similarity."));
        var assessment=new Assessment("Four prerequisites for the paper.",topics);
        var steps=new ArrayList<Step>();for(int i=0;i<4;i++)steps.add(new Step("model-step-"+i,"Stage "+i,"Learn a concept","Work through an example","Explain the result",30,List.of("Attention mechanisms")));
        var generated=Map.of("summary","Start with matrix multiplication, then build toward attention.","prerequisites",List.of(new Prerequisite("Matrices","Required for projections","learn"),new Prerequisite("Probability","Required for softmax","review"),new Prerequisite("Networks","Required for architecture","comfortable")),"roadmap",steps);
        when(ollama.structured(eq("test-chat"),anyString(),anyString(),anyMap())).thenReturn(json.valueToTree(assessment),json.valueToTree(generated));
        when(discovery.find(anyString(),anyString())).thenReturn(new ResourceDiscovery.Result(List.of(new Resource("test-resource","Matrix lesson","https://example.org/matrix","blog","A public topic lesson","Matrix multiplication","Web search")),"Test search"));
        String base="/api/papers/"+paper.id+"/learn";
        mvc.perform(put("/api/papers/"+paper.id+"/tags").session(alice).with(csrf()).contentType("application/json").content("{\"tags\":[\"Neural networks\"]}")) .andExpect(status().isOk());
        mvc.perform(post("/api/knowledge/tags").session(alice).with(csrf()).contentType("application/json").content("{\"name\":\"Neural networks\",\"level\":3,\"interested\":true}")) .andExpect(status().isOk());
        mvc.perform(post(base+"/assessment").session(alice).with(csrf()).contentType("application/json").content("{\"model\":\"test-chat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assessment.topics.length()").value(4)).andExpect(jsonPath("$.answers.confidence.network").value(3));
        mvc.perform(get(base).session(bob)).andExpect(jsonPath("$.assessment").isEmpty());
        var answers=new Answers(Map.of(),Map.of("matrix",0,"network",3,"probability",1,"attention",2),"Private goal: explain my research",30,"mixed");
        mvc.perform(post(base+"/plan").session(alice).with(csrf()).contentType("application/json").content(json.writeValueAsString(new LearningController.PlanInput(answers,"test-chat"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan.roadmap.length()").value(4)).andExpect(jsonPath("$.plan.prerequisites[0].readiness").value("learn"))
                .andExpect(jsonPath("$.plan.prerequisites[1].readiness").value("comfortable")).andExpect(jsonPath("$.plan.prerequisites[2].readiness").value("review"));
        mvc.perform(get("/api/knowledge").session(alice)).andExpect(jsonPath("$.tags.length()").value(4));
        mvc.perform(get("/api/knowledge").session(bob)).andExpect(jsonPath("$.tags.length()").value(0));
        mvc.perform(get(base).session(alice)).andExpect(jsonPath("$.assessment.topics[0].question").doesNotExist()).andExpect(jsonPath("$.assessment.topics[0].correctOption").doesNotExist());
        verify(discovery).find("Matrix multiplication","Attention Is All You Need");
        mvc.perform(put(base+"/progress").session(alice).with(csrf()).contentType("application/json").content("{\"completedSteps\":[\"step-1\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedSteps[0]").value("step-1"));
        mvc.perform(get(base).session(bob)).andExpect(jsonPath("$.plan").isEmpty()).andExpect(jsonPath("$.completedSteps.length()").value(0));
        mvc.perform(put(base+"/progress").session(bob).with(csrf()).contentType("application/json").content("{\"completedSteps\":[\"step-1\"]}")) .andExpect(status().isBadRequest());
        mvc.perform(put(base+"/progress").session(alice).with(csrf()).contentType("application/json").content("{\"completedSteps\":[\"not-a-step\"]}")) .andExpect(status().isBadRequest());
    }
    @Test void sharedPaperTagsProducePrivateExplainableRecommendations() throws Exception {
        var alice=register("tags-alice@example.org");var bob=register("tags-bob@example.org");
        String path="/api/papers/"+paper.id+"/tags";
        mvc.perform(put(path).session(alice).with(csrf()).contentType("application/json").content("{\"tags\":[\"Attention mechanisms\",\"Neural networks\",\"attention mechanisms\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tags.length()").value(2));
        mvc.perform(get("/api/papers").session(bob)).andExpect(jsonPath("$[0].tags[0]").value("Attention mechanisms"));
        mvc.perform(post("/api/knowledge/tags").session(alice).with(csrf()).contentType("application/json").content("{\"name\":\"Neural networks\",\"level\":2,\"interested\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recommendations").session(alice)).andExpect(jsonPath("$[0].paper.id").value(paper.id.toString())).andExpect(jsonPath("$[0].reasons[0]").value("Matches your interests: Neural networks"));
        mvc.perform(get("/api/recommendations").session(bob)).andExpect(content().json("[]"));
        var second=new Paper("1810.04805","test-embed");second.status="READY";second.tags=new ArrayList<>(List.of("Neural networks","Language models"));papers.save(second);
        mvc.perform(get("/api/topics").session(bob)).andExpect(jsonPath("$[0].name").value("Neural networks")).andExpect(jsonPath("$[0].paperCount").value(2));
        mvc.perform(post("/api/knowledge/tags").session(alice).with(csrf()).contentType("application/json").content("{\"name\":\"Unlisted topic\",\"level\":2,\"interested\":true}")) .andExpect(status().isBadRequest());
        mvc.perform(post("/api/knowledge/tags").session(alice).with(csrf()).contentType("application/json").content("{\"name\":\"Private invalid rating\",\"level\":8}")) .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/knowledge/tags/neural-networks").session(bob).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/knowledge").session(alice)).andExpect(jsonPath("$.tags.length()").value(1));
        mvc.perform(delete("/api/knowledge/tags/neural-networks").session(alice).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/recommendations").session(alice)).andExpect(content().json("[]"));
        mvc.perform(put(path).session(alice).contentType("application/json").content("{\"tags\":[]}")) .andExpect(status().isForbidden());
    }
    @Test void highlightsArePrivateAndExplanationsPinTheSelectedPassage() throws Exception {
        var alice=register("highlight-alice@example.org");var bob=register("highlight-bob@example.org");
        String base="/api/papers/"+paper.id+"/annotations";
        String body="{\"page\":2,\"text\":\"Private selected passage about query and key vectors.\",\"color\":\"amber\",\"rectangles\":[{\"x\":0.1,\"y\":0.2,\"width\":0.3,\"height\":0.02}]}";
        var result=mvc.perform(post(base).session(alice).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        String id=json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(get(base).session(bob)).andExpect(content().json("[]"));
        mvc.perform(post(base+"/"+id+"/explain").session(bob).with(csrf()).contentType("application/json").content("{\"model\":\"test-chat\"}")) .andExpect(status().isNotFound());
        mvc.perform(delete(base+"/"+id).session(bob).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(base+"/"+id+"/explain").session(alice).with(csrf()).contentType("application/json").content("{\"model\":\"test-chat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].page").value(2)).andExpect(jsonPath("$.sources[0].text").value("Private selected passage about query and key vectors."));
        var prompt=org.mockito.ArgumentCaptor.forClass(List.class); verify(ollama).chat(eq("test-chat"),prompt.capture());
        assertThat(prompt.getValue().toString()).contains("highlighted passage [1]","query and key vectors","The Transformer uses self-attention");
        mvc.perform(get("/api/papers/"+paper.id+"/chat").session(bob)).andExpect(content().json("[]"));
        mvc.perform(post(base).session(alice).with(csrf()).contentType("application/json").content(body.replace("0.3","1.3"))) .andExpect(status().isBadRequest());
        mvc.perform(post(base).session(alice).with(csrf()).contentType("application/json").content(body.replace("\"page\":2","\"page\":10"))) .andExpect(status().isBadRequest());
        mvc.perform(delete(base+"/"+id).session(alice).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(base).session(alice)).andExpect(content().json("[]"));
    }
}

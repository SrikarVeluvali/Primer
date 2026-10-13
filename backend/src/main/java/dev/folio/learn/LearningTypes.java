package dev.folio.learn;

import java.util.*;
import java.time.Instant;

public final class LearningTypes {
    private LearningTypes() {}
    public record Option(String id,String text) {}
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public record Topic(String id,String name,String why,String question,List<Option> options,String correctOption,String explanation) {
        public Topic(String id,String name,String why) { this(id,name,why,null,null,null,null); }
    }
    public record Assessment(String overview,List<Topic> topics) {}
    public record Answers(Map<String,String> answers,Map<String,Integer> confidence,String goal,int minutesPerDay,String preference) {}
    public record Prerequisite(String name,String reason,String readiness) {}
    public record Step(String id,String title,String goal,String activity,String checkpoint,int minutes,List<String> topics) {}
    public record Resource(String id,String title,String url,String type,String description,String topic,String origin) {}
    public record Plan(String summary,List<Prerequisite> prerequisites,List<Step> roadmap,List<Resource> resources,String resourceStatus,Instant generatedAt) {}
    public record State(Assessment assessment,Answers answers,Plan plan,Set<String> completedSteps) {
        public static State empty() { return new State(null,null,null,Set.of()); }
    }
}

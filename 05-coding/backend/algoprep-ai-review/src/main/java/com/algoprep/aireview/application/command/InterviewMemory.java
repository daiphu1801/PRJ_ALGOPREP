package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.OpenInterviewSessionCommand;
import com.algoprep.aireview.application.ports.out.InterviewChatMemoryPort;
import com.algoprep.aireview.application.ports.out.ProblemContextPort;
import com.algoprep.aireview.application.ports.out.SubmissionSnapshotPort;
import com.algoprep.aireview.domain.exception.AiNotOwnerException;
import com.algoprep.aireview.domain.model.ChatMessage;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.ProblemContext;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.domain.ports.out.InterviewSessionRepository;
import com.algoprep.aireview.domain.prompt.BlockName;
import com.algoprep.aireview.domain.prompt.DataBlock;
import com.algoprep.aireview.domain.prompt.ProblemGuidance;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * BR-AI-10: reads the Redis memory and rebuilds it from {@code interview_turns} when the key is
 * missing (restart, eviction, TTL) or Redis is down. The context entries are rebuilt from stored
 * session data (question text, topics) or, for a SUBMISSION entry, from the two upstream ports.
 * Every entry is data; nothing here is ever placed in the system message.
 */
@Component
class InterviewMemory {
  private final InterviewChatMemoryPort memory;
  private final InterviewSessionRepository sessions;
  private final SubmissionSnapshotPort submissions;
  private final ProblemContextPort problems;

  InterviewMemory(
      InterviewChatMemoryPort memory,
      InterviewSessionRepository sessions,
      SubmissionSnapshotPort submissions,
      ProblemContextPort problems) {
    this.memory = memory;
    this.sessions = sessions;
    this.submissions = submissions;
    this.problems = problems;
  }

  /** What the opening prompt needs: the author guidance (SUBMISSION only) and the context. */
  record Opening(ProblemGuidance guidance, List<ChatMessage> context) {}

  /**
   * Context of a new session. A SUBMISSION entry checks ownership here, because the serving side
   * checks none (DD F4 P4); QUESTION_BANK and TOPIC_SELECTION read nothing upstream.
   */
  Opening openingFor(OpenInterviewSessionCommand c) {
    return switch (c.entryType()) {
      case SUBMISSION -> {
        SubmissionSnapshot source = submissions.getAccepted(c.submissionId());
        if (!c.callerId().equals(source.userId())) {
          throw new AiNotOwnerException();
        }
        ProblemContext problem = problems.getContext(source.problemId(), source.language());
        yield new Opening(
            new ProblemGuidance(problem.aiBrief(), problem.noFullCode(), problem.socraticOnly()),
            submissionContext(source, problem));
      }
      case QUESTION_BANK -> new Opening(null, questionContext(c.questionText()));
      case TOPIC_SELECTION -> new Opening(null, topicsContext(c.topics()));
    };
  }

  /**
   * Author guidance for a turn of a SUBMISSION session, which is not kept in the memory; null for
   * the other entries. ponytail: two upstream reads per turn, cache only if they ever show up in a
   * latency budget.
   */
  ProblemGuidance guidanceFor(InterviewSession session) {
    if (session.entryType() != InterviewEntryType.SUBMISSION) {
      return null;
    }
    SubmissionSnapshot source = submissions.getAccepted(session.submissionId());
    ProblemContext problem = problems.getContext(source.problemId(), source.language());
    return new ProblemGuidance(problem.aiBrief(), problem.noFullCode(), problem.socraticOnly());
  }

  /** The stored memory, or a rebuilt one (which is also written back). */
  List<ChatMessage> load(InterviewSession session) {
    return memory.read(session.id()).orElseGet(() -> rebuild(session));
  }

  void write(UUID sessionId, List<ChatMessage> messages) {
    memory.write(sessionId, messages);
  }

  private List<ChatMessage> rebuild(InterviewSession session) {
    List<ChatMessage> out = new ArrayList<>(context(session));
    for (InterviewTurn turn : sessions.turns(session.id())) {
      out.add(
          new ChatMessage(
              turn.role() == InterviewTurn.Role.USER ? ChatMessage.Kind.USER : ChatMessage.Kind.AI,
              turn.content()));
    }
    memory.write(session.id(), out);
    return out;
  }

  private List<ChatMessage> context(InterviewSession session) {
    return switch (session.entryType()) {
      case SUBMISSION -> {
        SubmissionSnapshot source = submissions.getAccepted(session.submissionId());
        yield submissionContext(source, problems.getContext(source.problemId(), source.language()));
      }
      case QUESTION_BANK -> questionContext(session.questionText());
      case TOPIC_SELECTION -> topicsContext(session.topics());
    };
  }

  static List<ChatMessage> submissionContext(SubmissionSnapshot source, ProblemContext context) {
    return List.of(
        new ChatMessage(ChatMessage.Kind.PROBLEM, ReviewLlmCall.problemText(context)),
        new ChatMessage(ChatMessage.Kind.USER_CODE, source.sourceCode()));
  }

  static List<ChatMessage> questionContext(String questionText) {
    return List.of(new ChatMessage(ChatMessage.Kind.QUESTION, questionText));
  }

  static List<ChatMessage> topicsContext(List<String> topics) {
    return List.of(new ChatMessage(ChatMessage.Kind.TOPICS, String.join(", ", topics)));
  }

  /** Context entries become named blocks; the USER and AI lines become one transcript block. */
  static List<DataBlock> blocks(List<ChatMessage> messages) {
    List<DataBlock> blocks = new ArrayList<>();
    StringBuilder transcript = new StringBuilder();
    for (ChatMessage m : messages) {
      switch (m.kind()) {
        case PROBLEM -> blocks.add(new DataBlock(BlockName.PROBLEM, m.content()));
        case USER_CODE -> blocks.add(new DataBlock(BlockName.USER_CODE, m.content()));
        case QUESTION -> blocks.add(new DataBlock(BlockName.QUESTION, m.content()));
        case TOPICS -> blocks.add(new DataBlock(BlockName.TOPICS, m.content()));
        case USER -> transcript.append("Candidate: ").append(m.content()).append('\n');
        case AI -> transcript.append("Interviewer: ").append(m.content()).append('\n');
      }
    }
    if (!transcript.isEmpty()) {
      blocks.add(new DataBlock(BlockName.TRANSCRIPT, transcript.toString().stripTrailing()));
    }
    return blocks;
  }
}

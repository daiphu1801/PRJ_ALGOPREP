"""Materialize reviewed actor class designs as draw.io skill specs."""

from __future__ import annotations

import csv
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "08-diagram/class"
MANIFEST = BASE / "diagram_manifest.csv"
COVERAGE = BASE / "actor_coverage.csv"

# Each operation follows the order of the acceptance criteria in actor_coverage.csv.
# Names and signatures are proposed application design, traced to RD/DD in each spec.
DESIGNS = {
    "a1_problem_discovery": (
        "A1 — Tìm bài và xem bài được giao", "ProblemDiscoveryService",
        ["+ search(filters : ProblemFilter, studentId : UUID) : Page<ProblemSummary>",
         "+ assignedToStudent(studentId : UUID) : List<AssignedProblem>"],
        [("ProblemReadRepository", "+ findPublished(filters : ProblemFilter) : Page<Problem>"),
         ("AssignmentReadPort", "+ forStudent(studentId : UUID) : List<AssignedProblem>")],
        "Chỉ bài PUBLISHED, chưa xoá mới nằm trong kết quả tìm; bài giao nhóm riêng theo lớp."),
    "a1_submission": (
        "A1 — Chạy thử, nộp bài và xem kết quả", "StudentSubmissionService",
        ["+ getStarterCode(problemId : UUID, language : Language) : String",
         "+ runSample(command : RunSampleCommand) : SampleResult",
         "+ getSubmissionModels(problemId : UUID) : Set<SubmissionModel>",
         "+ getSupportedModels(problemId : UUID) : Set<SubmissionModel>",
         "+ acceptSource(command : SourceCommand) : String",
         "+ submit(command : SubmitCommand) : UUID",
         "+ subscribeResults(submissionId : UUID) : ResultStream",
         "+ getCaseSummaries(submissionId : UUID) : List<CaseSummary>",
         "+ getPublicResult(submissionId : UUID) : SubmissionResult",
         "+ getAcceptedActions(submissionId : UUID) : Set<NextAction>"],
        [("ProblemSpecReadPort", "+ getPublicSpec(problemId : UUID) : ProblemSpec"),
         ("SampleRunPort", "+ executeSample(command : RunSampleCommand) : SampleResult"),
         ("SubmissionPort", "+ enqueue(command : SubmitCommand) : UUID"),
         ("ResultReadPort", "+ getPublicResult(id : UUID) : SubmissionResult")],
        "Chạy thử không ghi tiến độ; kết quả Hidden chỉ có trạng thái/chỉ số. Tự giữ mã khi đổi mô hình và chọn nút sau Accepted thuộc client."),
    "a1_profile_progress": (
        "A1 — Tiến độ, hồ sơ và cài đặt", "StudentAccountService",
        ["+ progress(studentId : UUID) : ProgressSummary",
         "+ interviewHistory(studentId : UUID) : List<InterviewRecord>",
         "+ updateProfile(studentId : UUID, profile : Profile)",
         "+ deactivate(studentId : UUID)",
         "+ changePassword(studentId : UUID, current : String, next : String)",
         "+ submissions(studentId : UUID, filter : SubmissionFilter) : Page<SubmissionSummary>",
         "+ saveWorkspaceSettings(studentId : UUID, settings : WorkspaceSettings)",
         "+ saveEmailPreferences(studentId : UUID, preferences : EmailPreferences)",
         "+ exportPersonalData(studentId : UUID, format : ExportFormat) : ExportFile"],
        [("UserRepository", "+ findById(id : UUID) : UserAccount"),
         ("ProgressReadPort", "+ summary(studentId : UUID) : ProgressSummary"),
         ("SubmissionHistoryPort", "+ search(studentId : UUID, filter : SubmissionFilter) : Page<SubmissionSummary>"),
         ("InterviewHistoryPort", "+ list(studentId : UUID) : List<InterviewRecord>")],
        "Dữ liệu trả theo chính studentId; xoá tài khoản là vô hiệu hoá rồi ẩn danh sau ân hạn. Email nhắc lịch là tác vụ nền."),
    "a1_ai_review": (
        "A1 — Phân tích bài giải đã Accepted", "SolutionReviewService",
        ["+ requestReview(submissionId : UUID, studentId : UUID) : ReviewReport",
         "+ findCached(sourceHash : String) : ReviewReport",
         "+ getReport(submissionId : UUID, studentId : UUID) : ReviewReport",
         "+ proposedPatch(reportId : UUID, studentId : UUID) : SuggestedCode"],
        [("AcceptedSubmissionPort", "+ requireAccepted(id : UUID, studentId : UUID) : Submission"),
         ("ReviewReportRepository", "+ findByHash(hash : String) : ReviewReport"),
         ("ReviewModelPort", "+ analyze(context : ReviewContext) : ReviewReport")],
        "Báo cáo có độ phức tạp, so tối ưu, trường hợp biên, chất lượng mã và câu hỏi mở rộng. Xác nhận ghi đè/khôi phục mã thuộc Workspace."),
    "a1_mock_interview": (
        "A1 — Phỏng vấn giả lập ba giai đoạn", "MockInterviewService",
        ["+ startFromAccepted(submissionId : UUID, studentId : UUID) : InterviewSession",
         "+ advanceStage(sessionId : UUID) : InterviewStage",
         "+ streamReply(sessionId : UUID, answer : String) : TokenStream",
         "+ rubric(sessionId : UUID) : InterviewRubric",
         "+ endOnAiFailure(sessionId : UUID, reason : String)",
         "+ startSelfPractice(source : QuestionSource, studentId : UUID) : InterviewSession",
         "+ applySelfPracticeSettings(sessionId : UUID, settings : PracticeSettings)"],
        [("InterviewSessionRepository", "+ save(session : InterviewSession)"),
         ("InterviewQuestionPort", "+ resolve(source : QuestionSource) : QuestionContext"),
         ("InterviewModelPort", "+ respond(context : StageContext) : TokenStream"),
         ("AcceptedSubmissionPort", "+ requireAccepted(id : UUID, studentId : UUID) : Submission")],
        "Ba giai đoạn và rubric lưu cùng phiên; lỗi AI kết thúc phiên có kiểm soát, không đổi kết quả chấm."),
    "a1_interview_bank": (
        "A1 — Học và luyện câu hỏi phỏng vấn", "InterviewPracticeService",
        ["+ search(filter : QuestionFilter, studentId : UUID) : Page<Question>",
         "+ study(questionId : UUID) : StudyContent",
         "+ evaluate(questionId : UUID, studentId : UUID, answer : String) : PracticeFeedback",
         "+ progress(studentId : UUID) : PracticeProgress",
         "+ selfRate(questionId : UUID, studentId : UUID, rating : RecallRating)"],
        [("InterviewQuestionRepository", "+ findPublished(filter : QuestionFilter) : Page<Question>"),
         ("PracticeAttemptRepository", "+ save(attempt : PracticeAttempt)"),
         ("AnswerEvaluationPort", "+ evaluate(question : Question, answer : String) : PracticeFeedback")],
        "Chế độ học trả gợi ý/khung chuẩn/từ khoá; tự chấm gần nhất quyết định thứ tự ôn lại."),
    "a1_bookmark": (
        "A1 — Bookmark và ghi chú riêng", "BookmarkService",
        ["+ save(studentId : UUID, problemId : UUID, note : String)",
         "+ getPrivateNote(studentId : UUID, problemId : UUID) : String",
         "+ listMine(studentId : UUID) : List<Bookmark>"],
        [("BookmarkRepository", "+ findByOwnerAndProblem(ownerId : UUID, problemId : UUID) : Bookmark"),
         ("ProblemReadPort", "+ getSummary(problemId : UUID) : ProblemSummary")],
        "Mọi truy vấn ghi chú luôn nhận ownerId từ danh tính đã xác thực; dữ liệu bookmark ở server."),
    "a1_my_classes": (
        "A1 — Tham gia và xem lớp của tôi", "StudentClassService",
        ["+ joinByInvite(studentId : UUID, code : String) : ClassSummary",
         "+ validateInvite(code : String) : InviteStatus",
         "+ joinIdempotently(studentId : UUID, classId : UUID) : Membership",
         "+ listMine(studentId : UUID) : List<ClassSummary>",
         "+ assignments(studentId : UUID, classId : UUID) : List<AssignmentProgress>",
         "+ requireMembership(studentId : UUID, classId : UUID) : Membership"],
        [("ClassRepository", "+ findByInvite(code : String) : Classroom"),
         ("MembershipRepository", "+ find(studentId : UUID, classId : UUID) : Membership"),
         ("AssignmentProgressPort", "+ forClass(studentId : UUID, classId : UUID) : List<AssignmentProgress>")],
        "Mã mời hiệu lực 7 ngày; vào lớp lặp là idempotent; lớp ngoài phạm vi trả cùng kiểu 404."),
    "a2_problem_authoring": (
        "A2 — Soạn và xuất bản bài toán", "ProblemAuthoringService",
        ["+ saveContent(actorId : UUID, draft : ProblemDraft) : Problem",
         "+ saveFunctionSpec(actorId : UUID, spec : FunctionSpecDraft) : ProblemSpec",
         "+ setMatchingStrategy(actorId : UUID, strategy : MatchingStrategy)",
         "+ createUnpublished(actorId : UUID, draft : ProblemDraft) : Problem",
         "+ publish(actorId : UUID, problemId : UUID) : PublishChecklist",
         "+ validatePublish(problemId : UUID) : PublishChecklist",
         "+ withdraw(actorId : UUID, problemId : UUID)"],
        [("ProblemRepository", "+ save(problem : Problem)"),
         ("FunctionSpecEvaluator", "+ evaluate(spec : FunctionSpecDraft) : Evaluation"),
         ("PublishChecklistService", "+ evaluate(problemId : UUID) : PublishChecklist"),
         ("AuthorScopePolicy", "+ requireWrite(actorId : UUID, problemId : UUID)")],
        "Bài mới UNPUBLISHED; xuất bản cần đủ checklist và đáp án mẫu qua toàn bộ testcase duyệt."),
    "a2_testcases": (
        "A2 — Testcase và giới hạn chấm", "TestcaseAuthoringService",
        ["+ saveCase(actorId : UUID, draft : TestcaseDraft) : Testcase",
         "+ importBatch(actorId : UUID, batch : TestcaseBatch) : ImportSummary",
         "+ setLimits(actorId : UUID, problemId : UUID, limits : JudgeLimits)",
         "+ replaceCaseSet(actorId : UUID, problemId : UUID, cases : List<TestcaseDraft>) : int"],
        [("TestcaseRepository", "+ save(testcase : Testcase)"),
         ("TestcaseObjectStore", "+ putBatch(batch : TestcaseBatch) : ObjectRef"),
         ("ProblemRepository", "+ increaseTestcaseVersion(problemId : UUID) : int"),
         ("AuthorScopePolicy", "+ requireWrite(actorId : UUID, problemId : UUID)")],
        "Sample công khai, Hidden bảo mật; sửa bộ ca tăng version, giữ nguyên lịch sử bài nộp cũ."),
    "a2_assignments": (
        "A2 — Giao bài cho lớp", "ClassAssignmentService",
        ["+ assign(instructorId : UUID, classId : UUID, problemId : UUID) : Assignment"],
        [("InstructorClassPort", "+ requireOwner(instructorId : UUID, classId : UUID)"),
         ("ProblemReadRepository", "+ requirePublished(problemId : UUID) : Problem"),
         ("AssignmentRepository", "+ save(assignment : Assignment)")],
        "Chỉ giảng viên phụ trách lớp được giao bài đã công bố; học viên lớp khác không thấy bài giao."),
    "a2_ai_testcases": (
        "A2 — Sinh và duyệt testcase AI", "AiTestcaseService",
        ["+ generateDrafts(actorId : UUID, problemId : UUID) : List<TestcaseDraft>",
         "+ requireReferencePass(problemId : UUID)",
         "+ approve(actorId : UUID, draftId : UUID) : Testcase"],
        [("ReferenceSolutionPort", "+ verifyAll(problemId : UUID) : boolean"),
         ("TestcaseGenerationPort", "+ generate(input : GenerationInput) : List<TestcaseDraft>"),
         ("TestcaseRepository", "+ approve(id : UUID) : Testcase")],
        "Testcase AI ở trạng thái nháp đến khi người có quyền duyệt; không dùng đáp án mẫu làm đầu vào AI."),
    "a2_class_management": (
        "A2 — Quản lý lớp và mã mời", "InstructorClassService",
        ["+ create(instructorId : UUID, draft : ClassDraft) : Classroom",
         "+ currentInvite(instructorId : UUID, classId : UUID) : InviteCode",
         "+ validateInvite(code : String) : InviteStatus",
         "+ delete(instructorId : UUID, classId : UUID)",
         "+ expireInvite(classId : UUID)",
         "+ regenerateInvite(instructorId : UUID, classId : UUID) : InviteCode",
         "+ removeStudent(instructorId : UUID, classId : UUID, studentId : UUID)",
         "+ details(instructorId : UUID, classId : UUID) : ClassDetail"],
        [("ClassRepository", "+ findById(classId : UUID) : Classroom"),
         ("MembershipRepository", "+ remove(classId : UUID, studentId : UUID)"),
         ("InviteCodeRepository", "+ replace(classId : UUID) : InviteCode")],
        "Chỉ giảng viên phụ trách được sửa; xoá lớp phát sự kiện cho bài giao, mã mời cũ hết hiệu lực."),
    "a2_class_progress": (
        "A2 — Tiến độ lớp và học viên", "ClassProgressService",
        ["+ studentDetail(instructorId : UUID, classId : UUID, studentId : UUID) : StudentProgress",
         "+ requireInstructorScope(instructorId : UUID, classId : UUID)",
         "+ overview(instructorId : UUID) : ClassProgressOverview",
         "+ filterClass(instructorId : UUID, classId : UUID) : ClassProgress",
         "+ atRiskStudents(instructorId : UUID, classId : UUID) : List<StudentProgress>",
         "+ emptyOverview(instructorId : UUID) : ClassProgressOverview"],
        [("InstructorClassPort", "+ requireOwner(instructorId : UUID, classId : UUID)"),
         ("ProgressReadPort", "+ forClass(classId : UUID) : ClassProgress"),
         ("SubmissionReadPort", "+ recentForClass(classId : UUID) : List<SubmissionSummary>")],
        "Phạm vi lớp kiểm ở service trước khi đọc tiến độ; không phụ trách lớp nào trả trạng thái rỗng."),
    "a2_interview_content": (
        "A2 — Soạn câu hỏi phỏng vấn", "InterviewQuestionAuthoringService",
        ["+ create(actorId : UUID, draft : QuestionDraft) : Question",
         "+ updateDeleteOrImport(actorId : UUID, change : QuestionChange)",
         "+ requireCriteria(questionId : UUID)",
         "+ deletePreservingHistory(actorId : UUID, questionId : UUID)"],
        [("InterviewQuestionRepository", "+ save(question : Question)"),
         ("PermissionPort", "+ requireManageQuestions(actorId : UUID)"),
         ("AuditLogPort", "+ record(change : AuditEntry)")],
        "Quyền shared A2/A3; thiếu tiêu chí đánh giá thì không mở luyện; xoá mềm giữ lịch sử phiên."),
    "a2_overview": (
        "A2 — Tổng quan giảng viên", "InstructorDashboardService",
        ["+ routeForRole(instructorId : UUID) : DashboardRoute",
         "+ aggregateClasses(instructorId : UUID) : DashboardMetrics",
         "+ partialMetrics(instructorId : UUID) : DashboardMetrics"],
        [("InstructorClassPort", "+ listMine(instructorId : UUID) : List<Classroom>"),
         ("ProblemMetricsPort", "+ forClasses(ids : List<UUID>) : ProblemMetrics"),
         ("JudgeMetricsPort", "+ forClasses(ids : List<UUID>) : JudgeMetrics"),
         ("AiMetricsPort", "+ forClasses(ids : List<UUID>) : AiMetrics")],
        "Mỗi thẻ dùng tổng trên lớp đang phụ trách; khi một nguồn lỗi, trả thẻ còn lại và trạng thái nguồn lỗi."),
    "a3_permissions": (
        "A3 — Ma trận phân quyền", "PermissionMatrixService",
        ["+ setGrant(adminId : UUID, role : Role, function : Function, action : Action, allowed : boolean)",
         "+ listSeededActions() : PermissionMatrix",
         "+ auditGrantChange(adminId : UUID, change : GrantChange)"],
        [("PermissionRepository", "+ upsert(grant : PermissionGrant)"),
         ("PermissionCachePort", "+ invalidate(role : Role)"),
         ("AuditLogPort", "+ record(change : AuditEntry)")],
        "Function/Action là danh mục seed; đổi quyền làm mới cache và ghi audit."),
    "a3_users": (
        "A3 — Quản lý tài khoản", "AdminUserService",
        ["+ updateUser(adminId : UUID, command : UserAdminCommand)",
         "+ lockWithReason(adminId : UUID, userIds : List<UUID>, reason : String)",
         "+ bulkUpdate(adminId : UUID, command : BulkUserCommand) : BulkResult",
         "+ unlock(adminId : UUID, userId : UUID)"],
        [("UserRepository", "+ findById(id : UUID) : UserAccount"),
         ("SessionRevocationPort", "+ revokeAll(userId : UUID)"),
         ("AuditLogPort", "+ record(change : AuditEntry)")],
        "Khoá tài khoản đòi lý do; thao tác hàng loạt chọn trạng thái theo từng dòng; mở khoá không đòi lý do."),
    "a3_judge_operations": (
        "A3 — Giám sát máy chấm", "JudgeOperationsService",
        ["+ monitor() : JudgeQueueMetrics",
         "+ stalledSubmissions() : List<StalledSubmission>"],
        [("JudgeQueueReadPort", "+ metrics() : JudgeQueueMetrics"),
         ("SubmissionReadPort", "+ findStalled() : List<StalledSubmission>"),
         ("SystemLogPort", "+ listJudgeIncidents() : List<SystemLogEntry>")],
        "Bảng giám sát đọc số liệu hàng đợi và timeout; xử lý bài treo thuộc worker A4."),
    "a3_configuration": (
        "A3 — Cấu hình hệ thống và AI", "AdminConfigurationService",
        ["+ updateLanguageOrLimits(adminId : UUID, change : SystemConfigChange)",
         "+ publishAiConfig(adminId : UUID, draft : AiConfigDraft) : AiConfigVersion",
         "+ tokenUsage(filter : UsageFilter) : TokenUsageReport",
         "+ enforceBudget(actorId : UUID, feature : AiFeature) : BudgetDecision"],
        [("LanguageConfigRepository", "+ save(config : LanguageConfig)"),
         ("AiConfigRepository", "+ save(version : AiConfigVersion)"),
         ("AiUsageReadPort", "+ report(filter : UsageFilter) : TokenUsageReport"),
         ("AiBudgetPort", "+ check(actorId : UUID, feature : AiFeature) : BudgetDecision")],
        "Đổi prompt/model tạo phiên bản mới; ngân sách kiểm trước khi gọi AI; F1-F4 vẫn hoạt động khi hết hạn mức."),
    "a4_harness": (
        "A4 — Ghép mã và bảo vệ lỗi biên dịch", "HarnessOrchestrationService",
        ["+ generateForLanguage(spec : ProblemSpec, source : String, language : Language) : RunnableCode",
         "+ sanitizeCompilerError(error : CompileError) : PublicCompileError"],
        [("CodeGenerator", "+ generate(spec : ProblemSpec, source : String) : RunnableCode"),
         ("LanguagePluginRegistry", "+ forLanguage(language : Language) : LanguagePlugin"),
         ("CompilerErrorSanitizer", "+ sanitize(error : CompileError) : PublicCompileError")],
        "Chi tiết plugin F3 có ở hai hình mẫu; lỗi biên dịch không để lộ nội dung wrapper nội bộ."),
    "a4_judge_worker": (
        "A4 — Worker chấm bài và phát tiến độ", "JudgeWorkerService",
        ["+ acceptAndQueue(command : SubmitCommand) : UUID",
         "+ executeCase(submissionId : UUID, caseId : UUID) : CaseResult",
         "+ completeAllCases(submissionId : UUID) : SubmissionResult",
         "+ publishCaseProgress(result : CaseResult)",
         "+ recordIdempotently(result : CaseResult)",
         "+ sweepTimeouts(now : Instant) : int"],
        [("SubmissionRepository", "+ save(submission : Submission)"),
         ("JudgeEnginePort", "+ execute(payload : JudgePayload) : JudgeResult"),
         ("JudgeQueuePort", "+ enqueue(submissionId : UUID)"),
         ("ProgressEventPort", "+ publish(result : CaseResult)")],
        "Chấm hết testcase, tính điểm theo tỷ lệ đạt; ghi idempotent khi message phát lại; timeout sweep kết thúc bài treo."),
    "a4_hidden_testcases": (
        "A4 — Che dữ liệu testcase ẩn", "HiddenResultProjectionService",
        ["+ publicResult(result : CaseResult, visibility : Visibility) : PublicCaseResult"],
        [("CaseResultRepository", "+ listBySubmission(id : UUID) : List<CaseResult>"),
         ("HiddenDataPolicy", "+ project(result : CaseResult) : PublicCaseResult")],
        "Hidden chỉ công khai trạng thái/chỉ số; không trả input, expected output, diff hay object key qua API/event/log."),
    "a4_ai_processing": (
        "A4 — Điều phối AI và suy giảm có kiểm soát", "AiProcessingService",
        ["+ analyzeAccepted(submissionId : UUID) : ReviewReport",
         "+ runThreeStages(sessionId : UUID) : InterviewRubric",
         "+ buildSafeContext(request : AiRequest) : SafeAiContext",
         "+ degradeOnFailure(request : AiRequest, cause : AiFailure) : AiFallback"],
        [("AcceptedSubmissionPort", "+ requireAccepted(id : UUID) : Submission"),
         ("AiProviderPort", "+ complete(context : SafeAiContext) : AiResponse"),
         ("AiBudgetPort", "+ check(request : AiRequest) : BudgetDecision"),
         ("AiHistoryRepository", "+ save(result : AiResult)")],
        "Mọi lời gọi AI có ngữ cảnh tách dữ liệu/chỉ thị và kiểm hạn mức; lỗi AI không làm hỏng F1-F4."),
    "a4_interview_feedback": (
        "A4 — Đối chiếu câu trả lời lý thuyết", "InterviewAnswerEvaluationService",
        ["+ evaluate(questionId : UUID, studentId : UUID, answer : String) : PracticeFeedback"],
        [("InterviewQuestionRepository", "+ getCriteria(questionId : UUID) : EvaluationCriteria"),
         ("AiProviderPort", "+ compare(answer : String, criteria : EvaluationCriteria) : PracticeFeedback"),
         ("PracticeAttemptRepository", "+ save(attempt : PracticeAttempt)")],
        "AI so câu trả lời với tiêu chí chuẩn, ghi điểm đã đạt/còn thiếu/hướng bổ sung; không lộ đáp án chuẩn trong prompt người dùng."),
}


def build_spec(group: str, manifest: dict[str, str], criteria: list[dict[str, str]]) -> dict:
    title, service, methods, ports, note = DESIGNS[group]
    if len(methods) != len(criteria):
        raise ValueError(f"{group}: {len(methods)} methods for {len(criteria)} criteria")
    modules = manifest["dd_modules"].split(";")
    source = "; ".join(sorted({f"{row['source']}:{row['line']}" for row in criteria}))
    port_classes = [
        {"id": f"port{index}", "name": name,
         "kind": "interface" if name.endswith("Port") else "class",
         "methods": [method]}
        for index, (name, method) in enumerate(ports, 1)
    ]
    return {
        "_": (f"Nguồn tiêu chí: {source}. Nguồn thiết kế: "
              + "; ".join(f"03-dd/logic/{module}.md; 03-dd/api/{module}.md"
                          for module in modules)
              + ". [SoT: Suy luận] Tên lớp và chữ ký phương thức "
              "là thiết kế đề xuất từ hợp đồng DD vì backend nghiệp vụ chưa hiện thực đầy đủ. "
              "Mỗi phương thức ở lớp điều phối nối với đúng một tiêu chí theo thứ tự trong bảng truy vết; "
              "không hàm ý một endpoint riêng cho mỗi tiêu chí. Bỏ DTO HTTP, getter/setter, nhánh lỗi chi tiết."),
        "kind": "class", "title": title,
        "covers": manifest["feature_codes"].split(";"),
        "class": {
            "legend": "shared",
            "classes": [{"id": "service", "name": service, "methods": methods}] + port_classes,
            "externals": [],
            "relations": [{"from": "service", "to": f"port{index}",
                           "kind": "dependency"}
                          for index in range(1, len(ports) + 1)],
            "notes": [note, "[SoT: Suy luận] Tên lớp/phương thức là thiết kế backend từ DD."],
        },
    }


def main() -> None:
    with MANIFEST.open(encoding="utf-8-sig", newline="") as stream:
        manifest = list(csv.DictReader(stream))
    with COVERAGE.open(encoding="utf-8-sig", newline="") as stream:
        coverage = list(csv.DictReader(stream))
    for row in manifest:
        group = row["diagram_group"]
        if group not in DESIGNS:
            continue
        criteria = [item for item in coverage if item["diagram_group"] == group]
        spec = build_spec(group, row, criteria)
        path = ROOT / row["diagram_path"].replace(".drawio", ".spec.json")
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(spec, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        for item, method in zip(criteria, DESIGNS[group][2]):
            item["diagram"] = row["diagram_path"]
            item["class_refs"] = f"{DESIGNS[group][1]}.{method.split('(')[0].replace('+ ', '')}"
            item["evidence"] = (f"{item['source']}:{item['line']};"
                                + ";".join(f"03-dd/logic/{module}.md" for module in row["dd_modules"].split(";"))
                                + ";[SoT: Suy luận] application class names")
            item["status"] = "covered"
    with COVERAGE.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=coverage[0].keys())
        writer.writeheader()
        writer.writerows(coverage)
    print(f"Created {len(DESIGNS)} specs; mapped {sum(row['status'] == 'covered' for row in coverage)}/{len(coverage)} criteria")


if __name__ == "__main__":
    main()

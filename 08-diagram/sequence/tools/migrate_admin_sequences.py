"""Migrate admin sequence diagrams to align with standardized Class Diagrams."""

import json
from pathlib import Path
import sys

# Common participant rename rules per spec or general
def migrate_admin_spec(spec_path: Path):
    with spec_path.open("r", encoding="utf-8") as f:
        data = json.load(f)

    stem = spec_path.stem
    participants = data["sequence"]["participants"]
    messages = data["sequence"]["messages"]

    # File-specific or general participant mapping
    # Define mapping dictionary for this file
    p_map = {}
    stereo_map = {}

    if "them_tai_khoan" in stem:
        p_map["UserAdminCtrl"] = "AdminUserController"
        p_map["CreateUserCmd"] = "CreateUserCommandHandler"
        p_map["UserRepository"] = "UserAccountRepository"
        p_map["IdentityDbAdapter"] = "JdbcUserAccountRepository"

    elif "khoa_mo_khoa_tai_khoan" in stem:
        p_map["UserAdminController"] = "AdminUserActionController"
        p_map["ChangeUserStatus"] = "ChangeUserStatusCommandHandler"
        p_map["UserAdminStorePort"] = "AccountLifecycleRepository"
        p_map["IdentityDbAdapter"] = "JdbcUserAccountRepository"
        p_map["AdminEmailPort"] = "MailDeliveryPort"
        p_map["MailAdapter"] = "MailDeliveryAdapter"

    elif "doi_vai_tro_nguoi_dung_p1" in stem:
        p_map["UserAdminController"] = "AdminUserController"
        p_map["ListAdminUsers"] = "ListAdminUsersQueryHandler"
        p_map["UserAdminReadPort"] = "AccountLifecycleRepository"
        p_map["IdentityDbAdapter"] = "JdbcUserAccountRepository"

    elif "doi_vai_tro_nguoi_dung_p2" in stem:
        p_map["UserAdminController"] = "AdminUserActionController"
        p_map["ChangeUserRole"] = "ChangeUserRoleCommandHandler"
        p_map["UserAdminStorePort"] = "AccountLifecycleRepository"
        p_map["IdentityDbAdapter"] = "JdbcUserAccountRepository"

    elif "dat_lai_mat_khau" in stem:
        p_map["UserAdminController"] = "AdminUserActionController"
        p_map["ResetUserPassword"] = "ResetUserPasswordCommandHandler"
        p_map["IdentityStorePort"] = "AccountLifecycleRepository"
        p_map["IdentityDbAdapter"] = "JdbcUserAccountRepository"
        p_map["AdminEmailPort"] = "MailDeliveryPort"
        p_map["MailAdapter"] = "MailDeliveryAdapter"

    elif "doi_ma_tran_phan_quyen" in stem:
        p_map["GetPermissionMatrix"] = "GetPermissionMatrixQueryHandler"
        p_map["UpdatePermissionMatrixCell"] = "UpdateRolePermissionCommandHandler"
        p_map["RbacReadPort"] = "RbacRepository"
        p_map["RbacStorePort"] = "RbacRepository"
        p_map["IdentityDbAdapter"] = "JdbcRbacRepository"

    elif "giam_sat_hang_doi_p1" in stem:
        p_map["JudgeOpsController"] = "QueueMonitorController"
        p_map["GetQueueSnapshot"] = "GetQueueSnapshotQueryHandler"
        p_map["ListQueueJobs"] = "ListQueueJobsQueryHandler"
        p_map["QueueReadPort"] = "QueueMonitorRepository"
        p_map["JudgeDbAdapter"] = "JdbcQueueMonitorRepository"

    elif "giam_sat_hang_doi_p2" in stem:
        p_map["TimeoutSweepJob"] = "SweepStaleSubmissionsCommandHandler"
        p_map["SweepStorePort"] = "SubmissionWorkerRepository"
        p_map["JudgeDbAdapter"] = "JdbcSubmissionWorkerRepository"
        p_map["SubmissionEventPort"] = "SubmissionProgressPort"

    elif "giam_sat_hang_doi_p3b" in stem:
        p_map["QueueSettingsPort"] = "AdminQueueRepository"
        p_map["JudgeDbAdapter"] = "JdbcAdminQueueRepository"

    elif "giam_sat_hang_doi_p3" in stem:
        p_map["JudgeOpsController"] = "AdminQueueControlController"
        p_map["SetQueueConsumptionPaused"] = "SetQueueConsumptionPausedCommandHandler"
        p_map["QueueSettingsPort"] = "AdminQueueRepository"
        p_map["JudgeDbAdapter"] = "JdbcAdminQueueRepository"

    elif "cau_hinh_ngon_ngu_gioi_han_p1" in stem:
        p_map["LanguageConfigController"] = "AdminLanguageConfigController"
        p_map["GetLanguageConfigs"] = "GetLanguageConfigsQueryHandler"
        p_map["JudgeConfigReadPort"] = "AdminLanguageConfigRepository"
        p_map["JudgeDbAdapter"] = "JdbcAdminLanguageConfigRepository"

    elif "cau_hinh_ngon_ngu_gioi_han_p2" in stem:
        p_map["LanguageConfigController"] = "AdminLanguageConfigController"
        p_map["UpdateLanguageConfigs"] = "UpdateAdminLanguageConfigCommandHandler"
        p_map["JudgeConfigStorePort"] = "AdminLanguageConfigRepository"
        p_map["JudgeDbAdapter"] = "JdbcAdminLanguageConfigRepository"

    elif "cau_hinh_prompt_ai_p1" in stem:
        p_map["AiConfigController"] = "AdminLanguageConfigController"
        p_map["ListPromptTemplates"] = "ListPromptTemplatesQueryHandler"
        p_map["PromptReadPort"] = "AiPromptTemplateRepository"
        p_map["AiDbAdapter"] = "JdbcAiPromptTemplateRepository"

    elif "cau_hinh_prompt_ai_p2" in stem:
        p_map["AiConfigController"] = "AdminLanguageConfigController"
        p_map["UpdatePromptDraft"] = "UpdatePromptDraftCommandHandler"
        p_map["PromptStorePort"] = "AiPromptTemplateRepository"
        p_map["AiDbAdapter"] = "JdbcAiPromptTemplateRepository"

    elif "cau_hinh_prompt_ai_p3" in stem:
        p_map["AiConfigController"] = "AdminLanguageConfigController"
        p_map["PublishPromptTemplate"] = "PublishPromptTemplateCommandHandler"
        p_map["PromptStorePort"] = "AiPromptTemplateRepository"
        p_map["AiDbAdapter"] = "JdbcAiPromptTemplateRepository"

    elif "cau_hinh_prompt_ai_p4" in stem:
        p_map["AiConfigController"] = "AdminLanguageConfigController"
        p_map["PromptHistoryQuery"] = "GetPromptHistoryQueryHandler"
        p_map["UpdatePromptDraft"] = "UpdatePromptDraftCommandHandler"
        p_map["PromptReadPort"] = "AiPromptTemplateRepository"
        p_map["PromptStorePort"] = "AiPromptTemplateRepository"
        p_map["AiDbAdapter"] = "JdbcAiPromptTemplateRepository"

    elif "dat_ngan_sach_token_p1b" in stem:
        p_map["AiUsageController"] = "AdminLanguageConfigController"
        p_map["SetTokenBudgetLimit"] = "SetTokenBudgetLimitCommandHandler"
        p_map["BudgetStorePort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    elif "dat_ngan_sach_token_p1" in stem:
        p_map["AiUsageController"] = "AdminLanguageConfigController"
        p_map["GetAiTokenBudget"] = "GetAiTokenBudgetQueryHandler"
        p_map["BudgetReadPort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    elif "dat_ngan_sach_token_p2b" in stem:
        p_map["AiUsageController"] = "AdminLanguageConfigController"
        p_map["ListAiUsageAlerts"] = "ListAiUsageAlertsQueryHandler"
        p_map["AlertReadPort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"
        p_map["UserDirectoryPort"] = "AccountLifecycleRepository"

    elif "dat_ngan_sach_token_p2" in stem:
        p_map["UsageQueryPort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"
        p_map["UserDirectoryPort"] = "AccountLifecycleRepository"

    elif "dat_ngan_sach_token_p3" in stem:
        p_map["AiUsageController"] = "AdminLanguageConfigController"
        p_map["ReviewUsageAlert"] = "ReviewUsageAlertCommandHandler"
        p_map["AlertStorePort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    elif "dat_han_muc_va_gioi_han_ai_p1" in stem:
        p_map["AiLimitsController"] = "AdminLanguageConfigController"
        p_map["UnlockAiTokenBudget"] = "UnlockAiTokenBudgetCommandHandler"
        p_map["BudgetStorePort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    elif "dat_han_muc_va_gioi_han_ai_p2" in stem or "dat_han_muc_va_gioi_han_ai_p3" in stem:
        p_map["AiLimitsController"] = "AdminLanguageConfigController"
        p_map["UpdateRateLimitConfig"] = "UpdateRateLimitConfigCommandHandler"
        p_map["BudgetStorePort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    elif "dat_han_muc_va_gioi_han_ai_p4b" in stem:
        p_map["ProblemSettingsCtl"] = "AdminLanguageConfigController"
        p_map["UpdateTestcaseQuota"] = "UpdateTestcaseQuotaCommandHandler"
        p_map["ProblemSettingsPort"] = "AdminLanguageConfigRepository"
        p_map["ProblemBankDbAdapter"] = "JdbcAdminLanguageConfigRepository"

    elif "dat_han_muc_va_gioi_han_ai_p4" in stem:
        p_map["AiLimitsController"] = "AdminLanguageConfigController"
        p_map["ManageModelPrice"] = "ManageModelPriceCommandHandler"
        p_map["ModelPricePort"] = "AiTokenBudgetRepository"
        p_map["AiDbAdapter"] = "JdbcAiTokenBudgetRepository"

    # Apply mapping to participants
    new_participants = []
    seen_names = set()
    for p in participants:
        old_name = p["name"]
        new_name = p_map.get(old_name, old_name)
        if new_name in seen_names:
            # Skip duplicate if two old participants mapped to the same new name (e.g. PromptReadPort & PromptStorePort)
            continue
        seen_names.add(new_name)
        new_p = dict(p)
        new_p["name"] = new_name
        new_participants.append(new_p)

    data["sequence"]["participants"] = new_participants

    # Recursively update messages
    def update_messages(items):
        for item in items:
            if "alt" in item:
                update_messages(item["alt"].get("then", []))
                update_messages(item["alt"].get("else", []))
            else:
                if "from" in item:
                    item["from"] = p_map.get(item["from"], item["from"])
                if "to" in item:
                    item["to"] = p_map.get(item["to"], item["to"])

    update_messages(messages)

    with spec_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

def main():
    admin_folder = Path("08-diagram/sequence/admin")
    specs = sorted(admin_folder.glob("*.spec.json"))
    for s in specs:
        migrate_admin_spec(s)
        print(f"Migrated {s.name}")

if __name__ == "__main__":
    main()

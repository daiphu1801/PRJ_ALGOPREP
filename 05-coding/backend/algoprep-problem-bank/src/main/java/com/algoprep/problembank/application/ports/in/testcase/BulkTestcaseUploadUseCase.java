package com.algoprep.problembank.application.ports.in.testcase;

import com.algoprep.problembank.application.dto.testcase.BulkTestcaseUpload;

/** Bulk testcase upload through presigned URLs, DD F2 sections 46 and 47 (BR-14). */
public interface BulkTestcaseUploadUseCase {
  BulkTestcaseUpload.UrlPlan requestUrls(BulkTestcaseUpload.RequestUrls command);

  BulkTestcaseUpload.Added confirm(BulkTestcaseUpload.Confirm command);
}

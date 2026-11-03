/**
 * Cổng RA của đường ĐỌC.
 *
 * <p>Trả read model phẳng, KHÔNG trả Aggregate. Đường đọc không đi qua Aggregate để một dòng danh
 * sách không nạp kèm toàn bộ dữ liệu con (01-rd/system/backend_architecture.md mục 4.B).
 */
package com.algoprep.interviewbank.application.ports.out;

/**
 * Adapter và cấu hình của Bounded Context F1.
 *
 * <p>Hiện thực các cổng ra, nói chuyện với PostgreSQL / RabbitMQ / Redis / hệ ngoài. Tầng
 * presentation KHÔNG được gọi thẳng vào đây (01-rd/system/backend_architecture.md mục 3.A).
 */
package com.algoprep.identity.infrastructure;

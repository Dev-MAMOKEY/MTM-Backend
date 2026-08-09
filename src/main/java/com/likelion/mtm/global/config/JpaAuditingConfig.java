package com.likelion.mtm.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity의 @CreatedDate/@LastModifiedDate가 동작하려면 필요한 설정.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

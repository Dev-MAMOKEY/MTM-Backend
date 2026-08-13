package com.likelion.mtm.domain.member.entity;

import com.likelion.mtm.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 회원 — 이메일·비밀번호로 가입한 사용자. 신체 정보(키·몸무게)를 함께 갖는다.
 * 신체 정보는 가입 시점엔 비어 있고, 신체 정보 입력 화면에서 채워진다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email")
)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "height_cm", precision = 4, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Builder
    private Member(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /**
     * 회원가입 시 사용하는 정적 팩토리 메서드. 비밀번호는 반드시 해시된 값으로 받는다.
     */
    public static Member register(String email, String passwordHash) {
        return Member.builder()
                .email(email)
                .passwordHash(passwordHash)
                .build();
    }

    /**
     * 신체 정보(키·몸무게) 입력/수정 — 프롬프트 조립기가 몸무게를 쓴다.
     */
    public void updateBodyInfo(BigDecimal heightCm, BigDecimal weightKg) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }

    /**
     * 비밀번호 변경 — 반드시 해시된 값을 받는다. 평문을 저장하지 않는다.
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }
}
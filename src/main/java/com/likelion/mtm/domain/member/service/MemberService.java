package com.likelion.mtm.domain.member.service;

import com.likelion.mtm.domain.member.dto.MemberResponseDTO;
import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.member.repository.MemberRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /** 내 정보 조회 — DTO 변환까지 트랜잭션 안에서 끝낸다 */
    public MemberResponseDTO getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponseDTO.from(member);
    }
}
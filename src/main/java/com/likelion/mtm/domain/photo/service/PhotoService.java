package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.member.repository.MemberRepository;
import com.likelion.mtm.domain.photo.dto.PhotoResponse;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 원본 사진 업로드와 사진첩 조회 비즈니스 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final MemberRepository memberRepository;
    private final PhotoRepository photoRepository;
    private final ImageStorage imageStorage;

    /**
     * 로그인한 회원의 원본 사진을 저장한다.
     *
     * @param memberId 로그인 회원 식별자
     * @param file 업로드할 원본 사진
     * @return 저장된 원본 사진 정보
     */
    @Transactional
    public PhotoResponse upload(Long memberId, MultipartFile file) {
        validateImageFile(file);

        Member member = findMember(memberId);

        String storageKey = imageStorage.store(file, "photos");

        Photo photo = Photo.upload(member, storageKey);
        Photo savedPhoto = photoRepository.save(photo);

        String imageUrl = imageStorage.getUrl(savedPhoto.getStorageKey());

        return PhotoResponse.from(savedPhoto, imageUrl);
    }

    /**
     * 로그인한 회원의 사진첩을 최신 업로드 순으로 조회한다.
     *
     * @param memberId 로그인 회원 식별자
     * @return 해당 회원의 원본 사진 목록
     */
    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotos(Long memberId) {
        return photoRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(photo -> PhotoResponse.from(
                        photo,
                        imageStorage.getUrl(photo.getStorageKey())
                ))
                .toList();
    }

    /**
     * 회원 식별자로 회원을 조회한다.
     *
     * @param memberId 회원 식별자
     * @return 조회된 회원
     */
    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 업로드된 파일이 유효한 이미지인지 검증한다.
     *
     * @param file 검증할 파일
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_IMAGE_FILE);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }
}
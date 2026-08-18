package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.member.repository.MemberRepository;
import com.likelion.mtm.domain.photo.dto.PhotoResponse;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 원본 사진 업로드와 사진첩 조회·삭제 비즈니스 로직을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final MemberRepository memberRepository;
    private final PhotoRepository photoRepository;
    private final BaseImageRepository baseImageRepository;
    private final WornImageRepository wornImageRepository;
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
     * 로그인한 회원의 원본 사진을 삭제한다.
     *
     * 사진 위에 만든 기준 이미지와 착용 이미지도 함께 사라진다 — 원본이 없으면 그 위의 결과물은
     * 의미가 없기 때문이다. 되돌릴 수 없다.
     *
     * 외래 키가 CASCADE라 사진만 지워도 DB는 정리되지만, 그러면 저장소 키를 잃어버려
     * 파일이 버킷에 영구히 남는다. 그래서 지우기 전에 키를 모으고 자식부터 명시적으로 지운다.
     * 저장소 삭제는 DB 삭제가 모두 끝난 뒤 마지막에 수행한다.
     *
     * @param memberId 로그인 회원 식별자
     * @param photoId 삭제할 원본 사진 식별자
     */
    @Transactional
    public void delete(Long memberId, Long photoId) {
        Photo photo = findOwnedPhoto(memberId, photoId);

        List<String> storageKeys = new ArrayList<>();

        baseImageRepository.findByPhotoId(photoId).ifPresent(baseImage -> {
            // 착용 이미지는 삭제된 엔티티를 돌려받아 저장소 키를 뽑는다
            wornImageRepository.deleteAllByBaseImageId(baseImage.getId())
                    .stream()
                    .map(WornImage::getStorageKey)
                    .forEach(storageKeys::add);

            storageKeys.add(baseImage.getStorageKey());
            baseImageRepository.delete(baseImage);
        });

        storageKeys.add(photo.getStorageKey());
        photoRepository.delete(photo);

        // DB 삭제가 끝난 뒤에 파일을 지운다. 실패해도 예외를 올리지 않으므로 트랜잭션이 되돌아가지 않는다
        storageKeys.forEach(this::deleteQuietly);
    }

    /**
     * 로그인 회원이 소유한 원본 사진을 조회한다.
     * 남의 사진이면 존재 자체를 숨기기 위해 조회 실패와 같은 예외를 던진다.
     *
     * @param memberId 로그인 회원 식별자
     * @param photoId 원본 사진 식별자
     * @return 조회된 원본 사진
     */
    private Photo findOwnedPhoto(Long memberId, Long photoId) {
        Photo photo = photoRepository.findByIdWithMember(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        if (!Objects.equals(photo.getMember().getId(), memberId)) {
            throw new CustomException(ErrorCode.PHOTO_NOT_FOUND);
        }

        return photo;
    }

    /**
     * 저장소 파일을 지운다. DB에서는 이미 사라졌으므로 파일 삭제가 실패해도 예외를 올리지 않고
     * 로그만 남긴다 — 여기서 실패하면 고아 파일이 남을 뿐, 사용자에게는 삭제가 끝난 상태다.
     *
     * @param storageKey 삭제할 저장소 키
     */
    private void deleteQuietly(String storageKey) {
        try {
            imageStorage.delete(storageKey);
        } catch (Exception e) {
            log.error("삭제된 사진의 저장소 파일 정리에 실패했습니다. storageKey={}", storageKey, e);
        }
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

        if (contentType == null || !SUPPORTED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

}

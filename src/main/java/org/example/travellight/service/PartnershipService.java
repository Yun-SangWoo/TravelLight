package org.example.travellight.service;

import org.example.travellight.dto.PartnershipDto;
import org.example.travellight.entity.Partnership;
import org.example.travellight.repository.PartnershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class PartnershipService {

    @Autowired
    private PartnershipRepository partnershipRepository;

    private final AddressTsService addressTsService;

    public PartnershipService(PartnershipRepository partnershipRepository, AddressTsService addressTsService) {
        this.addressTsService = addressTsService;
    }

    @Transactional
    public Partnership createPartnership(PartnershipDto dto) {
        Partnership partnership = new Partnership();

        // 프론트엔드에서 전달된 위도/경도 값 사용
        double latitude = dto.getLatitude();
        double longitude = dto.getLongitude();
        
        // 위경도가 전달되지 않았거나 0인 경우에만 API로 변환 시도
        if (latitude == 0 || longitude == 0) {
            try {
                System.out.println("프론트엔드에서 좌표가 전달되지 않아 API로 변환 시도");
                // 주소 → 위도/경도 변환
                double[] latLng = addressTsService.getCoordinatesFromAddress(dto.getAddress());
                latitude = latLng[0];
                longitude = latLng[1];
                System.out.println("API 변환 좌표: [" + latitude + ", " + longitude + "]");
            } catch (Exception e) {
                System.out.println("API 좌표 변환 실패, 오류: " + e.getMessage());
                throw new RuntimeException("주소를 좌표로 변환할 수 없습니다: " + e.getMessage());
            }
        } else {
            System.out.println("프론트엔드에서 전달된 좌표 사용: [" + latitude + ", " + longitude + "]");
        }

        // 기본 정보 설정
        partnership.setBusinessName(dto.getBusinessName());
        partnership.setOwnerName(dto.getOwnerName());
        partnership.setEmail(dto.getEmail());
        partnership.setPhone(dto.getPhone());
        partnership.setAddress(dto.getAddress());

        // 좌표 설정
        partnership.setLatitude(latitude);
        partnership.setLongitude(longitude);

        partnership.setBusinessType(dto.getBusinessType());
        partnership.setSpaceSize(dto.getSpaceSize());
        partnership.setAdditionalInfo(dto.getAdditionalInfo());
        partnership.setAgreeTerms(dto.isAgreeTerms());
        partnership.setIs24Hours(dto.isIs24Hours());

        // 영업시간 정보 변환 및 설정
        Map<String, String> businessHoursMap = new HashMap<>();
        if (dto.getBusinessHours() != null) {
            for (Map.Entry<String, PartnershipDto.BusinessHourDto> entry : dto.getBusinessHours().entrySet()) {
                String day = entry.getKey();
                PartnershipDto.BusinessHourDto hourDto = entry.getValue();

                // 24시간 영업이거나 해당 요일이 활성화된 경우만 저장
                if (dto.isIs24Hours()) {
                    businessHoursMap.put(day, "24시간");
                } else if (hourDto.isEnabled()) {
                    businessHoursMap.put(day, hourDto.getOpen() + "-" + hourDto.getClose());
                }
            }
        }
        partnership.setBusinessHours(businessHoursMap);

        // 고유 신청 ID 생성
        String submissionId = generateSubmissionId();
        partnership.setSubmissionId(submissionId);

        // 저장 및 반환
        return partnershipRepository.save(partnership);
    }

    private String generateSubmissionId() {
        // "PN" + 현재 시간 기반으로 고유 ID 생성
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        return "PN" + timestamp;
    }

    public Partnership getPartnershipBySubmissionId(String submissionId) {
        return partnershipRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("제휴 신청 정보를 찾을 수 없습니다: " + submissionId));
    }

    public Partnership registerPartnership(PartnershipDto dto) {
        // 주소 → 좌표 변환은 AddressTsService에게 맡김
        double[] latLng = addressTsService.getCoordinatesFromAddress(dto.getAddress());

        Partnership partnership = new Partnership();
        partnership.setAddress(dto.getAddress());
        partnership.setLatitude(latLng[0]);
        partnership.setLongitude(latLng[1]);

        return partnershipRepository.save(partnership);
    }
}
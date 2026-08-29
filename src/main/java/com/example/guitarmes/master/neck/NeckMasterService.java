package com.example.guitarmes.master.neck;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;

@Service
public class NeckMasterService {

    private final NeckMasterRepository neckMasterRepository;

    public NeckMasterService(
            NeckMasterRepository neckMasterRepository) {
        this.neckMasterRepository = neckMasterRepository;
    }

    public List<NeckMaster> getNeckMasters() {
        return neckMasterRepository.findAll();
    }

    public NeckMaster getNeckMasterById(Long id) {
        if (id == null) {
            throw new BusinessException(
                    "ネックマスタIDが指定されていません。");
        }
        return neckMasterRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "指定されたネックマスタが存在しません。"));
    }

    public NeckMasterUpdateRequest
            getNeckMasterUpdateRequest(Long id) {

        NeckMaster neckMaster = getNeckMasterById(id);
        NeckMasterUpdateRequest request =
                new NeckMasterUpdateRequest();

        request.setModelCode(neckMaster.getModelCode());
        request.setModelName(neckMaster.getModelName());
        request.setProductFamilyCode(
                neckMaster.getProductFamilyCode());
        request.setNeckType(neckMaster.getNeckType());
        request.setNeckMaterial(
                neckMaster.getNeckMaterial());
        request.setFingerboardMaterial(
                neckMaster.getFingerboardMaterial());
        request.setFretCount(neckMaster.getFretCount());
        request.setScale(neckMaster.getScale());

        return request;
    }

    @Transactional
    public NeckMaster createNeckMaster(
            NeckMaster neckMaster) {
        if (neckMaster == null) {
            throw new BusinessException(
                    "ネックマスタ情報が指定されていません。");
        }
        return neckMasterRepository.save(neckMaster);
    }

    @Transactional
    public NeckMaster updateNeckMaster(
            Long id,
            NeckMasterUpdateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "ネックマスタ情報が指定されていません。");
        }

        NeckMaster neckMaster = getNeckMasterById(id);
        String modelName = normalizeRequired(
                request.getModelName(),
                "モデル名");

        neckMaster.setModelName(modelName);
        return neckMasterRepository.save(neckMaster);
    }

    private String normalizeRequired(
            String value,
            String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    fieldName + "を入力してください。");
        }
        return value.trim();
    }
}

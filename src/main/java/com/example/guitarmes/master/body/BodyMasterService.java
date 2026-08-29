package com.example.guitarmes.master.body;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;

@Service
public class BodyMasterService {

    private final BodyMasterRepository
            bodyMasterRepository;

    public BodyMasterService(
            BodyMasterRepository bodyMasterRepository) {
        this.bodyMasterRepository = bodyMasterRepository;
    }

    public List<BodyMaster> getBodyMasters() {
        return bodyMasterRepository.findAll();
    }

    public BodyMaster getBodyMasterById(
            Long id) {

        if (id == null) {
            throw new BusinessException(
                    "ボディマスタIDが指定されていません。");
        }

        return bodyMasterRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(
                                "指定されたボディマスタが存在しません。"));
    }

    public BodyMasterUpdateRequest
            getBodyMasterUpdateRequest(
                    Long id) {

        BodyMaster bodyMaster =
                getBodyMasterById(id);
        BodyMasterUpdateRequest request =
                new BodyMasterUpdateRequest();

        request.setModelCode(
                bodyMaster.getModelCode());
        request.setModelName(
                bodyMaster.getModelName());
        request.setProductFamilyCode(
                bodyMaster.getProductFamilyCode());
        request.setBodyType(
                bodyMaster.getBodyType());
        request.setMaterial(
                bodyMaster.getMaterial());
        request.setColor(
                bodyMaster.getColor());

        return request;
    }

    @Transactional
    public BodyMaster createBodyMaster(
            BodyMaster bodyMaster) {

        if (bodyMaster == null) {
            throw new BusinessException(
                    "ボディマスタ情報が指定されていません。");
        }
        return bodyMasterRepository.save(bodyMaster);
    }

    @Transactional
    public BodyMaster updateBodyMaster(
            Long id,
            BodyMasterUpdateRequest request) {

        if (request == null) {
            throw new BusinessException(
                    "ボディマスタ情報が指定されていません。");
        }

        BodyMaster bodyMaster =
                getBodyMasterById(id);
        String modelName =
                normalizeRequired(
                        request.getModelName(),
                        "モデル名");

        bodyMaster.setModelName(modelName);
        return bodyMasterRepository.save(bodyMaster);
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

package com.example.guitarmes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.entity.NeckMaster;
import com.example.guitarmes.repository.NeckMasterRepository;

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
        return neckMasterRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "指定されたネックマスタが存在しません。"));
    }

    public NeckMaster createNeckMaster(
            NeckMaster neckMaster) {

        return neckMasterRepository.save(neckMaster);
    }
}
package com.example.guitarmes.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.guitarmes.dto.AssemblyCreateRequest;
import com.example.guitarmes.dto.AssemblyResponse;
import com.example.guitarmes.entity.Assembly;
import com.example.guitarmes.service.AssemblyService;

@RestController
@RequestMapping("/api/assemblies")
public class AssemblyController {

    private final AssemblyService assemblyService;

    public AssemblyController(
            AssemblyService assemblyService) {

        this.assemblyService =
                assemblyService;
    }

    @PostMapping
    public Assembly createAssembly(
            @RequestBody
            AssemblyCreateRequest request) {

        return assemblyService.createAssembly(
                request.getProductionOrderId(),
                request.getNeckId(),
                request.getBodyId(),
                request.getWorkerName());
    }

    @GetMapping
    public List<AssemblyResponse>
            getAssemblies() {

        return assemblyService.getAssemblies();
    }

    @GetMapping("/{id}")
    public AssemblyResponse getAssemblyById(
            @PathVariable Long id) {

        return assemblyService
                .getAssemblyById(id);
    }
}
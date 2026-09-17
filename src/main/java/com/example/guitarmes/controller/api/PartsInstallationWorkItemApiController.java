package com.example.guitarmes.controller.api;
import org.springframework.web.bind.annotation.*;
import com.example.guitarmes.process.partsinstallation.*;
@RestController
@RequestMapping("/api/process-work-items")
public class PartsInstallationWorkItemApiController {
    private final PartsInstallationWorkItemApiService service;
    public PartsInstallationWorkItemApiController(PartsInstallationWorkItemApiService service){this.service=service;}
    @PutMapping("/{itemId}/complete") public PartsInstallationWorkItemResponse complete(@PathVariable Long itemId){return service.complete(itemId);}
    @DeleteMapping("/{itemId}/complete") public PartsInstallationWorkItemResponse uncomplete(@PathVariable Long itemId){return service.uncomplete(itemId);}
}

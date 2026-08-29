package com.example.guitarmes.body;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bodies")
public class BodyController {

    private final BodyService bodyService;

    public BodyController(
            BodyService bodyService) {

        this.bodyService = bodyService;
    }

    @GetMapping
    public List<Body> getBodies() {

        return bodyService.getBodies();
    }

    @GetMapping("/{id}")
    public Body getBodyById(
            @PathVariable Long id) {

        return bodyService
                .getBodyById(id);
    }

    @PostMapping
    public Body createBody(
            @RequestBody
            BodyCreateRequest request) {

        return bodyService.createBody(
                request.getBodyMasterId());
    }
}
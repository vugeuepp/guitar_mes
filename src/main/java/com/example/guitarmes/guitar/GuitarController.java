package com.example.guitarmes.guitar;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guitars")
public class GuitarController {

    private final GuitarService guitarService;

    public GuitarController(
            GuitarService guitarService) {

        this.guitarService =
                guitarService;
    }

    @GetMapping
    public List<Guitar> getGuitars() {

        return guitarService.getGuitars();
    }

    @GetMapping("/{id}")
    public Guitar getGuitarById(
            @PathVariable Long id) {

        return guitarService
                .getGuitarById(id);
    }

    @PutMapping("/{id}")
    public Guitar updateGuitar(
            @PathVariable Long id,
            @RequestBody
            GuitarUpdateRequest request) {

        return guitarService.updateGuitar(
                id,
                request.getCurrentProcess());
    }
}
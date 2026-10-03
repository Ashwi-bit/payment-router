package com.ashwini.router.controller;

import com.ashwini.router.gateway.GatewayRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateways")
public class GatewayController {

    private final GatewayRegistry registry;

    public GatewayController(GatewayRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("gateways", registry.snapshotAll());
        out.put("aliveCount", registry.rankedByEma().size());
        out.put("totalCount", registry.allIncludingKilled().size());
        return out;
    }

    @PostMapping("/{id}/kill")
    public ResponseEntity<Map<String, Object>> kill(@PathVariable String id) {
        boolean ok = registry.kill(id);
        if (!ok) return notFound(id);
        return ResponseEntity.ok(Map.of(
            "gateway", id,
            "action", "killed",
            "status", "ok"
        ));
    }

    @PostMapping("/{id}/revive")
    public ResponseEntity<Map<String, Object>> revive(@PathVariable String id) {
        boolean ok = registry.revive(id);
        if (!ok) return notFound(id);
        return ResponseEntity.ok(Map.of(
            "gateway", id,
            "action", "revived",
            "status", "ok"
        ));
    }

    private ResponseEntity<Map<String, Object>> notFound(String id) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "gateway not found",
            "gateway", id
        ));
    }
}